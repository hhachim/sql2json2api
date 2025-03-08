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

        when(sqlFileService.readSqlFile(sqlFileName)).thenReturn(sqlFile);
        when(sqlExecutionService.executeQuery(sqlFile.getContent())).thenReturn(results);

        // Act
        var response = orchestrator.processSqlFile(sqlFileName);

        // Assert
        assertNotNull(response);
        // Pour l'instant, on vérifie juste que la méthode s'exécute sans erreur
        // et retourne une liste (vide dans notre implémentation initiale)
        assertEquals(0, response.size());
    }
}