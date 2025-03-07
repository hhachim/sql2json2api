package com.etljobs.sql2json2api.service.http;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.etljobs.sql2json2api.model.ApiResponse;

@ExtendWith(MockitoExtension.class)
class ApiClientServiceTest {

    @Mock
    private RestTemplate restTemplate;
    
    @Mock
    private TokenService tokenService;
    
    @InjectMocks
    private ApiClientService apiClientService;
    
    @Captor
    private ArgumentCaptor<HttpEntity<String>> entityCaptor;
    
    @Test
    void callApi_ShouldMakeRequestWithCorrectParameters() {
        // Arrange
        String route = "https://api.example.com/data";
        HttpMethod method = HttpMethod.GET;
        String mockToken = "Bearer test-token";
        String mockResponse = "{\"id\": 123, \"name\": \"Test Data\"}";
        
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        
        Map<String, Object> urlParams = new HashMap<>();
        urlParams.put("limit", 10);
        
        // Mock token service
        when(tokenService.getToken()).thenReturn(mockToken);
        
        // Mock REST template response
        ResponseEntity<String> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);
        when(restTemplate.exchange(
                anyString(), 
                eq(HttpMethod.GET), 
                any(HttpEntity.class), 
                eq(String.class)))
            .thenReturn(responseEntity);
        
        // Act
        ApiResponse response = apiClientService.callApi(route, method, null, headers, urlParams);
        
        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
        assertTrue(response.isSuccess());
        
        // Verify the call was made with correct URL parameters
        verify(restTemplate).exchange(
                eq("https://api.example.com/data?limit=10"), 
                eq(HttpMethod.GET), 
                entityCaptor.capture(), 
                eq(String.class));
        
        // Verify headers
        HttpEntity<String> capturedEntity = entityCaptor.getValue();
        HttpHeaders capturedHeaders = capturedEntity.getHeaders();
        assertEquals(mockToken, capturedHeaders.getFirst("Authorization"));
        assertEquals("application/json", capturedHeaders.getFirst("Content-Type"));
    }
    
    @Test
    void callApi_ShouldHandleHttpErrorResponse() {
        // Arrange
        String route = "https://api.example.com/data";
        HttpMethod method = HttpMethod.GET;
        String mockToken = "Bearer test-token";
        String errorResponse = "{\"error\": \"Not Found\"}";
        
        // Mock token service
        when(tokenService.getToken()).thenReturn(mockToken);
        
        // Mock REST template to throw HttpStatusCodeException
        when(restTemplate.exchange(
                anyString(), 
                eq(HttpMethod.GET), 
                any(HttpEntity.class), 
                eq(String.class)))
            .thenThrow(HttpClientErrorException.create(
                    HttpStatus.NOT_FOUND, 
                    "Not Found", 
                    HttpHeaders.EMPTY, 
                    errorResponse.getBytes(), 
                    null));
        
        // Act
        ApiResponse response = apiClientService.callApi(route, method, null);
        
        // Assert
        assertNotNull(response);
        assertEquals(404, response.getStatusCode());
        assertEquals(errorResponse, response.getBody());
        assertEquals(false, response.isSuccess());
    }
    
    @Test
    void retryWithNewToken_ShouldRefreshTokenAndRetry() {
        // Arrange
        String route = "https://api.example.com/data";
        HttpMethod method = HttpMethod.POST;
        String payload = "{\"name\": \"Test Data\"}";
        String initialToken = "Bearer old-token";
        String newToken = "Bearer new-token";
        String mockResponse = "{\"id\": 123, \"success\": true}";
        
        // Mock token service
        when(tokenService.refreshToken()).thenReturn(newToken);
        when(tokenService.getToken()).thenReturn(newToken);
        
        // Mock REST template response
        ResponseEntity<String> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);
        when(restTemplate.exchange(
                anyString(), 
                eq(HttpMethod.POST), 
                any(HttpEntity.class), 
                eq(String.class)))
            .thenReturn(responseEntity);
        
        // Act
        ApiResponse response = apiClientService.retryWithNewToken(route, method, payload, null, null);
        
        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
        
        // Verify token was refreshed
        verify(tokenService).refreshToken();
        
        // Verify the API call was made
        verify(restTemplate).exchange(
                eq("https://api.example.com/data"), 
                eq(HttpMethod.POST), 
                entityCaptor.capture(), 
                eq(String.class));
        
        // Verify payload and headers
        HttpEntity<String> capturedEntity = entityCaptor.getValue();
        assertEquals(payload, capturedEntity.getBody());
        assertEquals(newToken, capturedEntity.getHeaders().getFirst("Authorization"));
    }
}