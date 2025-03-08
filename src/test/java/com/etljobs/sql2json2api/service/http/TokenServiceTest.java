package com.etljobs.sql2json2api.service.http;

import java.io.StringWriter;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import freemarker.template.Configuration;
import freemarker.template.Template;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private RestTemplate restTemplate;
    
    @Mock
    private Configuration freemarkerConfiguration;
    
    @Mock
    private Template mockTemplate;
    
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();
    
    @InjectMocks
    private TokenService tokenService;
    
    @Captor
    private ArgumentCaptor<HttpEntity<String>> httpEntityCaptor;
    
    @BeforeEach
    void setUp() throws Exception {
        // Set required fields via reflection
        ReflectionTestUtils.setField(tokenService, "authUrl", "https://api.test.com/auth");
        ReflectionTestUtils.setField(tokenService, "username", "testuser");
        ReflectionTestUtils.setField(tokenService, "password", "testpass");
        ReflectionTestUtils.setField(tokenService, "tokenTtlSeconds", 3600L);
        ReflectionTestUtils.setField(tokenService, "payloadTemplatePath", "auth/auth-payload.ftlh");
        
        // Mock the template processing
        when(freemarkerConfiguration.getTemplate(anyString())).thenReturn(mockTemplate);
        
        // Mock the template processing to return a JSON with our expected values
        doAnswer(invocation -> {
            StringWriter writer = invocation.getArgument(1);
            writer.write("{\"username\":\"testuser\",\"password\":\"testpass\",\"context\":\"api\"}");
            return null;
        }).when(mockTemplate).process(any(Map.class), any(StringWriter.class));
    }
    
    @Test
    void getToken_ShouldGenerateNewToken_WhenNoneExists() {
        // Arrange - mock the API response
        String mockResponse = "{\"token\": \"abc123\", \"expires_in\": 3600}";
        ResponseEntity<String> mockResponseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);
        
        when(restTemplate.exchange(
                anyString(), 
                eq(HttpMethod.POST), 
                any(HttpEntity.class), 
                eq(String.class)))
            .thenReturn(mockResponseEntity);
        
        // Act
        String token = tokenService.getToken();
        
        // Assert
        assertNotNull(token);
        assertTrue(token.startsWith("Bearer "));
        assertEquals("Bearer abc123", token);
        
        // Verify the template was loaded
        try {
            verify(freemarkerConfiguration).getTemplate("auth/auth-payload.ftlh");
        } catch (Exception e) {
            fail("Template loading failed: " + e.getMessage());
        }
        
        // Verify the call was made with the expected payload
        verify(restTemplate).exchange(
                eq("https://api.test.com/auth"), 
                eq(HttpMethod.POST), 
                httpEntityCaptor.capture(), 
                eq(String.class));
        
        // Check that the payload contains the expected JSON
        String payload = httpEntityCaptor.getValue().getBody();
        assertNotNull(payload);
        assertTrue(payload.contains("\"username\":\"testuser\""));
        assertTrue(payload.contains("\"password\":\"testpass\""));
        assertTrue(payload.contains("\"context\":\"api\""));
    }
    
    // Les autres tests existants peuvent rester similaires, avec les vérifications appropriées
    // des appels à Freemarker...
    
    @Test
    void generatePayload_ShouldLoadTemplateAndProcessIt() throws Exception {
        // Act
        String result = tokenService.generatePayload();
        
        // Assert
        verify(freemarkerConfiguration).getTemplate("auth/auth-payload.ftlh");
        assertNotNull(result);
        assertEquals("{\"username\":\"testuser\",\"password\":\"testpass\",\"context\":\"api\"}", result);
        
        // Verify the data model passed to the template
        ArgumentCaptor<Map<String, Object>> dataModelCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mockTemplate).process(dataModelCaptor.capture(), any(StringWriter.class));
        
        Map<String, Object> dataModel = dataModelCaptor.getValue();
        assertEquals("testuser", dataModel.get("username"));
        assertEquals("testpass", dataModel.get("password"));
    }
}