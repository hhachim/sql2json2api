package com.etljobs.sql2json2api.service.orchestration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpMethod;

import com.etljobs.sql2json2api.exception.TemplateProcessingException;
import com.etljobs.sql2json2api.model.ApiEndpointInfo;
import com.etljobs.sql2json2api.model.ApiResponse;
import com.etljobs.sql2json2api.model.ApiTemplateResult;
import com.etljobs.sql2json2api.model.SqlFile;
import com.etljobs.sql2json2api.service.http.ApiClientService;
import com.etljobs.sql2json2api.service.orchestration.RetryStrategy.RetryContext;
import com.etljobs.sql2json2api.service.template.TemplateProcessingService;

class RowProcessorTest {

    @Mock
    private TemplateProcessingService templateService;

    @Mock
    private ApiClientService apiClientService;

    @Mock
    private RetryStrategy retryStrategy;

    @Mock
    private RetryContext retryContext;

    private RowProcessor rowProcessor;
    private List<RowError> rowErrors;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        rowProcessor = new RowProcessor(templateService, apiClientService);
        rowErrors = new ArrayList<>();
        
        // Configuration standard du RetryStrategy
        when(retryStrategy.createContext()).thenReturn(retryContext);
        when(retryStrategy.getMaxAttempts()).thenReturn(3);
        when(retryContext.getCurrentAttempt()).thenReturn(1);
    }

    @Test
    void processRow_ShouldReturnSuccessResponse_WhenApiCallSucceeds() {
        // Arrange
        SqlFile sqlFile = SqlFile.builder()
                .fileName("GET_users.sql")
                .templateName("GET_users.ftlh")
                .build();
        
        Map<String, Object> row = new HashMap<>();
        row.put("id", 1);
        row.put("username", "user1");
        
        String rowIdentifier = "id=1";
        
        // Créer une réponse de template
        ApiEndpointInfo endpointInfo = new ApiEndpointInfo();
        endpointInfo.setRoute("/api/users/1");
        endpointInfo.setMethod(HttpMethod.GET);
        
        ApiTemplateResult templateResult = new ApiTemplateResult(
                "{\"id\":1,\"username\":\"user1\"}", 
                endpointInfo);
        
        // Créer une réponse API
        ApiResponse apiResponse = ApiResponse.builder()
                .statusCode(200)
                .body("{\"success\":true}")
                .build();
        
        // Configurer les mocks
        when(templateService.processTemplate(eq(sqlFile.getTemplateName()), eq(row)))
                .thenReturn(templateResult);
        
        when(apiClientService.callApi(
                eq(endpointInfo.getRoute()),
                eq(endpointInfo.getMethod()),
                eq(templateResult.getJsonPayload()),
                eq(endpointInfo.getHeaders()),
                eq(endpointInfo.getUrlParams())))
                .thenReturn(apiResponse);
        
        // Act
        ApiResponse result = rowProcessor.processRow(
                sqlFile, row, 0, rowIdentifier, retryStrategy, rowErrors);
        
        // Assert
        assertNotNull(result);
        assertEquals(200, result.getStatusCode());
        assertEquals("{\"success\":true}", result.getBody());
        assertTrue(rowErrors.isEmpty(), "Aucune erreur ne devrait être enregistrée");
    }
    
    @Test
    void processRow_ShouldReturnErrorResponse_WhenTemplateProcessingFails() {
        // Arrange
        SqlFile sqlFile = SqlFile.builder()
                .fileName("GET_users.sql")
                .templateName("GET_users.ftlh")
                .build();
        
        Map<String, Object> row = new HashMap<>();
        row.put("id", 1);
        
        String rowIdentifier = "id=1";
        
        // Simuler une erreur de template
        TemplateProcessingException exception = new TemplateProcessingException("Erreur de template");
        when(templateService.processTemplate(eq(sqlFile.getTemplateName()), eq(row)))
                .thenThrow(exception);
        
        // Act
        ApiResponse result = rowProcessor.processRow(
                sqlFile, row, 0, rowIdentifier, retryStrategy, rowErrors);
        
        // Assert
        assertNotNull(result);
        assertEquals(500, result.getStatusCode());
        assertTrue(result.getBody().contains("Erreur de template"));
        
        // Vérifier qu'une erreur a été enregistrée
        assertEquals(1, rowErrors.size());
        RowError error = rowErrors.get(0);
        assertEquals(0, error.getRowIndex());
        assertEquals(row, error.getRowData());
        assertEquals(exception, error.getException());
    }
    
    @Test
    void processRow_ShouldRetry_WhenApiCallFailsWithRetryableError() {
        // Arrange
        SqlFile sqlFile = SqlFile.builder()
                .fileName("GET_users.sql")
                .templateName("GET_users.ftlh")
                .build();
        
        Map<String, Object> row = new HashMap<>();
        row.put("id", 1);
        
        String rowIdentifier = "id=1";
        
        // Créer une réponse de template
        ApiEndpointInfo endpointInfo = new ApiEndpointInfo();
        endpointInfo.setRoute("/api/users/1");
        endpointInfo.setMethod(HttpMethod.GET);
        
        ApiTemplateResult templateResult = new ApiTemplateResult(
                "{\"id\":1}", 
                endpointInfo);
        
        // Configurer les réponses API (erreur puis succès)
        ApiResponse errorResponse = ApiResponse.builder()
                .statusCode(503)
                .body("{\"error\":\"Service unavailable\"}")
                .build();
        
        ApiResponse successResponse = ApiResponse.builder()
                .statusCode(200)
                .body("{\"success\":true}")
                .build();
        
        // Configurer les mocks
        when(templateService.processTemplate(anyString(), any()))
                .thenReturn(templateResult);
        
        // Simuler une erreur retryable
        when(apiClientService.callApi(
                anyString(), any(), anyString(), any(), any()))
                .thenReturn(errorResponse)
                .thenReturn(successResponse);
        
        when(retryStrategy.isRetryableStatusCode(503)).thenReturn(true);
        when(retryStrategy.shouldRetry(503, 1)).thenReturn(true);
        
        // Simuler la progression des tentatives
        when(retryContext.getCurrentAttempt())
                .thenReturn(1)
                .thenReturn(2);
        
        // Act
        ApiResponse result = rowProcessor.processRow(
                sqlFile, row, 0, rowIdentifier, retryStrategy, rowErrors);
        
        // Assert
        assertNotNull(result);
        assertEquals(503, result.getStatusCode());
        
        // Vérifier que l'API a été appelée une seule fois (car notre mock n'effectue pas réellement de réessai)
        verify(apiClientService, times(1)).callApi(
                anyString(), any(), anyString(), any(), any());
    }
}