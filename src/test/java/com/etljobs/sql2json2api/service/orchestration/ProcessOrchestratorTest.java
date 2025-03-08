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
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpMethod;
import org.springframework.test.util.ReflectionTestUtils;

import com.etljobs.sql2json2api.exception.TemplateProcessingException;
import com.etljobs.sql2json2api.model.ApiEndpointInfo;
import com.etljobs.sql2json2api.model.ApiResponse;
import com.etljobs.sql2json2api.model.ApiTemplateResult;
import com.etljobs.sql2json2api.model.SqlFile;
import com.etljobs.sql2json2api.service.http.ApiClientService;
import com.etljobs.sql2json2api.service.http.TokenService;
import com.etljobs.sql2json2api.service.sql.SqlExecutionService;
import com.etljobs.sql2json2api.service.sql.SqlFileService;
import com.etljobs.sql2json2api.service.template.TemplateProcessingService;

class ProcessOrchestratorTest {

    @Mock
    private SqlFileService sqlFileService;

    @Mock
    private SqlExecutionService sqlExecutionService;

    @Mock
    private TemplateProcessingService templateService;

    @Mock
    private TokenService tokenService;

    @Mock
    private ApiClientService apiClientService;

    private ProcessOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        orchestrator = new ProcessOrchestrator(
                sqlFileService,
                sqlExecutionService,
                templateService,
                tokenService,
                apiClientService
        );
    }

    @Test
    void processSqlFile_ShouldReadAndExecuteSQL() {
        // Arrange
        String sqlFileName = "GET_users.sql";
        
        // Créer un SqlFile de test
        SqlFile sqlFile = SqlFile.builder()
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

        // Préparer le résultat du template et les infos d'endpoint
        ApiEndpointInfo endpointInfo = new ApiEndpointInfo();
        endpointInfo.setRoute("/api/users/1");
        endpointInfo.setMethod(HttpMethod.GET);
        endpointInfo.setHeaders(Map.of("Content-Type", "application/json"));
        
        String jsonPayload = "{\"user\":{\"id\":1,\"username\":\"testuser\"}}";
        ApiTemplateResult templateResult = new ApiTemplateResult(jsonPayload, endpointInfo);
        
        // Préparer la réponse API
        ApiResponse apiResponse = ApiResponse.builder()
                .statusCode(200)
                .body("{\"status\":\"success\"}")
                .build();

        when(sqlFileService.readSqlFile(sqlFileName)).thenReturn(sqlFile);
        when(sqlExecutionService.executeQuery(sqlFile.getContent())).thenReturn(results);
        when(tokenService.getToken()).thenReturn("Bearer token123");
        when(templateService.processTemplate(sqlFile.getTemplateName(), row)).thenReturn(templateResult);
        when(apiClientService.callApi(
                endpointInfo.getRoute(),
                endpointInfo.getMethod(),
                jsonPayload,
                endpointInfo.getHeaders(),
                endpointInfo.getUrlParams()
        )).thenReturn(apiResponse);

        // Act
        var response = orchestrator.processSqlFile(sqlFileName);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals(apiResponse, response.get(0));
    }

    @Test
    void processSqlFile_ShouldProcessSingleRow() {
        // Arrange
        String sqlFileName = "GET_users.sql";
        
        // Créer un SqlFile de test
        SqlFile sqlFile = SqlFile.builder()
                .fileName(sqlFileName)
                .content("SELECT * FROM users")
                .httpMethod("GET")
                .baseName("users")
                .templateName("GET_users.ftlh")
                .build();
        
        // Préparer une ligne de résultat SQL
        List<Map<String, Object>> results = new ArrayList<>();
        Map<String, Object> row = new HashMap<>();
        row.put("id", 1);
        row.put("username", "testuser");
        results.add(row);
        
        // Préparer le résultat du template et les infos d'endpoint
        ApiEndpointInfo endpointInfo = new ApiEndpointInfo();
        endpointInfo.setRoute("/api/users/1");
        endpointInfo.setMethod(HttpMethod.GET);
        endpointInfo.setHeaders(Map.of("Content-Type", "application/json"));
        
        String jsonPayload = "{\"user\":{\"id\":1,\"username\":\"testuser\"}}";
        ApiTemplateResult templateResult = new ApiTemplateResult(jsonPayload, endpointInfo);
        
        // Préparer la réponse API
        ApiResponse apiResponse = ApiResponse.builder()
                .statusCode(200)
                .body("{\"status\":\"success\"}")
                .build();
        
        // Configurer les mocks
        when(sqlFileService.readSqlFile(sqlFileName)).thenReturn(sqlFile);
        when(sqlExecutionService.executeQuery(sqlFile.getContent())).thenReturn(results);
        when(tokenService.getToken()).thenReturn("Bearer token123");
        when(templateService.processTemplate(sqlFile.getTemplateName(), row)).thenReturn(templateResult);
        when(apiClientService.callApi(
                endpointInfo.getRoute(),
                endpointInfo.getMethod(),
                jsonPayload,
                endpointInfo.getHeaders(),
                endpointInfo.getUrlParams()
        )).thenReturn(apiResponse);
        
        // Act
        List<ApiResponse> responses = orchestrator.processSqlFile(sqlFileName);
        
        // Assert
        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(apiResponse, responses.get(0));
        
        // Vérifier les appels
        verify(tokenService, times(1)).getToken();
        verify(templateService, times(1)).processTemplate(any(), any());
        verify(apiClientService, times(1)).callApi(any(), any(), any(), any(), any());
    }
    
    @Test
    void processSqlFile_ShouldProcessMultipleRows() {
        // Arrange
        String sqlFileName = "GET_users.sql";
        
        // Créer un SqlFile de test
        SqlFile sqlFile = SqlFile.builder()
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
        
        // Préparer les résultats du template pour chaque ligne
        ApiEndpointInfo endpointInfo1 = new ApiEndpointInfo();
        endpointInfo1.setRoute("/api/users/1");
        endpointInfo1.setMethod(HttpMethod.GET);
        endpointInfo1.setHeaders(Map.of("Content-Type", "application/json"));
        
        ApiEndpointInfo endpointInfo2 = new ApiEndpointInfo();
        endpointInfo2.setRoute("/api/users/2");
        endpointInfo2.setMethod(HttpMethod.GET);
        endpointInfo2.setHeaders(Map.of("Content-Type", "application/json"));
        
        ApiEndpointInfo endpointInfo3 = new ApiEndpointInfo();
        endpointInfo3.setRoute("/api/users/3");
        endpointInfo3.setMethod(HttpMethod.GET);
        endpointInfo3.setHeaders(Map.of("Content-Type", "application/json"));
        
        String jsonPayload1 = "{\"user\":{\"id\":1,\"username\":\"user1\"}}";
        String jsonPayload2 = "{\"user\":{\"id\":2,\"username\":\"user2\"}}";
        String jsonPayload3 = "{\"user\":{\"id\":3,\"username\":\"user3\"}}";
        
        ApiTemplateResult templateResult1 = new ApiTemplateResult(jsonPayload1, endpointInfo1);
        ApiTemplateResult templateResult2 = new ApiTemplateResult(jsonPayload2, endpointInfo2);
        ApiTemplateResult templateResult3 = new ApiTemplateResult(jsonPayload3, endpointInfo3);
        
        // Préparer les réponses API
        ApiResponse apiResponse1 = ApiResponse.builder()
                .statusCode(200)
                .body("{\"id\":1,\"status\":\"success\"}")
                .build();
                
        ApiResponse apiResponse2 = ApiResponse.builder()
                .statusCode(200)
                .body("{\"id\":2,\"status\":\"success\"}")
                .build();
                
        ApiResponse apiResponse3 = ApiResponse.builder()
                .statusCode(200)
                .body("{\"id\":3,\"status\":\"success\"}")
                .build();
        
        // Configurer les mocks
        when(sqlFileService.readSqlFile(sqlFileName)).thenReturn(sqlFile);
        when(sqlExecutionService.executeQuery(sqlFile.getContent())).thenReturn(results);
        when(tokenService.getToken()).thenReturn("Bearer token123");
        
        when(templateService.processTemplate(eq(sqlFile.getTemplateName()), eq(row1))).thenReturn(templateResult1);
        when(templateService.processTemplate(eq(sqlFile.getTemplateName()), eq(row2))).thenReturn(templateResult2);
        when(templateService.processTemplate(eq(sqlFile.getTemplateName()), eq(row3))).thenReturn(templateResult3);
        
        when(apiClientService.callApi(
                eq(endpointInfo1.getRoute()),
                eq(endpointInfo1.getMethod()),
                eq(jsonPayload1),
                eq(endpointInfo1.getHeaders()),
                eq(endpointInfo1.getUrlParams())
        )).thenReturn(apiResponse1);
        
        when(apiClientService.callApi(
                eq(endpointInfo2.getRoute()),
                eq(endpointInfo2.getMethod()),
                eq(jsonPayload2),
                eq(endpointInfo2.getHeaders()),
                eq(endpointInfo2.getUrlParams())
        )).thenReturn(apiResponse2);
        
        when(apiClientService.callApi(
                eq(endpointInfo3.getRoute()),
                eq(endpointInfo3.getMethod()),
                eq(jsonPayload3),
                eq(endpointInfo3.getHeaders()),
                eq(endpointInfo3.getUrlParams())
        )).thenReturn(apiResponse3);
        
        // Act
        List<ApiResponse> responses = orchestrator.processSqlFile(sqlFileName);
        
        // Assert
        assertNotNull(responses);
        assertEquals(3, responses.size());
        assertEquals(apiResponse1, responses.get(0));
        assertEquals(apiResponse2, responses.get(1));
        assertEquals(apiResponse3, responses.get(2));
        
        // Vérifier que le token n'est généré qu'une seule fois
        verify(tokenService, times(1)).getToken();
        
        // Vérifier que chaque ligne est traitée
        verify(templateService, times(1)).processTemplate(eq(sqlFile.getTemplateName()), eq(row1));
        verify(templateService, times(1)).processTemplate(eq(sqlFile.getTemplateName()), eq(row2));
        verify(templateService, times(1)).processTemplate(eq(sqlFile.getTemplateName()), eq(row3));
        
        // Vérifier que chaque appel API est effectué
        verify(apiClientService, times(1)).callApi(
                eq(endpointInfo1.getRoute()),
                eq(endpointInfo1.getMethod()),
                eq(jsonPayload1),
                eq(endpointInfo1.getHeaders()),
                eq(endpointInfo1.getUrlParams()));
        
        verify(apiClientService, times(1)).callApi(
                eq(endpointInfo2.getRoute()),
                eq(endpointInfo2.getMethod()),
                eq(jsonPayload2),
                eq(endpointInfo2.getHeaders()),
                eq(endpointInfo2.getUrlParams()));
        
        verify(apiClientService, times(1)).callApi(
                eq(endpointInfo3.getRoute()),
                eq(endpointInfo3.getMethod()),
                eq(jsonPayload3),
                eq(endpointInfo3.getHeaders()),
                eq(endpointInfo3.getUrlParams()));
    }
    
    @Test
    void processSqlFile_ShouldReturnEmptyListWhenNoResults() {
        // Arrange
        String sqlFileName = "GET_empty.sql";
        
        // Créer un SqlFile de test
        SqlFile sqlFile = SqlFile.builder()
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
        when(sqlExecutionService.executeQuery(sqlFile.getContent())).thenReturn(emptyResults);
        
        // Act
        List<ApiResponse> responses = orchestrator.processSqlFile(sqlFileName);
        
        // Assert
        assertNotNull(responses);
        assertEquals(0, responses.size());
        
        // Vérifier que le token n'est pas généré
        verify(tokenService, times(0)).getToken();
        
        // Vérifier qu'aucun template n'est traité
        verify(templateService, times(0)).processTemplate(any(), any());
        
        // Vérifier qu'aucun appel API n'est effectué
        verify(apiClientService, times(0)).callApi(any(), any(), any(), any(), any());
    }
    
    @Test
    void processSqlFile_ShouldContinueProcessingOnErrors() {
        // Arrange
        String sqlFileName = "GET_users_with_errors.sql";
        
        // Créer un SqlFile de test
        SqlFile sqlFile = SqlFile.builder()
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
        
        // Préparer les résultats du template et les infos d'endpoint
        ApiEndpointInfo endpointInfo1 = new ApiEndpointInfo();
        endpointInfo1.setRoute("/api/users/1");
        endpointInfo1.setMethod(HttpMethod.GET);
        endpointInfo1.setHeaders(Map.of("Content-Type", "application/json"));
        
        ApiEndpointInfo endpointInfo3 = new ApiEndpointInfo();
        endpointInfo3.setRoute("/api/users/3");
        endpointInfo3.setMethod(HttpMethod.GET);
        endpointInfo3.setHeaders(Map.of("Content-Type", "application/json"));
        
        String jsonPayload1 = "{\"user\":{\"id\":1,\"username\":\"user1\"}}";
        String jsonPayload3 = "{\"user\":{\"id\":3,\"username\":\"user3\"}}";
        
        ApiTemplateResult templateResult1 = new ApiTemplateResult(jsonPayload1, endpointInfo1);
        ApiTemplateResult templateResult3 = new ApiTemplateResult(jsonPayload3, endpointInfo3);
        
        // Préparer les réponses API
        ApiResponse apiResponse1 = ApiResponse.builder()
                .statusCode(200)
                .body("{\"id\":1,\"status\":\"success\"}")
                .build();
                
        ApiResponse apiResponse3 = ApiResponse.builder()
                .statusCode(200)
                .body("{\"id\":3,\"status\":\"success\"}")
                .build();
        
        // Configurer les mocks
        when(sqlFileService.readSqlFile(sqlFileName)).thenReturn(sqlFile);
        when(sqlExecutionService.executeQuery(sqlFile.getContent())).thenReturn(results);
        when(tokenService.getToken()).thenReturn("Bearer token123");
        
        // La ligne 1 réussit
        when(templateService.processTemplate(eq(sqlFile.getTemplateName()), eq(row1))).thenReturn(templateResult1);
        when(apiClientService.callApi(
                eq(endpointInfo1.getRoute()),
                eq(endpointInfo1.getMethod()),
                eq(jsonPayload1),
                eq(endpointInfo1.getHeaders()),
                eq(endpointInfo1.getUrlParams())
        )).thenReturn(apiResponse1);
        
        // La ligne 2 échoue lors du traitement du template
        when(templateService.processTemplate(eq(sqlFile.getTemplateName()), eq(row2)))
                .thenThrow(new TemplateProcessingException("Erreur de template pour la ligne 2"));
        
        // La ligne 3 réussit
        when(templateService.processTemplate(eq(sqlFile.getTemplateName()), eq(row3))).thenReturn(templateResult3);
        when(apiClientService.callApi(
                eq(endpointInfo3.getRoute()),
                eq(endpointInfo3.getMethod()),
                eq(jsonPayload3),
                eq(endpointInfo3.getHeaders()),
                eq(endpointInfo3.getUrlParams())
        )).thenReturn(apiResponse3);
        
        // Act
        List<ApiResponse> responses = orchestrator.processSqlFile(sqlFileName);
        
        // Assert
        assertNotNull(responses);
        assertEquals(2, responses.size()); // Seulement 2 réponses car la ligne 2 a échoué
        assertEquals(apiResponse1, responses.get(0));
        assertEquals(apiResponse3, responses.get(1));
        
        // Vérifier que le token n'est généré qu'une seule fois
        verify(tokenService, times(1)).getToken();
        
        // Vérifier que les trois lignes sont traitées ou tentées
        verify(templateService, times(1)).processTemplate(eq(sqlFile.getTemplateName()), eq(row1));
        verify(templateService, times(1)).processTemplate(eq(sqlFile.getTemplateName()), eq(row2));
        verify(templateService, times(1)).processTemplate(eq(sqlFile.getTemplateName()), eq(row3));
        
        // Vérifier que seulement 2 appels API sont effectués (pas pour la ligne 2)
        verify(apiClientService, times(1)).callApi(
                eq(endpointInfo1.getRoute()),
                eq(endpointInfo1.getMethod()),
                eq(jsonPayload1),
                eq(endpointInfo1.getHeaders()),
                eq(endpointInfo1.getUrlParams()));
        
        verify(apiClientService, times(1)).callApi(
                eq(endpointInfo3.getRoute()),
                eq(endpointInfo3.getMethod()),
                eq(jsonPayload3),
                eq(endpointInfo3.getHeaders()),
                eq(endpointInfo3.getUrlParams()));
    }
    
    @Test
    void processSqlFile_ShouldImplementRetryStrategy() {
        // Arrange
        String sqlFileName = "GET_users_with_retry.sql";
        
        // Créer un SqlFile de test
        SqlFile sqlFile = SqlFile.builder()
                .fileName(sqlFileName)
                .content("SELECT * FROM users")
                .httpMethod("GET")
                .baseName("users_with_retry")
                .templateName("GET_users.ftlh")
                .build();
        
        // Préparer une ligne de résultat SQL
        List<Map<String, Object>> results = new ArrayList<>();
        Map<String, Object> row = new HashMap<>();
        row.put("id", 1);
        row.put("username", "testuser");
        results.add(row);
        
        // Préparer les infos d'endpoint
        ApiEndpointInfo endpointInfo = new ApiEndpointInfo();
        endpointInfo.setRoute("/api/users/1");
        endpointInfo.setMethod(HttpMethod.GET);
        endpointInfo.setHeaders(Map.of("Content-Type", "application/json"));
        
        String jsonPayload = "{\"user\":{\"id\":1,\"username\":\"testuser\"}}";
        ApiTemplateResult templateResult = new ApiTemplateResult(jsonPayload, endpointInfo);
        
        // Préparer les réponses API (erreur puis succès)
        ApiResponse errorResponse = ApiResponse.builder()
                .statusCode(503) // Service Unavailable - devrait déclencher un réessai
                .body("{\"error\":\"Service temporarily unavailable\"}")
                .build();
                
        ApiResponse successResponse = ApiResponse.builder()
                .statusCode(200)
                .body("{\"id\":1,\"status\":\"success\"}")
                .build();
        
        // Configurer les mocks
        when(sqlFileService.readSqlFile(sqlFileName)).thenReturn(sqlFile);
        when(sqlExecutionService.executeQuery(sqlFile.getContent())).thenReturn(results);
        when(tokenService.getToken()).thenReturn("Bearer token123");
        when(templateService.processTemplate(eq(sqlFile.getTemplateName()), eq(row))).thenReturn(templateResult);
        
        // La première tentative échoue avec 503, la deuxième réussit
        when(apiClientService.callApi(
                eq(endpointInfo.getRoute()),
                eq(endpointInfo.getMethod()),
                eq(jsonPayload),
                eq(endpointInfo.getHeaders()),
                eq(endpointInfo.getUrlParams())))
                .thenReturn(errorResponse)
                .thenReturn(successResponse);
        
        // Configurer le délai de réessai à 0 pour le test
        ReflectionTestUtils.setField(orchestrator, "retryDelayMs", 1L);
        ReflectionTestUtils.setField(orchestrator, "maxRetryAttempts", 3);
        
        // Act
        List<ApiResponse> responses = orchestrator.processSqlFile(sqlFileName);
        
        // Assert
        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(successResponse, responses.get(0));
        
        // Vérifier que le token n'est généré qu'une seule fois
        verify(tokenService, times(1)).getToken();
        
        // Vérifier que le template n'est traité qu'une seule fois
        verify(templateService, times(1)).processTemplate(eq(sqlFile.getTemplateName()), eq(row));
        
        // Vérifier que l'API est appelée exactement 2 fois (erreur puis succès)
        verify(apiClientService, times(2)).callApi(
                eq(endpointInfo.getRoute()),
                eq(endpointInfo.getMethod()),
                eq(jsonPayload),
                eq(endpointInfo.getHeaders()),
                eq(endpointInfo.getUrlParams()));
    }
}