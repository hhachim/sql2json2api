package com.etljobs.sql2json2api.runner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.etljobs.sql2json2api.model.ApiResponse;
import com.etljobs.sql2json2api.model.ApiTemplateResult;
import com.etljobs.sql2json2api.model.SqlFile;
import com.etljobs.sql2json2api.service.http.ApiClientService;
import com.etljobs.sql2json2api.service.http.TokenService;
import com.etljobs.sql2json2api.service.sql.SqlExecutionService;
import com.etljobs.sql2json2api.service.sql.SqlFileService;
import com.etljobs.sql2json2api.service.template.TemplateProcessingService;
import com.etljobs.sql2json2api.service.threading.SqlFileSequentialCoordinator;

import lombok.extern.slf4j.Slf4j;

/**
 * Command line runner for testing API calls. This will run when the
 * 'api-call-demo' profile is active.
 */
@Component
@Slf4j
@Profile({"api-call-demo", "dev", "sql2json2api"})
public class ApiCallRunner implements CommandLineRunner, ExitCodeGenerator {

    private final SqlFileService sqlFileService;
    private final SqlExecutionService sqlExecutionService;
    private final TemplateProcessingService templateProcessingService;
    private final ApiClientService apiClientService;
    private final TokenService tokenService;
    private final SqlFileSequentialCoordinator coordinator;
    
    @Value("${app.threading.enabled:false}")
    private boolean threadingEnabled;
    private int exitCode = 0;

    @Autowired
    public ApiCallRunner(
            SqlFileService sqlFileService,
            SqlExecutionService sqlExecutionService,
            TemplateProcessingService templateProcessingService,
            ApiClientService apiClientService,
            TokenService tokenService,
            SqlFileSequentialCoordinator coordinator) {
        this.sqlFileService = sqlFileService;
        this.sqlExecutionService = sqlExecutionService;
        this.templateProcessingService = templateProcessingService;
        this.apiClientService = apiClientService;
        this.tokenService = tokenService;
        this.coordinator = coordinator;
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            log.info("=== API Call Sql2Json2Api ===");
            log.info("Mode d'exécution: {}", threadingEnabled ? "parallèle" : "séquentiel");

            if (threadingEnabled) {
                // Mode multithreading avec le coordinateur
                processWithCoordinator();
            } else {
                // Mode séquentiel traditionnel
                processSequentially();
            }

            log.info("\n=== All SQL Files Processing Complete ===");

        } catch (Exception e) {
            log.error("Error during API call demo", e);
            exitCode = 1;
        }
    }
    
    /**
     * Traitement avec le coordinateur pour l'exécution parallèle des appels API
     */
    private void processWithCoordinator() {
        // Utiliser le coordinateur pour traiter tous les fichiers SQL séquentiellement
        Map<String, List<ApiResponse>> resultsByFile = coordinator.processAllSqlFiles();
        
        // Afficher le résumé des résultats
        if (resultsByFile.isEmpty()) {
            log.warn("Aucun résultat obtenu, vérifiez la configuration des fichiers SQL");
        } else {
            log.info("\n=== Résumé des traitements ===");
            
            for (Map.Entry<String, List<ApiResponse>> entry : resultsByFile.entrySet()) {
                String fileName = entry.getKey();
                List<ApiResponse> responses = entry.getValue();
                
                long successCount = responses.stream().filter(ApiResponse::isSuccess).count();
                
                log.info("{}: {} appels API - {} succès, {} échecs", 
                        fileName, responses.size(), successCount, responses.size() - successCount);
            }
        }
    }
    
    /**
     * Traitement séquentiel traditionnel (comportement d'origine)
     */
    private void processSequentially() {
        // Récupérer les fichiers SQL dans l'ordre configuré
        List<SqlFile> sqlFiles = sqlFileService.getSqlFilesInConfiguredOrder();
        if (sqlFiles.isEmpty()) {
            log.warn("No SQL files found");
            return;
        }

        log.info("Found {} SQL files to process", sqlFiles.size());

        // Traiter chaque fichier SQL
        for (SqlFile sqlFile : sqlFiles) {
            log.info("\n===> Processing SQL file: {}", sqlFile.getFileName());

            // Exécuter la requête SQL
            List<Map<String, Object>> results = sqlExecutionService.executeQuery(sqlFile.getContent());

            if (results.isEmpty()) {
                log.info("No results for SQL file: {}", sqlFile.getFileName());
                continue;
            }

            log.info("Found {} rows to process for {}", results.size(), sqlFile.getFileName());

            // Get the authentication token once for each SQL file
            String token = tokenService.getToken();
            log.info("Using authentication token for calls");

            // Process results and make API calls
            List<ApiResponse> responses = new ArrayList<>();

            int rowsToProcess = results.size();
            for (int i = 0; i < rowsToProcess; i++) {
                Map<String, Object> row = results.get(i);
                log.info("\nProcessing row {}: {}", i + 1, row);

                // Transform the row data using the template
                ApiTemplateResult templateResult = templateProcessingService.processTemplate(
                        sqlFile.getTemplateName(), row);

                log.info("Generated JSON payload: {}", templateResult.getJsonPayload());
                log.info("API endpoint info: {} {}",
                        templateResult.getEndpointInfo().getMethod(),
                        templateResult.getEndpointInfo().getRoute());

                // Make the API call
                ApiResponse response = apiClientService.callApi(
                        templateResult.getEndpointInfo().getRoute(),
                        templateResult.getEndpointInfo().getMethod(),
                        templateResult.getJsonPayload(),
                        templateResult.getEndpointInfo().getHeaders(),
                        templateResult.getEndpointInfo().getUrlParams());

                // If the response indicates an authentication error, retry with a new token
                if (response.getStatusCode() == 401) {
                    log.info("Authentication error detected, retrying with a new token...");
                    response = apiClientService.retryWithNewToken(
                            templateResult.getEndpointInfo().getRoute(),
                            templateResult.getEndpointInfo().getMethod(),
                            templateResult.getJsonPayload(),
                            templateResult.getEndpointInfo().getHeaders(),
                            templateResult.getEndpointInfo().getUrlParams());
                }

                responses.add(response);

                log.info("API call result - Status: {}, Success: {}",
                        response.getStatusCode(), response.isSuccess());
                log.info("Response body: {}", response.getBody());
            }

            // Show summary for this SQL file
            int successCount = (int) responses.stream().filter(ApiResponse::isSuccess).count();
            log.info("\nSummary for {}: Processed {} rows with {} API calls - {} successful, {} failed",
                    sqlFile.getFileName(), rowsToProcess, responses.size(),
                    successCount, responses.size() - successCount);
        }
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }
}