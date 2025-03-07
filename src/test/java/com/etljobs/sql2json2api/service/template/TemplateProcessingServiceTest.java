package com.etljobs.sql2json2api.service.template;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;

import com.etljobs.sql2json2api.model.ApiEndpointInfo;
import com.etljobs.sql2json2api.model.ApiTemplateResult;

import freemarker.template.Configuration;
import freemarker.template.Template;

@ExtendWith(MockitoExtension.class)
class TemplateProcessingServiceTest {

    @Mock
    private Configuration freemarkerConfig;
    
    @Mock
    private TemplateMetadataService metadataService;
    
    @InjectMocks
    private TemplateProcessingService templateProcessingService;
    
    @BeforeEach
    void setUp() throws Exception {
        // Ne pas utiliser le chemin de ressource, utiliser un mock
        // Cela évite l'erreur "class path resource [templates/json/] cannot be opened because it does not exist"
        
        // Mock template content
        String mockTemplateContent = "<#-- @api-route: /api/users/${result.id} @api-method: GET -->\n"
                + "{ \"id\": ${result.id}, \"name\": \"${result.name}\" }";
        
        // Créer un mock pour le template
        Template mockTemplate = Mockito.mock(Template.class);
        
        // Créer un spy pour intercepter getTemplateContent
        templateProcessingService = Mockito.spy(templateProcessingService);
        when(templateProcessingService.getTemplateContent(anyString())).thenReturn(mockTemplateContent);
        
        // Configurer le comportement du mock de freemarkerConfig
        when(freemarkerConfig.getTemplate(anyString())).thenReturn(mockTemplate);
        
        // Configurer le mock de metadataService
        ApiEndpointInfo mockEndpointInfo = new ApiEndpointInfo();
        mockEndpointInfo.setRoute("/api/users/${result.id}");
        mockEndpointInfo.setMethod(HttpMethod.GET);
        when(metadataService.extractMetadataFromTemplate(anyString())).thenReturn(mockEndpointInfo);
        
        // Simuler le traitement du template
        Mockito.doAnswer(invocation -> {
            StringWriter writer = (StringWriter) invocation.getArguments()[1];
            Map<String, Object> dataModel = (Map<String, Object>) invocation.getArguments()[0];
            Map<String, Object> result = (Map<String, Object>) dataModel.get("result");
            writer.write("{ \"id\": " + result.get("id") + ", \"name\": \"" + result.get("name") + "\" }");
            return null;
        }).when(mockTemplate).process(Mockito.any(), Mockito.any(StringWriter.class));
    }
    
    @Test
    void testProcessTemplate() throws Exception {
        // Arrange
        Map<String, Object> rowData = new HashMap<>();
        rowData.put("id", 123);
        rowData.put("name", "John Doe");
        
        // Act
        ApiTemplateResult result = templateProcessingService.processTemplate("test.ftlh", rowData);
        
        // Assert
        assertNotNull(result);
        assertNotNull(result.getEndpointInfo());
        assertEquals(HttpMethod.GET, result.getEndpointInfo().getMethod());
        assertEquals("/api/users/123", result.getEndpointInfo().getRoute());
        assertNotNull(result.getJsonPayload());
    }
}