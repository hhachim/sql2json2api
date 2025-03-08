package com.etljobs.sql2json2api.service.orchestration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpMethod;

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
    }
}