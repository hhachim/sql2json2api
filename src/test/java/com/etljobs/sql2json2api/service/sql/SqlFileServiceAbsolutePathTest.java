package com.etljobs.sql2json2api.service.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.util.ReflectionTestUtils;

import com.etljobs.sql2json2api.config.SqlConfig;
import com.etljobs.sql2json2api.model.SqlFile;

/**
 * Tests for SqlFileService using absolute paths.
 * These tests create temporary files and directories.
 */
@SpringBootTest
class SqlFileServiceAbsolutePathTest {

    @Autowired
    private SqlFileService sqlFileService;
    
    @MockBean
    private SqlConfig sqlConfig;
    
    // Use JUnit 5's temporary directory feature
    @TempDir
    static Path tempDir;
    
    private static final String GET_USERS_SQL = 
            "SELECT id, username, email, created_at FROM users WHERE active = true ORDER BY created_at DESC";
    
    private static final String POST_ORDER_SQL = 
            "SELECT o.id as order_id, o.order_date, o.status FROM orders o";
    
    @BeforeAll
    static void setup() throws IOException {
        // Create test SQL files in the temporary directory
        Files.writeString(tempDir.resolve("GET_users.sql"), GET_USERS_SQL);
        Files.writeString(tempDir.resolve("POST_order.sql"), POST_ORDER_SQL);
        Files.writeString(tempDir.resolve("regular_file.sql"), "SELECT 1");
    }
    
    @Test
    void testListSqlFilesWithAbsolutePath() {
        // Inject the absolute path of the temp directory into the service
        ReflectionTestUtils.setField(sqlFileService, "sqlDirectory", tempDir.toString());
        
        // List SQL files
        List<SqlFile> sqlFiles = sqlFileService.listSqlFiles();
        
        // Verify results
        assertNotNull(sqlFiles);
        assertFalse(sqlFiles.isEmpty());
        assertEquals(3, sqlFiles.size(), "Should find 3 SQL files");
        
        // Verify specific files
        boolean foundGetUsers = false;
        boolean foundPostOrder = false;
        
        for (SqlFile file : sqlFiles) {
            if ("GET_users.sql".equals(file.getFileName())) {
                foundGetUsers = true;
                assertEquals("GET", file.getHttpMethod());
                assertEquals("users", file.getBaseName());
                assertEquals("GET_users.ftlh", file.getTemplateName());
                assertEquals(GET_USERS_SQL, file.getContent());
            } else if ("POST_order.sql".equals(file.getFileName())) {
                foundPostOrder = true;
                assertEquals("POST", file.getHttpMethod());
                assertEquals("order", file.getBaseName());
                assertEquals("POST_order.ftlh", file.getTemplateName());
                assertEquals(POST_ORDER_SQL, file.getContent());
            }
        }
        
        assertTrue(foundGetUsers, "Should find GET_users.sql");
        assertTrue(foundPostOrder, "Should find POST_order.sql");
    }
    
    @Test
    void testReadSqlFileWithAbsolutePath() {
        // Inject the absolute path
        ReflectionTestUtils.setField(sqlFileService, "sqlDirectory", tempDir.toString());
        
        // Read a specific file
        SqlFile sqlFile = sqlFileService.readSqlFile("GET_users.sql");
        
        // Verify results
        assertNotNull(sqlFile);
        assertEquals("GET_users.sql", sqlFile.getFileName());
        assertEquals("GET", sqlFile.getHttpMethod());
        assertEquals("users", sqlFile.getBaseName());
        assertEquals("GET_users.ftlh", sqlFile.getTemplateName());
        assertEquals(GET_USERS_SQL, sqlFile.getContent());
    }
    
    @Test
    void testGetSqlFilesInConfiguredOrderWithAbsolutePath() {
        // Inject the absolute path
        ReflectionTestUtils.setField(sqlFileService, "sqlDirectory", tempDir.toString());
        
        // Configure the order
        when(sqlConfig.getExecutionOrder()).thenReturn(List.of("POST_order.sql", "GET_users.sql"));
        
        // Get files in order
        List<SqlFile> orderedFiles = sqlFileService.getSqlFilesInConfiguredOrder();
        
        // Verify results
        assertNotNull(orderedFiles);
        assertEquals(2, orderedFiles.size(), "Should return 2 files in configured order");
        assertEquals("POST_order.sql", orderedFiles.get(0).getFileName(), "First file should be POST_order.sql");
        assertEquals("GET_users.sql", orderedFiles.get(1).getFileName(), "Second file should be GET_users.sql");
    }
}