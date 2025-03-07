package com.etljobs.sql2json2api.service.sql;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.etljobs.sql2json2api.model.SqlFile;

@SpringBootTest
public class SqlFileServiceTest {

    @Autowired
    private SqlFileService sqlFileService;
    
    @Test
    public void testListSqlFiles() {
        List<SqlFile> sqlFiles = sqlFileService.listSqlFiles();
        
        // Verify that the list is not empty
        assertFalse(sqlFiles.isEmpty(), "SQL files list should not be empty");
        
        // Verify that it contains our example files
        boolean foundGetUsersFile = sqlFiles.stream()
                .anyMatch(file -> file.getFileName().equals("GET_users.sql"));
        boolean foundPostOrderFile = sqlFiles.stream()
                .anyMatch(file -> file.getFileName().equals("POST_order.sql"));
        
        assertTrue(foundGetUsersFile, "Should find the GET_users.sql file");
        assertTrue(foundPostOrderFile, "Should find the POST_order.sql file");
    }
    
    @Test
    public void testReadSqlFile() {
        // Read the example file
        SqlFile sqlFile = sqlFileService.readSqlFile("GET_users.sql");
        
        // Verify file properties
        assertNotNull(sqlFile, "SQL file should not be null");
        assertEquals("GET_users.sql", sqlFile.getFileName(), "File name should match");
        assertEquals("GET", sqlFile.getHttpMethod(), "HTTP method should be GET");
        assertEquals("users", sqlFile.getBaseName(), "Base name should be users");
        assertEquals("GET_users.ftlh", sqlFile.getTemplateName(), "Template name should match");
        
        // Verify content
        assertNotNull(sqlFile.getContent(), "Content should not be null");
        assertTrue(sqlFile.getContent().contains("SELECT"), "Content should contain SELECT statement");
        assertTrue(sqlFile.getContent().contains("FROM users"), "Content should contain FROM users");
    }
}