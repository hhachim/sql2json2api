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
 * Command line runner for testing API calls.
 * This will run when the 'api-call-demo' profile is active.
 */
@Component
@Slf4j
@Profile({"api-call-demo","dev"})
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
            
            // Select one SQL file to process
            List<SqlFile> sqlFiles = sqlFileService.listSqlFiles();
            if (sqlFiles.isEmpty()) {
                log.warn("No SQL files found");
                return;
            }
            
            // Choose the first SQL file for demo purposes
            SqlFile sqlFile = sqlFiles.get(0);
            log.info("Processing SQL file: {}", sqlFile.getFileName());
            
            // Execute the SQL query
            List<Map<String, Object>> results = sqlExecutionService.executeQuery(sqlFile.getContent());
            
            if (results.isEmpty()) {
                log.info("No results to process");
                return;
            }
            
            log.info("Found {} rows to process", results.size());
            
            // Get the authentication token once (reused for all calls)
            String token = tokenService.getToken();
            log.info("Generated authentication token: {}", token);
            
            // Process results and make API calls
            List<ApiResponse> responses = new ArrayList<>();
            
            // Process only the first 2 results for demo purposes
            int rowsToProcess = Math.min(2, results.size());
            for (int i = 0; i < rowsToProcess; i++) {
                Map<String, Object> row = results.get(i);
                log.info("\nProcessing row {}: {}", i+1, row);
                
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
            
            log.info("\nProcessed {} rows with {} API calls", rowsToProcess, responses.size());
            
            // Show summary
            int successCount = (int) responses.stream().filter(ApiResponse::isSuccess).count();
            log.info("API Call Summary: {} successful, {} failed", 
                    successCount, responses.size() - successCount);
            
            log.info("=== Demo Complete ===");
            
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