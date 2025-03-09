package com.etljobs.sql2json2api.service.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.util.ReflectionTestUtils;

import com.etljobs.sql2json2api.config.SqlConfig;
import com.etljobs.sql2json2api.exception.SqlFileException;
import com.etljobs.sql2json2api.model.SqlFile;

@SpringBootTest
public class SqlFileServiceTest {

    @Autowired
    private SqlFileService sqlFileService;
    
    @MockBean
    private SqlConfig sqlConfig;
    
    // Tests existants pour les méthodes originales
    
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
    
    // Nouveaux tests pour getSqlFilesInConfiguredOrder
    
    @Test
    public void testGetSqlFilesInConfiguredOrder_WhenExecutionOrderIsEmpty() {
        // Configure mock pour retourner une liste vide
        when(sqlConfig.getExecutionOrder()).thenReturn(Collections.emptyList());
        
        // Exécuter la méthode à tester
        List<SqlFile> result = sqlFileService.getSqlFilesInConfiguredOrder();
        
        // Vérifier que tous les fichiers sont retournés (même comportement que listSqlFiles)
        assertFalse(result.isEmpty(), "Result should not be empty");
        assertTrue(result.stream().anyMatch(file -> file.getFileName().equals("GET_users.sql")));
        assertTrue(result.stream().anyMatch(file -> file.getFileName().equals("POST_order.sql")));
    }
    
    @Test
    public void testGetSqlFilesInConfiguredOrder_WhenExecutionOrderIsNull() {
        // Configure mock pour retourner null
        when(sqlConfig.getExecutionOrder()).thenReturn(null);
        
        // Exécuter la méthode à tester
        List<SqlFile> result = sqlFileService.getSqlFilesInConfiguredOrder();
        
        // Vérifier que tous les fichiers sont retournés
        assertFalse(result.isEmpty(), "Result should not be empty");
    }
    
    @Test
    public void testGetSqlFilesInConfiguredOrder_ShouldRespectConfiguredOrder() {
        // Configure mock pour retourner un ordre spécifique
        when(sqlConfig.getExecutionOrder()).thenReturn(Arrays.asList("POST_order.sql", "GET_users.sql"));
        
        // Exécuter la méthode à tester
        List<SqlFile> result = sqlFileService.getSqlFilesInConfiguredOrder();
        
        // Vérifier que l'ordre est respecté
        assertFalse(result.isEmpty(), "Result should not be empty");
        assertEquals(2, result.size(), "Should return only the configured files");
        assertEquals("POST_order.sql", result.get(0).getFileName(), "First file should be POST_order.sql");
        assertEquals("GET_users.sql", result.get(1).getFileName(), "Second file should be GET_users.sql");
    }
    
    @Test
    public void testGetSqlFilesInConfiguredOrder_ShouldSkipNonExistentFiles() {
        // Configure mock pour retourner une liste avec un fichier non existant
        when(sqlConfig.getExecutionOrder()).thenReturn(Arrays.asList("POST_order.sql", "NONEXISTENT.sql", "GET_users.sql"));
        
        // Exécuter la méthode à tester
        List<SqlFile> result = sqlFileService.getSqlFilesInConfiguredOrder();
        
        // Vérifier que seuls les fichiers existants sont retournés
        assertEquals(2, result.size(), "Should return only existing files");
        assertEquals("POST_order.sql", result.get(0).getFileName());
        assertEquals("GET_users.sql", result.get(1).getFileName());
    }
}