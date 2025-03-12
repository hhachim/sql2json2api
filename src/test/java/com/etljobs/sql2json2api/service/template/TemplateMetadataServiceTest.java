package com.etljobs.sql2json2api.service.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;

import com.etljobs.sql2json2api.exception.TemplateProcessingException;
import com.etljobs.sql2json2api.model.ApiEndpointInfo;

@SpringBootTest
@ActiveProfiles("test")
class TemplateMetadataServiceTest {

    @Autowired
    private TemplateMetadataService metadataService;
    
    @Test
    void extractMetadataFromTemplate_ShouldExtractCorrectly_WithSingleLineMetadata() {
        // Arrange
        String templateContent = "<#-- @api-route: /api/users @api-method: GET -->";
        
        // Act
        ApiEndpointInfo result = metadataService.extractMetadataFromTemplate(templateContent);
        
        // Assert
        assertNotNull(result);
        assertEquals("/api/users", result.getRoute());
        assertEquals(HttpMethod.GET, result.getMethod());
    }
    
    @Test
    void extractMetadataFromTemplate_ShouldExtractCorrectly_WithMultilineMetadata() {
        // Arrange
        String templateContent = "<#--\n" +
                "  @api-route: /api/users/${result.id}\n" +
                "  @api-method: POST\n" +
                "  @api-headers: {\"Content-Type\": \"application/json\"}\n" +
                "-->\n" +
                "Template content here";
        
        // Act
        ApiEndpointInfo result = metadataService.extractMetadataFromTemplate(templateContent);
        
        // Assert
        assertNotNull(result);
        assertEquals("/api/users/${result.id}", result.getRoute());
        assertEquals(HttpMethod.POST, result.getMethod());
        assertNotNull(result.getHeaders());
        assertEquals("application/json", result.getHeaders().get("Content-Type"));
    }
    
    @Test
    void extractMetadataFromTemplate_ShouldThrowException_WhenRequiredFieldsMissing() {
        // Arrange
        String templateContentWithoutRoute = "<#-- @api-method: GET -->";
        String templateContentWithoutMethod = "<#-- @api-route: /api/users -->";
        
        // Act & Assert
        assertThrows(TemplateProcessingException.class, () -> {
            metadataService.extractMetadataFromTemplate(templateContentWithoutRoute);
        });
        
        assertThrows(TemplateProcessingException.class, () -> {
            metadataService.extractMetadataFromTemplate(templateContentWithoutMethod);
        });
    }
    
    @Test
    void extractMetadataFromTemplate_ShouldParseJsonValues_Correctly() {
        // Arrange
        String templateContent = "<#--\n" +
                "  @api-route: /api/complex\n" +
                "  @api-method: POST\n" +
                "  @api-headers: {\"Content-Type\": \"application/json\", \"Authorization\": \"Bearer token\"}\n" +
                "  @api-params: {\"filter\": true, \"count\": 25, \"nested\": {\"prop\": \"value\"}}\n" +
                "-->";
        
        // Act
        ApiEndpointInfo result = metadataService.extractMetadataFromTemplate(templateContent);
        
        // Assert
        assertNotNull(result);
        assertNotNull(result.getHeaders());
        assertEquals(2, result.getHeaders().size());
        assertEquals("application/json", result.getHeaders().get("Content-Type"));
        assertEquals("Bearer token", result.getHeaders().get("Authorization"));
        
        assertNotNull(result.getUrlParams());
        assertEquals(3, result.getUrlParams().size());
        assertEquals(true, result.getUrlParams().get("filter"));
        assertEquals(25, result.getUrlParams().get("count"));
        assertNotNull(result.getUrlParams().get("nested"));
    }
}