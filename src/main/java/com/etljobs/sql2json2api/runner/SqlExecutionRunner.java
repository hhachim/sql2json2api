package com.etljobs.sql2json2api.runner;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.etljobs.sql2json2api.model.SqlFile;
import com.etljobs.sql2json2api.service.sql.SqlExecutionService;
import com.etljobs.sql2json2api.service.sql.SqlFileService;

import lombok.extern.slf4j.Slf4j;

/**
 * Command line runner for testing SQL execution functionality.
 * This will run when the 'sql-execution-demo' profile is active.
 */
@Component
@Slf4j
@Profile("sql-execution-demo")
public class SqlExecutionRunner implements CommandLineRunner, ExitCodeGenerator {
    
    private final SqlFileService sqlFileService;
    private final SqlExecutionService sqlExecutionService;
    private int exitCode = 0;
    
    @Autowired
    public SqlExecutionRunner(SqlFileService sqlFileService, SqlExecutionService sqlExecutionService) {
        this.sqlFileService = sqlFileService;
        this.sqlExecutionService = sqlExecutionService;
    }
    
    @Override
    public void run(String... args) throws Exception {
        try {
            log.info("=== SQL Execution Demo ===");
            
            // List available SQL files
            List<SqlFile> sqlFiles = sqlFileService.listSqlFiles();
            log.info("Available SQL files:");
            sqlFiles.forEach(file -> log.info("- {}", file.getFileName()));
            
            // Execute each available SQL file
            for (SqlFile sqlFile : sqlFiles) {
                String fileName = sqlFile.getFileName();
                log.info("\nExecuting SQL file: {}", fileName);
                
                // Directly use the content from sqlFile instead of reading it again
                List<Map<String, Object>> results = sqlExecutionService.executeQuery(sqlFile.getContent());
                
                log.info("\nExecution Results for {}:", fileName);
                if (results.isEmpty()) {
                    log.info("No results returned");
                } else {
                    // Print column names (keys from first result map)
                    Map<String, Object> firstRow = results.get(0);
                    log.info("Columns: {}", String.join(", ", firstRow.keySet()));
                    
                    // Print first few rows
                    int rowsToDisplay = Math.min(5, results.size());
                    log.info("First {} rows:", rowsToDisplay);
                    for (int i = 0; i < rowsToDisplay; i++) {
                        log.info("Row {}: {}", i + 1, results.get(i));
                    }
                    
                    log.info("Total rows: {}", results.size());
                }
                
                log.info("--------------------------------------");
            }
            
            // Execute a simple count query to test the count functionality
            int userCount = sqlExecutionService.executeCountQuery("SELECT COUNT(*) FROM users");
            log.info("Total users in database: {}", userCount);
            
            log.info("=== Demo Complete ===");
            
        } catch (Exception e) {
            log.error("Error during SQL execution demo", e);
            exitCode = 1;
        }
    }
    
    @Override
    public int getExitCode() {
        return exitCode;
    }
}