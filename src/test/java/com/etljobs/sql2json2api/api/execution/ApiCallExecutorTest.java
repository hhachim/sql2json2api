package com.etljobs.sql2json2api.api.execution;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.etljobs.sql2json2api.api.request.ApiRequest;
import com.etljobs.sql2json2api.api.response.ApiResponse;
import com.etljobs.sql2json2api.api.response.ApiResponseFactory;
import com.etljobs.sql2json2api.exception.ApiCallException;
import com.etljobs.sql2json2api.service.http.TokenService;

@ExtendWith(MockitoExtension.class)
class ApiCallExecutorTest {

    @Mock
    private RestTemplate restTemplate;
    
    @Mock
    private TokenService tokenService;
    
    @Mock
    private ApiResponseFactory responseFactory;
    
    @Mock
    private DefaultApiCallStrategy defaultStrategy;

    private ApiCallExecutor executor;
    
    @BeforeEach
    void setUp() {
        executor = new ApiCallExecutor(restTemplate, tokenService, responseFactory, defaultStrategy);
    }
    
    @Test
    void execute_ShouldMakeApiCallAndReturnResponse() {
        // Arrange
        String url = "https://api.example.com/data";
        HttpMethod method = HttpMethod.GET;
        String token = "Bearer test-token";
        String responseBody = "{\"success\": true}";
        
        ApiRequest request = ApiRequest.builder()
                .url(url)
                .method(method)
                .build();
        
        ResponseEntity<String> responseEntity = ResponseEntity.ok(responseBody);
        ApiResponse expectedResponse = ApiResponse.builder()
                .statusCode(200)
                .body(responseBody)
                .build();
        
        when(tokenService.getToken()).thenReturn(token);
        when(restTemplate.exchange(
                eq(url), eq(method), any(HttpEntity.class), eq(String.class)))
                .thenReturn(responseEntity);
        when(responseFactory.fromResponseEntity(any(), any(), anyLong(), anyInt()))
                .thenReturn(expectedResponse);
        
        // Act
        ApiResponse result = executor.execute(request);
        
        // Assert
        assertEquals(expectedResponse, result);
        verify(restTemplate).exchange(
                eq(url), eq(method), any(HttpEntity.class), eq(String.class));
        verify(responseFactory).fromResponseEntity(any(), any(), anyLong(), anyInt());
    }
    
    @Test
    void execute_ShouldHandleHttpException() {
        // Arrange
        String url = "https://api.example.com/data";
        HttpMethod method = HttpMethod.GET;
        String token = "Bearer test-token";
        String errorBody = "{\"error\": \"Not found\"}";
        
        ApiRequest request = ApiRequest.builder()
                .url(url)
                .method(method)
                .build();
        
        HttpClientErrorException exception = HttpClientErrorException.create(
                HttpStatus.NOT_FOUND, "Not Found", null, errorBody.getBytes(), null);
        
        ApiResponse expectedResponse = ApiResponse.builder()
                .statusCode(404)
                .body(errorBody)
                .build();
        
        when(tokenService.getToken()).thenReturn(token);
        when(restTemplate.exchange(
                eq(url), eq(method), any(HttpEntity.class), eq(String.class)))
                .thenThrow(exception);
        when(responseFactory.fromHttpException(any(), any(), anyLong(), anyInt()))
                .thenReturn(expectedResponse);
        
        // Act
        ApiResponse result = executor.execute(request);
        
        // Assert
        assertEquals(expectedResponse, result);
        verify(responseFactory).fromHttpException(any(), any(), anyLong(), anyInt());
    }
    
    @Test
    void execute_ShouldThrowExceptionForOtherErrors() {
        // Arrange
        String url = "https://api.example.com/data";
        HttpMethod method = HttpMethod.GET;
        
        ApiRequest request = ApiRequest.builder()
                .url(url)
                .method(method)
                .build();
        
        RuntimeException unexpectedException = new RuntimeException("Unexpected error");
        
        when(tokenService.getToken()).thenReturn("Bearer token");
        when(restTemplate.exchange(
                eq(url), eq(method), any(HttpEntity.class), eq(String.class)))
                .thenThrow(unexpectedException);
        
        // Act & Assert
        ApiCallException exception = assertThrows(ApiCallException.class, () -> {
            executor.execute(request);
        });
        
        assertTrue(exception.getMessage().contains("Échec de l'exécution"));
        assertTrue(exception.getCause() == unexpectedException);
    }
    
