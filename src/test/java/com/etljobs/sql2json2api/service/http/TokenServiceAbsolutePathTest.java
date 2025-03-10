package com.etljobs.sql2json2api.service.http;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Captor;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import freemarker.template.Configuration;

@ExtendWith(MockitoExtension.class)
class TokenServiceAbsolutePathTest {

    @Mock
    private RestTemplate restTemplate;
    
    @Mock
    private Configuration freemarkerConfiguration;
    
    @TempDir
    Path tempDir;
    
    private TokenService tokenService;
    private ObjectMapper objectMapper = new ObjectMapper();
    
    @Captor
    private ArgumentCaptor<HttpEntity<String>> httpEntityCaptor;
    
    private static final String AUTH_TEMPLATE_CONTENT = 
            "{\n" +
            "  \"username\": \"${username}\",\n" +
            "  \"password\": \"${password}\",\n" +
            "  \"context\": \"api\"\n" +
            "}";
    
    @BeforeEach
    void setUp() throws IOException {
        // Créer le template d'authentification dans le répertoire temporaire
        Path authTemplatePath = tempDir.resolve("auth-payload.ftlh");
        Files.writeString(authTemplatePath, AUTH_TEMPLATE_CONTENT);
        
        // Créer le service avec les mocks
        tokenService = new TokenService(restTemplate, objectMapper, freemarkerConfiguration);
        
        // Configurer les valeurs par réflexion
        ReflectionTestUtils.setField(tokenService, "authUrl", "https://api.test.com/auth");
        ReflectionTestUtils.setField(tokenService, "username", "testuser");
        ReflectionTestUtils.setField(tokenService, "password", "testpass");
        ReflectionTestUtils.setField(tokenService, "tokenTtlSeconds", 3600L);
        ReflectionTestUtils.setField(tokenService, "payloadTemplatePath", authTemplatePath.toString());
        
        // Préparer la réponse de l'API
        String mockResponse = "{\"token\": \"abc123\", \"expires_in\": 3600}";
        ResponseEntity<String> mockResponseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);
        
        when(restTemplate.exchange(
                anyString(), 
                eq(HttpMethod.POST), 
                any(HttpEntity.class), 
                eq(String.class)))
            .thenReturn(mockResponseEntity);
    }
    
    @Test
    void getToken_ShouldGenerateTokenUsingTemplateFromAbsolutePath() throws JsonProcessingException {
        // Act
        String token = tokenService.getToken();
        
        // Assert
        assertNotNull(token);
        assertTrue(token.startsWith("Bearer "));
        assertEquals("Bearer abc123", token);
        
        // Vérifier que l'appel REST a été fait avec le bon payload
        verify(restTemplate).exchange(
                eq("https://api.test.com/auth"), 
                eq(HttpMethod.POST), 
                httpEntityCaptor.capture(), 
                eq(String.class));
        
        // Extraire et vérifier le payload
        String payload = httpEntityCaptor.getValue().getBody();
        assertNotNull(payload);
        
        // Vérifier que le payload contient les bonnes valeurs
        Map<String, Object> payloadMap = objectMapper.readValue(payload, Map.class);
        assertEquals("testuser", payloadMap.get("username"));
        assertEquals("testpass", payloadMap.get("password"));
        assertEquals("api", payloadMap.get("context"));
    }
}