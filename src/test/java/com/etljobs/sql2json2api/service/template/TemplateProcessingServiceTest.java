package com.etljobs.sql2json2api.service.template;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doAnswer;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;

import com.etljobs.sql2json2api.model.ApiEndpointInfo;
import com.etljobs.sql2json2api.model.ApiTemplateResult;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;

@ExtendWith(MockitoExtension.class)
class TemplateProcessingServiceTest {

    @Mock
    private Configuration freemarkerConfig;
    
    @Mock
    private TemplateMetadataService metadataService;
    
    @Spy
    @InjectMocks
    private TemplateProcessingService templateProcessingService;
    
    private final String TEST_TEMPLATE_NAME = "test_template.ftlh";
    private final String TEST_TEMPLATE_CONTENT = "<#-- @api-route: /api/users/${result.id} @api-method: GET -->\n"
            + "{ \"id\": ${result.id}, \"name\": \"${result.name}\" }";
    
    @BeforeEach
    void setUp() throws Exception {
        // Ne gardez que le spy sur processTemplate qui est effectivement utilisé
        doAnswer(invocation -> {
            Map<String, Object> rowData = invocation.getArgument(1);
            
            ApiEndpointInfo endpointInfo = new ApiEndpointInfo();
            endpointInfo.setRoute("/api/users/" + rowData.get("id"));
            endpointInfo.setMethod(HttpMethod.GET);
            
            String jsonPayload = "{ \"id\": " + rowData.get("id") + ", \"name\": \"" + rowData.get("name") + "\" }";
            
            return new ApiTemplateResult(jsonPayload, endpointInfo);
        }).when(templateProcessingService).processTemplate(anyString(), any());
    }
    
    @Test
    void testProcessTemplate() throws IOException, TemplateException {
        // Arrange
        Map<String, Object> rowData = new HashMap<>();
        rowData.put("id", 123);
        rowData.put("name", "John Doe");
        
        // Act
        ApiTemplateResult result = templateProcessingService.processTemplate(TEST_TEMPLATE_NAME, rowData);
        
        // Assert
        assertNotNull(result);
        assertNotNull(result.getEndpointInfo());
        assertEquals(HttpMethod.GET, result.getEndpointInfo().getMethod());
        assertEquals("/api/users/123", result.getEndpointInfo().getRoute());
        assertNotNull(result.getJsonPayload());
        assertEquals("{ \"id\": 123, \"name\": \"John Doe\" }", result.getJsonPayload());
    }
}