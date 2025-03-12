package com.etljobs.sql2json2api.service.template;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;

import com.etljobs.sql2json2api.exception.TemplateProcessingException;
import com.etljobs.sql2json2api.model.ApiEndpointInfo;
import com.etljobs.sql2json2api.model.ApiTemplateResult;

@ExtendWith(MockitoExtension.class)
class TemplateProcessingServiceTest {

    @Mock
    private TemplateLoader templateLoader;
    
    @Mock
    private TemplateRenderer templateRenderer;
    
    @Mock
    private TemplateMetadataService metadataService;
    
    @Mock
    private PlaceholderProcessor placeholderProcessor;
    
    @InjectMocks
    private TemplateProcessingService templateProcessingService;
    
    private static final String TEMPLATE_NAME = "test.ftlh";
    private static final String TEMPLATE_CONTENT = "<#-- @api-route: /api/users/${result.id} @api-method: GET -->";
    private static final String JSON_RESULT = "{\"id\":123,\"name\":\"John\"}";
    
    @BeforeEach
    void setUp() {
        // Configuration des mocks communs
        when(templateLoader.loadTemplateContent(TEMPLATE_NAME)).thenReturn(TEMPLATE_CONTENT);
    }
    
    @Test
    void processTemplate_ShouldProcessTemplateSuccessfully() {
        // Arrange
        Map<String, Object> rowData = new HashMap<>();
        rowData.put("id", 123);
        rowData.put("name", "John");
        
        ApiEndpointInfo endpointInfo = new ApiEndpointInfo();
        endpointInfo.setRoute("/api/users/${result.id}");
        endpointInfo.setMethod(HttpMethod.GET);
        
        when(metadataService.extractMetadataFromTemplate(TEMPLATE_CONTENT)).thenReturn(endpointInfo);
        when(placeholderProcessor.processPlaceholders("/api/users/${result.id}", rowData))
            .thenReturn("/api/users/123");
        when(templateRenderer.renderTemplate(eq(TEMPLATE_NAME), anyMap())).thenReturn(JSON_RESULT);
        
        // Act
        ApiTemplateResult result = templateProcessingService.processTemplate(TEMPLATE_NAME, rowData);
        
        // Assert
        assertNotNull(result);
        assertEquals(JSON_RESULT, result.getJsonPayload());
        assertEquals("/api/users/123", result.getEndpointInfo().getRoute());
        assertEquals(HttpMethod.GET, result.getEndpointInfo().getMethod());
        
        // Verify interactions
        verify(templateLoader).loadTemplateContent(TEMPLATE_NAME);
        verify(metadataService).extractMetadataFromTemplate(TEMPLATE_CONTENT);
        verify(placeholderProcessor).processPlaceholders("/api/users/${result.id}", rowData);
        verify(templateRenderer).renderTemplate(eq(TEMPLATE_NAME), anyMap());
    }
    
    @Test
    void processTemplate_ShouldThrowException_WhenTemplateLoadingFails() {
        // Arrange
        Map<String, Object> rowData = new HashMap<>();
        
        when(templateLoader.loadTemplateContent(TEMPLATE_NAME))
            .thenThrow(new TemplateProcessingException("Template loading failed"));
        
        // Act & Assert
        assertThrows(TemplateProcessingException.class, () -> {
            templateProcessingService.processTemplate(TEMPLATE_NAME, rowData);
        });
    }
    
    @Test
    void processTemplate_ShouldThrowException_WhenMetadataExtractionFails() {
        // Arrange
        Map<String, Object> rowData = new HashMap<>();
        
        when(metadataService.extractMetadataFromTemplate(TEMPLATE_CONTENT))
            .thenThrow(new TemplateProcessingException("Metadata extraction failed"));
        
        // Act & Assert
        assertThrows(TemplateProcessingException.class, () -> {
            templateProcessingService.processTemplate(TEMPLATE_NAME, rowData);
        });
    }
    
    @Test
    void processTemplate_ShouldThrowException_WhenTemplateRenderingFails() {
        // Arrange
        Map<String, Object> rowData = new HashMap<>();
        rowData.put("id", 123);
        
        ApiEndpointInfo endpointInfo = new ApiEndpointInfo();
        endpointInfo.setRoute("/api/users/${result.id}");
        endpointInfo.setMethod(HttpMethod.GET);
        
        when(metadataService.extractMetadataFromTemplate(TEMPLATE_CONTENT)).thenReturn(endpointInfo);
        when(placeholderProcessor.processPlaceholders(anyString(), any())).thenReturn("/api/users/123");
        when(templateRenderer.renderTemplate(eq(TEMPLATE_NAME), anyMap()))
            .thenThrow(new TemplateProcessingException("Template rendering failed"));
        
        // Act & Assert
        assertThrows(TemplateProcessingException.class, () -> {
            templateProcessingService.processTemplate(TEMPLATE_NAME, rowData);
        });
    }
}