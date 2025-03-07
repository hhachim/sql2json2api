package com.etljobs.sql2json2api.service.template;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import com.etljobs.sql2json2api.exception.TemplateProcessingException;
import com.etljobs.sql2json2api.model.ApiEndpointInfo;
import com.fasterxml.jackson.databind.ObjectMapper;

class TemplateMetadataServiceTest {

    private final TemplateMetadataService metadataService = new TemplateMetadataService(new ObjectMapper());
    
    @Test
    void testExtractMetadataFromTemplate_Complete() {
        // Arrange
        String templateContent = "<#--\n" +
                "  @api-route: /api/users/${result.id}\n" +
                "  @api-method: GET\n" +
                "  @api-headers: {\"Content-Type\": \"application/json\", \"Accept\": \"application/json\"}\n" +
                "  @api-params: {\"includeDetails\": true}\n" +
                "-->";
        
        // Act
        ApiEndpointInfo endpointInfo = metadataService.extractMetadataFromTemplate(templateContent);
        
        // Assert
        assertNotNull(endpointInfo);
        assertEquals("/api/users/${result.id}", endpointInfo.getRoute());
        assertEquals(HttpMethod.GET, endpointInfo.getMethod());
        
        // Check headers
        Map<String, String> headers = endpointInfo.getHeaders();
        assertNotNull(headers);
        assertEquals(2, headers.size());
        assertEquals("application/json", headers.get("Content-Type"));
        assertEquals("application/json", headers.get("Accept"));
        
        // Check params
        Map<String, Object> params = endpointInfo.getUrlParams();
        assertNotNull(params);
        assertEquals(1, params.size());
        assertTrue(params.containsKey("includeDetails"));
        assertEquals(true, params.get("includeDetails"));
    }
    
    @Test
    void testExtractMetadataFromTemplate_MissingRequiredFields() {
        // Arrange
        String templateContentMissingMethod = "<#--\n" +
                "  @api-route: /api/users/${result.id}\n" +
                "-->\n" +
                "{ \"id\": ${result.id} }";
        
        // Act & Assert
        assertThrows(TemplateProcessingException.class, () -> {
            metadataService.extractMetadataFromTemplate(templateContentMissingMethod);
        });
        
        String templateContentMissingRoute = "<#--\n" +
                "  @api-method: GET\n" +
                "-->\n" +
                "{ \"id\": ${result.id} }";
        
        // Act & Assert
        assertThrows(TemplateProcessingException.class, () -> {
            metadataService.extractMetadataFromTemplate(templateContentMissingRoute);
        });
    }
    
    @Test
    void testExtractMetadataFromTemplate_InvalidJson() {
        // Arrange
        String templateContentInvalidJson = "<#--\n" +
                "  @api-route: /api/users/${result.id}\n" +
                "  @api-method: GET\n" +
                "  @api-headers: {\"Content-Type\": \"application/json\", INVALID_JSON}\n" +
                "-->\n" +
                "{ \"id\": ${result.id} }";
        
        // Act & Assert
        assertThrows(TemplateProcessingException.class, () -> {
            metadataService.extractMetadataFromTemplate(templateContentInvalidJson);
        });
    }
}