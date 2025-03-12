package com.etljobs.sql2json2api.runner;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.etljobs.sql2json2api.model.ApiEndpointInfo;
import com.etljobs.sql2json2api.model.ApiTemplateResult;
import com.etljobs.sql2json2api.model.SqlFile;
import com.etljobs.sql2json2api.service.sql.SqlExecutionService;
import com.etljobs.sql2json2api.service.sql.SqlFileService;
import com.etljobs.sql2json2api.service.template.TemplateProcessingService;

import lombok.extern.slf4j.Slf4j;

/**
 * Command line runner for testing template processing functionality.
 * This will run when the 'template-processing-demo' profile is active.
 */
@Component
@Slf4j
@Profile("template-processing-demo")
public class TemplateProcessingRunner implements CommandLineRunner, ExitCodeGenerator {
    
    private final SqlFileService sqlFileService;
    private final SqlExecutionService sqlExecutionService;
    private final TemplateProcessingService templateProcessingService;
    private int exitCode = 0;
    
    @Autowired
    public TemplateProcessingRunner(
            SqlFileService sqlFileService, 
            SqlExecutionService sqlExecutionService,
            TemplateProcessingService templateProcessingService) {
        this.sqlFileService = sqlFileService;
        this.sqlExecutionService = sqlExecutionService;
        this.templateProcessingService = templateProcessingService;
    }
    
    @Override
    public void run(String... args) throws Exception {
        try {
            log.info("=== Template Processing Demo ===");
            
            // List available SQL files
            List<SqlFile> sqlFiles = sqlFileService.listSqlFiles();
            log.info("Available SQL files:");
            sqlFiles.forEach(file -> log.info("- {}", file.getFileName()));
            
            // Process each available SQL file
            for (SqlFile sqlFile : sqlFiles) {
                String fileName = sqlFile.getFileName();
                String templateName = sqlFile.getTemplateName();
                
                log.info("\nProcessing SQL file: {} with template: {}", fileName, templateName);
                
                // Execute SQL query
                List<Map<String, Object>> results = sqlExecutionService.executeQuery(sqlFile.getContent());
                
                if (results.isEmpty()) {
                    log.info("No results to process for this SQL file");
                    continue;
                }
                
                // Process first 2 rows with templates
                int rowsToProcess = Math.min(2, results.size());
                log.info("Processing first {} rows:", rowsToProcess);
                
                for (int i = 0; i < rowsToProcess; i++) {
                    Map<String, Object> rowData = results.get(i);
                    log.info("Row {}: {}", i + 1, rowData);
                    
                    // Process template for this row
                    ApiTemplateResult templateResult = templateProcessingService.processTemplate(templateName, rowData);
                    
                    // Log the results
                    log.info("Generated JSON for row {}:", i + 1);
                    log.info(templateResult.getJsonPayload());
                    
                    ApiEndpointInfo endpointInfo = templateResult.getEndpointInfo();
                    log.info("API Endpoint: {} {}", endpointInfo.getMethod(), endpointInfo.getRoute());
                    log.info("Headers: {}", endpointInfo.getHeaders());
                    log.info("URL Parameters: {}", endpointInfo.getUrlParams());
                }
                
                log.info("--------------------------------------");
            }
            
            log.info("=== Demo Complete ===");
            
        } catch (Exception e) {
            log.error("Error during template processing demo", e);
            exitCode = 1;
        }
    }
    
    @Override
    public int getExitCode() {
        return exitCode;
    }
}