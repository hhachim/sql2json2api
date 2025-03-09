package com.etljobs.sql2json2api.runner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
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

import lombok.extern.slf4j.Slf4j;

/**
 * Command line runner for testing API calls. This will run when the
 * 'api-call-demo' profile is active.
 */
@Component
@Slf4j
@Profile({"api-call-demo", "dev"})
public class ApiCallRunner implements CommandLineRunner, ExitCodeGenerator {

    private final SqlFileService sqlFileService;
    private final SqlExecutionService sqlExecutionService;
    private final TemplateProcessingService templateProcessingService;
    private final ApiClientService apiClientService;
    private final TokenService tokenService;
    private int exitCode = 0;

    @Autowired
    public ApiCallRunner(
            SqlFileService sqlFileService,
            SqlExecutionService sqlExecutionService,
            TemplateProcessingService templateProcessingService,
            ApiClientService apiClientService,
            TokenService tokenService) {
        this.sqlFileService = sqlFileService;
        this.sqlExecutionService = sqlExecutionService;
        this.templateProcessingService = templateProcessingService;
        this.apiClientService = apiClientService;
        this.tokenService = tokenService;
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            log.info("=== API Call Demo ===");

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

                // Process only the first 2 results for demo purposes
                int rowsToProcess = Math.min(2, results.size());
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

            log.info("\n=== All SQL Files Processing Complete ===");

        } catch (Exception e) {
            log.error("Error during API call demo", e);
            exitCode = 1;
        }
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }
}
