package com.etljobs.sql2json2api.service.orchestration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

import com.etljobs.sql2json2api.model.ApiResponse;
import com.etljobs.sql2json2api.model.SqlFile;
import com.etljobs.sql2json2api.service.http.TokenService;
import com.etljobs.sql2json2api.service.sql.SqlExecutionService;
import com.etljobs.sql2json2api.service.sql.SqlFileService;

class ProcessOrchestratorTest {

    @Mock
    private SqlFileService sqlFileService;

    @Mock
    private SqlExecutionService sqlExecutionService;

    @Mock
    private TokenService tokenService;
    
    @Mock
    private RetryStrategyFactory retryStrategyFactory;
    
    @Mock
    private RetryStrategy retryStrategy;
    
    @Mock
    private RowProcessor rowProcessor;

    private ProcessOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Configuration du RetryStrategyFactory mock
        when(retryStrategyFactory.create()).thenReturn(retryStrategy);
        when(retryStrategy.getMaxAttempts()).thenReturn(3);
        
        orchestrator = new ProcessOrchestrator(
                sqlFileService,
                sqlExecutionService,
                tokenService,
                retryStrategyFactory,
                rowProcessor
        );
    }

    @Test
    void processSqlFile_ShouldReadAndExecuteSQL() {
        // Arrange
        String sqlFileName = "GET_users.sql";

        // Créer un SqlFile de test
        SqlFile sqlFile = SqlFile
                .builder()
                .fileName(sqlFileName)
                .content("SELECT * FROM users")
                .httpMethod("GET")
                .baseName("users")
                .templateName("GET_users.ftlh")
                .build();

        List<Map<String, Object>> results = new ArrayList<>();
        Map<String, Object> row = new HashMap<>();
        row.put("id", 1);
        row.put("username", "testuser");
        results.add(row);

        // Préparer la réponse API
        ApiResponse apiResponse = ApiResponse
                .builder()
                .statusCode(200)
                .body("{\"status\":\"success\"}")
                .build();

        when(sqlFileService.readSqlFile(sqlFileName)).thenReturn(sqlFile);
        when(sqlExecutionService.executeQuery(sqlFile.getContent()))
                .thenReturn(results);
        when(tokenService.getToken()).thenReturn("Bearer token123");
        
        // Configurer le RowProcessor mock
        when(rowProcessor.processRow(
                eq(sqlFile), 
                eq(row), 
                eq(0), 
                anyString(), 
                eq(retryStrategy), 
                any())).thenReturn(apiResponse);

        // Act
        List<ApiResponse> response = orchestrator.processSqlFile(sqlFileName);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals(apiResponse, response.get(0));

        // Vérifier que processRow a été appelé avec les bons arguments
        verify(rowProcessor).processRow(
                eq(sqlFile), 
                eq(row), 
                eq(0), 
                anyString(), 
                eq(retryStrategy), 
                any());
    }

    @Test
    void processSqlFile_ShouldProcessMultipleRows() {
        // Arrange
        String sqlFileName = "GET_users.sql";

        // Créer un SqlFile de test
        SqlFile sqlFile = SqlFile
                .builder()
                .fileName(sqlFileName)
                .content("SELECT * FROM users")
                .httpMethod("GET")
                .baseName("users")
                .templateName("GET_users.ftlh")
                .build();

        // Préparer plusieurs lignes de résultat SQL
        List<Map<String, Object>> results = new ArrayList<>();

        Map<String, Object> row1 = new HashMap<>();
        row1.put("id", 1);
        row1.put("username", "user1");
        results.add(row1);

        Map<String, Object> row2 = new HashMap<>();
        row2.put("id", 2);
        row2.put("username", "user2");
        results.add(row2);

        Map<String, Object> row3 = new HashMap<>();
        row3.put("id", 3);
        row3.put("username", "user3");
        results.add(row3);

        // Préparer les réponses API
        ApiResponse apiResponse1 = ApiResponse
                .builder()
                .statusCode(200)
                .body("{\"id\":1,\"status\":\"success\"}")
                .build();

        ApiResponse apiResponse2 = ApiResponse
                .builder()
                .statusCode(200)
                .body("{\"id\":2,\"status\":\"success\"}")
                .build();

        ApiResponse apiResponse3 = ApiResponse
                .builder()
                .statusCode(200)
                .body("{\"id\":3,\"status\":\"success\"}")
                .build();

        // Configurer les mocks
        when(sqlFileService.readSqlFile(sqlFileName)).thenReturn(sqlFile);
        when(sqlExecutionService.executeQuery(sqlFile.getContent()))
                .thenReturn(results);
        when(tokenService.getToken()).thenReturn("Bearer token123");

        // Configurer le RowProcessor pour chaque ligne
        when(rowProcessor.processRow(
                eq(sqlFile), eq(row1), eq(0), anyString(), eq(retryStrategy), any()))
                .thenReturn(apiResponse1);
        
        when(rowProcessor.processRow(
                eq(sqlFile), eq(row2), eq(1), anyString(), eq(retryStrategy), any()))
                .thenReturn(apiResponse2);
        
        when(rowProcessor.processRow(
                eq(sqlFile), eq(row3), eq(2), anyString(), eq(retryStrategy), any()))
                .thenReturn(apiResponse3);

        // Act
        List<ApiResponse> responses = orchestrator.processSqlFile(sqlFileName);

        // Assert
        assertNotNull(responses);
        assertEquals(3, responses.size());
        assertEquals(apiResponse1, responses.get(0));
        assertEquals(apiResponse2, responses.get(1));
        assertEquals(apiResponse3, responses.get(2));

        // Vérifier que processRow a été appelé pour chaque ligne
        verify(rowProcessor, times(3)).processRow(
                eq(sqlFile), any(), anyInt(), anyString(), eq(retryStrategy), any());
    }

    @Test
    void processSqlFile_ShouldReturnEmptyListWhenNoResults() {
        // Arrange
        String sqlFileName = "GET_empty.sql";

        // Créer un SqlFile de test
        SqlFile sqlFile = SqlFile
                .builder()
                .fileName(sqlFileName)
                .content("SELECT * FROM users WHERE 1=0")
                .httpMethod("GET")
                .baseName("empty")
                .templateName("GET_empty.ftlh")
                .build();

        // Aucun résultat SQL
        List<Map<String, Object>> emptyResults = new ArrayList<>();

        // Configurer les mocks
        when(sqlFileService.readSqlFile(sqlFileName)).thenReturn(sqlFile);
        when(sqlExecutionService.executeQuery(sqlFile.getContent()))
                .thenReturn(emptyResults);

        // Act
        List<ApiResponse> responses = orchestrator.processSqlFile(sqlFileName);

        // Assert
        assertNotNull(responses);
        assertEquals(0, responses.size());

        // Vérifier que le token n'est pas généré
        verify(tokenService, times(0)).getToken();

        // Vérifier qu'aucun traitement de ligne n'est effectué
        verify(rowProcessor, times(0)).processRow(
                any(), any(), anyInt(), anyString(), any(), any());
    }

    @Test
    void processSqlFile_ShouldContinueProcessingOnErrors() {
        // Arrange
        String sqlFileName = "GET_users_with_errors.sql";

        // Créer un SqlFile de test
        SqlFile sqlFile = SqlFile
                .builder()
                .fileName(sqlFileName)
                .content("SELECT * FROM users")
                .httpMethod("GET")
                .baseName("users_with_errors")
                .templateName("GET_users.ftlh")
                .build();

        // Préparer plusieurs lignes de résultat SQL
        List<Map<String, Object>> results = new ArrayList<>();

        Map<String, Object> row1 = new HashMap<>();
        row1.put("id", 1);
        row1.put("username", "user1");
        results.add(row1);

        Map<String, Object> row2 = new HashMap<>();
        row2.put("id", 2);
        row2.put("username", "user2");
        results.add(row2);

        Map<String, Object> row3 = new HashMap<>();
        row3.put("id", 3);
        row3.put("username", "user3");
        results.add(row3);

        // Préparer les réponses API
        ApiResponse apiResponse1 = ApiResponse
                .builder()
                .statusCode(200)
                .body("{\"id\":1,\"status\":\"success\"}")
                .build();

        ApiResponse apiResponse3 = ApiResponse
                .builder()
                .statusCode(200)
                .body("{\"id\":3,\"status\":\"success\"}")
                .build();

        // Configurer les mocks
        when(sqlFileService.readSqlFile(sqlFileName)).thenReturn(sqlFile);
        when(sqlExecutionService.executeQuery(sqlFile.getContent()))
                .thenReturn(results);
        when(tokenService.getToken()).thenReturn("Bearer token123");
        
        // Configurer le RowProcessor pour simuler un succès, null (erreur) et succès
        when(rowProcessor.processRow(
                eq(sqlFile), eq(row1), eq(0), anyString(), eq(retryStrategy), any()))
                .thenReturn(apiResponse1);
        
        // La deuxième ligne retourne null (simulant une erreur)
        when(rowProcessor.processRow(
                eq(sqlFile), eq(row2), eq(1), anyString(), eq(retryStrategy), any()))
                .thenReturn(null);
        
        when(rowProcessor.processRow(
                eq(sqlFile), eq(row3), eq(2), anyString(), eq(retryStrategy), any()))
                .thenReturn(apiResponse3);

        // Act
        List<ApiResponse> responses = orchestrator.processSqlFile(sqlFileName);

        // Assert
        assertNotNull(responses);
        assertEquals(2, responses.size()); // Seulement 2 réponses car la ligne 2 retourne null
        assertEquals(apiResponse1, responses.get(0));
        assertEquals(apiResponse3, responses.get(1));

        // Vérifier que processRow a été appelé pour chaque ligne
        verify(rowProcessor).processRow(
                eq(sqlFile), eq(row1), eq(0), anyString(), eq(retryStrategy), any());
        verify(rowProcessor).processRow(
                eq(sqlFile), eq(row2), eq(1), anyString(), eq(retryStrategy), any());
        verify(rowProcessor).processRow(
                eq(sqlFile), eq(row3), eq(2), anyString(), eq(retryStrategy), any());
    }
}