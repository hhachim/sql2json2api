package com.etljobs.sql2json2api.runner;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;

import com.etljobs.sql2json2api.model.SqlFile;
import com.etljobs.sql2json2api.service.sql.SqlFileService;

import lombok.extern.slf4j.Slf4j;

/**
 * Command line runner for executing SQL file operations at startup.
 */
@Component
@Slf4j
public class SqlFileRunner implements CommandLineRunner, ExitCodeGenerator {
    
    private final SqlFileService sqlFileService;
    private int exitCode = 0;
    
    @Autowired
    public SqlFileRunner(SqlFileService sqlFileService) {
        this.sqlFileService = sqlFileService;
    }
    
    @Override
    public void run(String... args) throws Exception {
        try {
            log.info("Starting SQL file processing...");
            
            // List all SQL files
            List<SqlFile> sqlFiles = sqlFileService.listSqlFiles();
            log.info("Found {} SQL files:", sqlFiles.size());
            
            for (SqlFile sqlFile : sqlFiles) {
                log.info("File: {}, HTTP Method: {}, Base Name: {}, Template: {}", 
                        sqlFile.getFileName(), 
                        sqlFile.getHttpMethod(), 
                        sqlFile.getBaseName(), 
                        sqlFile.getTemplateName());
                
                // Log the first 100 characters of content for demonstration
                String contentPreview = sqlFile.getContent().length() > 100 
                        ? sqlFile.getContent().substring(0, 100) + "..." 
                        : sqlFile.getContent();
                log.info("Content preview: {}", contentPreview);
            }
            
            log.info("SQL file processing completed successfully");
            
        } catch (Exception e) {
            log.error("Error during SQL file processing", e);
            exitCode = 1;
        }
    }
    
    @Override
    public int getExitCode() {
        return exitCode;
    }
}