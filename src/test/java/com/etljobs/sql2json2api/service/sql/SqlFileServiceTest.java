package com.etljobs.sql2json2api.service.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import com.etljobs.sql2json2api.config.SqlConfig;
import com.etljobs.sql2json2api.exception.SqlFileException;
import com.etljobs.sql2json2api.model.SqlFile;

@SpringBootTest
@ActiveProfiles("test") 
public class SqlFileServiceTest {

    @Autowired
    private SqlFileService sqlFileService;
    
    @MockBean
    private SqlConfig sqlConfig;
    
    private SqlFileService spyService;
    
    @BeforeEach
    void setUp() {
        // Créer un spy du service
        spyService = spy(sqlFileService);
        
        // Configurer le spy pour retourner nos fichiers SQL de test
        List<SqlFile> testSqlFiles = createTestSqlFiles();
        doReturn(testSqlFiles).when(spyService).listSqlFiles();
    }
    
    private List<SqlFile> createTestSqlFiles() {
        List<SqlFile> testFiles = new ArrayList<>();
        
        SqlFile getUsersFile = SqlFile.builder()
                .fileName("GET_users.sql")
                .content("SELECT id, username, email, created_at FROM users WHERE active = true ORDER BY created_at DESC")
                .httpMethod("GET")
                .baseName("users")
                .templateName("GET_users.ftlh")
                .build();
        
        SqlFile postOrderFile = SqlFile.builder()
                .fileName("POST_order.sql")
                .content("SELECT o.id as order_id, o.order_date, o.status FROM orders o")
                .httpMethod("POST")
                .baseName("order")
                .templateName("POST_order.ftlh")
                .build();
        
        testFiles.add(getUsersFile);
        testFiles.add(postOrderFile);
        
        return testFiles;
    }
    
    @Test
    public void testListSqlFiles() {
        // Act - utiliser le spy qui retourne les fichiers de test
        List<SqlFile> sqlFiles = spyService.listSqlFiles();
        
        // Assert
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
        // Mock pour readSqlFile
        SqlFile getUsersFile = SqlFile.builder()
                .fileName("GET_users.sql")
                .content("SELECT id, username, email, created_at FROM users WHERE active = true ORDER BY created_at DESC")
                .httpMethod("GET")
                .baseName("users")
                .templateName("GET_users.ftlh")
                .build();
        
        doReturn(getUsersFile).when(spyService).readSqlFile("GET_users.sql");
        
        // Act
        SqlFile sqlFile = spyService.readSqlFile("GET_users.sql");
        
        // Assert
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
    
    // Tests pour getSqlFilesInConfiguredOrder
    
    @Test
    public void testGetSqlFilesInConfiguredOrder_WhenExecutionOrderIsEmpty() {
        // Configure mock pour retourner une liste vide
        when(sqlConfig.getExecutionOrder()).thenReturn(Collections.emptyList());
        
        // Configure le spy pour retourner notre liste de test
        doReturn(createTestSqlFiles()).when(spyService).listSqlFiles();
        
        // Act
        List<SqlFile> result = spyService.getSqlFilesInConfiguredOrder();
        
        // Assert
        assertFalse(result.isEmpty(), "Result should not be empty");
        assertTrue(result.stream().anyMatch(file -> file.getFileName().equals("GET_users.sql")));
        assertTrue(result.stream().anyMatch(file -> file.getFileName().equals("POST_order.sql")));
    }
    
    @Test
    public void testGetSqlFilesInConfiguredOrder_WhenExecutionOrderIsNull() {
        // Configure mock pour retourner null
        when(sqlConfig.getExecutionOrder()).thenReturn(null);
        
        // Configure le spy pour retourner notre liste de test
        doReturn(createTestSqlFiles()).when(spyService).listSqlFiles();
        
        // Act
        List<SqlFile> result = spyService.getSqlFilesInConfiguredOrder();
        
        // Assert
        assertFalse(result.isEmpty(), "Result should not be empty");
    }
    
    @Test
    public void testGetSqlFilesInConfiguredOrder_ShouldRespectConfiguredOrder() {
        // Configure mock pour retourner un ordre spécifique
        when(sqlConfig.getExecutionOrder()).thenReturn(Arrays.asList("POST_order.sql", "GET_users.sql"));
        
        // Mock pour readSqlFile
        SqlFile getUsersFile = SqlFile.builder()
                .fileName("GET_users.sql")
                .content("SELECT id, username, email, created_at FROM users")
                .httpMethod("GET")
                .baseName("users")
                .templateName("GET_users.ftlh")
                .build();
        
        SqlFile postOrderFile = SqlFile.builder()
                .fileName("POST_order.sql")
                .content("SELECT o.id as order_id, o.order_date, o.status FROM orders o")
                .httpMethod("POST")
                .baseName("order")
                .templateName("POST_order.ftlh")
                .build();
        
        doReturn(postOrderFile).when(spyService).readSqlFile("POST_order.sql");
        doReturn(getUsersFile).when(spyService).readSqlFile("GET_users.sql");
        
        // Act
        List<SqlFile> result = spyService.getSqlFilesInConfiguredOrder();
        
        // Assert
        assertFalse(result.isEmpty(), "Result should not be empty");
        assertEquals(2, result.size(), "Should return only the configured files");
        assertEquals("POST_order.sql", result.get(0).getFileName(), "First file should be POST_order.sql");
        assertEquals("GET_users.sql", result.get(1).getFileName(), "Second file should be GET_users.sql");
    }
    
   @Test
    public void testGetSqlFilesInConfiguredOrder_ShouldSkipNonExistentFiles() {
        // Créer un spy du service
        SqlFileService spyService = spy(sqlFileService);
        
        // Configure mock pour retourner une liste avec un fichier non existant
        when(sqlConfig.getExecutionOrder()).thenReturn(Arrays.asList("POST_order.sql", "NONEXISTENT.sql", "GET_users.sql"));
        
        // Configurer les mocks pour les fichiers qui existent
        SqlFile postOrderFile = SqlFile.builder()
                .fileName("POST_order.sql")
                .content("SELECT o.id as order_id, o.order_date, o.status FROM orders o")
                .httpMethod("POST")
                .baseName("order")
                .templateName("POST_order.ftlh")
                .build();
                
        SqlFile getUsersFile = SqlFile.builder()
                .fileName("GET_users.sql")
                .content("SELECT id, username, email, created_at FROM users")
                .httpMethod("GET")
                .baseName("users")
                .templateName("GET_users.ftlh")
                .build();
                
        // Simuler le comportement de readSqlFile
        doReturn(postOrderFile).when(spyService).readSqlFile("POST_order.sql");
        doReturn(getUsersFile).when(spyService).readSqlFile("GET_users.sql");
        doThrow(new SqlFileException("File not found")).when(spyService).readSqlFile("NONEXISTENT.sql");
        
        // Exécuter la méthode à tester
        List<SqlFile> result = spyService.getSqlFilesInConfiguredOrder();
        
        // Vérifier que seuls les fichiers existants sont retournés
        assertEquals(2, result.size(), "Should return only existing files");
        assertEquals("POST_order.sql", result.get(0).getFileName(), "First file should be POST_order.sql");
        assertEquals("GET_users.sql", result.get(1).getFileName(), "Second file should be GET_users.sql");
        
        // Vérifier que toutes les méthodes ont été appelées
        verify(spyService).readSqlFile("POST_order.sql");
        verify(spyService).readSqlFile("NONEXISTENT.sql");
        verify(spyService).readSqlFile("GET_users.sql");
    }
}