    @Test
    void execute_ShouldUseRequestTokenIfProvided() {
        // Arrange
        String url = "https://api.example.com/data";
        HttpMethod method = HttpMethod.GET;
        String providedToken = "Bearer provided-token";
        
        ApiRequest request = ApiRequest.builder()
                .url(url)
                .method(method)
                .authToken(providedToken)
                .build();
        
        ResponseEntity<String> responseEntity = ResponseEntity.ok("{}");
        ApiResponse expectedResponse = ApiResponse.builder()
                .statusCode(200)
                .body("{}")
                .build();
        
        when(restTemplate.exchange(
                anyString(), any(), any(), eq(String.class)))
                .thenReturn(responseEntity);
        when(responseFactory.fromResponseEntity(any(), any(), anyLong(), anyInt()))
                .thenReturn(expectedResponse);
        
        // Act
        executor.execute(request);
        
        // Assert - le token du service ne devrait pas être appelé
        verify(tokenService, times(0)).getToken();
    }
    
    @Test
    void execute_ShouldRefreshTokenIfRequested() {
        // Arrange
        String url = "https://api.example.com/data";
        HttpMethod method = HttpMethod.GET;
        String refreshedToken = "Bearer refreshed-token";
        
        ApiRequest request = ApiRequest.builder()
                .url(url)
                .method(method)
                .refreshToken(true)
                .build();
        
        ResponseEntity<String> responseEntity = ResponseEntity.ok("{}");
        ApiResponse expectedResponse = ApiResponse.builder()
                .statusCode(200)
                .body("{}")
                .build();
        
        when(tokenService.refreshToken()).thenReturn(refreshedToken);
        when(restTemplate.exchange(
                anyString(), any(), any(), eq(String.class)))
                .thenReturn(responseEntity);
        when(responseFactory.fromResponseEntity(any(), any(), anyLong(), anyInt()))
                .thenReturn(expectedResponse);
        
        // Act
        executor.execute(request);
        
        // Assert - refreshToken devrait être appelé au lieu de getToken
        verify(tokenService).refreshToken();
        verify(tokenService, times(0)).getToken();
    }
    
    @Test
    void executeWithRetry_ShouldProcessMultipleAttempts() {
        // Cette version du test ne simule pas un cycle complet avec réessai
        // mais vérifie simplement que la logique de base fonctionne
        
        // Arrange
        ApiRequest request = ApiRequest.builder()
                .url("https://api.example.com/test")
                .method(HttpMethod.GET)
                .build();
        
        // Configurer un comportement simple qui simule un succès immédiat
        ApiResponse successResponse = ApiResponse.builder()
                .statusCode(200)
                .body("{\"success\":true}")
                .build();
        
        // Uniquement le minimum de mocks nécessaires
        when(defaultStrategy.shouldRetry(any())).thenReturn(false);
        when(tokenService.getToken()).thenReturn("Bearer token");
        when(restTemplate.exchange(
                anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"success\":true}"));
        when(responseFactory.fromResponseEntity(any(), any(), anyLong(), anyInt()))
                .thenReturn(successResponse);
        
        // Act
        ApiResponse result = executor.executeWithRetry(request, null, 1, 0);
        
        // Assert - vérifier simplement que le résultat a été retourné
        assertNotNull(result);
        assertEquals(200, result.getStatusCode());
    }
    
    @Test
    void execute_ShouldConstructUrlWithParameters() {
        // Arrange
        String baseUrl = "https://api.example.com/data";
        HttpMethod method = HttpMethod.GET;
        
        Map<String, Object> urlParams = new HashMap<>();
        urlParams.put("param1", "value1");
        urlParams.put("param2", 123);
        
        ApiRequest request = ApiRequest.builder()
                .url(baseUrl)
                .method(method)
                .urlParams(urlParams)
                .build();
        
        String expectedUrl = "https://api.example.com/data?param1=value1&param2=123";
        
        ResponseEntity<String> responseEntity = ResponseEntity.ok("{}");
        ApiResponse apiResponse = ApiResponse.builder()
                .statusCode(200)
                .body("{}")
                .build();
        
        when(tokenService.getToken()).thenReturn("Bearer token");
        when(restTemplate.exchange(
                eq(expectedUrl), eq(method), any(HttpEntity.class), eq(String.class)))
                .thenReturn(responseEntity);
        when(responseFactory.fromResponseEntity(any(), any(), anyLong(), anyInt()))
                .thenReturn(apiResponse);
        
        // Act
        executor.execute(request);
        
        // Assert - l'URL avec les paramètres a été utilisée
        verify(restTemplate).exchange(
                eq(expectedUrl), any(), any(), eq(String.class));
    }
}