package com.etljobs.sql2json2api.service.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
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

import com.etljobs.sql2json2api.model.AuthenticationDetails;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private RestTemplate restTemplate;
    
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();
    
    @InjectMocks
    private TokenService tokenService;
    
    @BeforeEach
    void setUp() {
        // Set required fields via reflection as they would normally be injected
        ReflectionTestUtils.setField(tokenService, "authUrl", "https://api.test.com/auth");
        ReflectionTestUtils.setField(tokenService, "username", "testuser");
        ReflectionTestUtils.setField(tokenService, "password", "testpass");
        ReflectionTestUtils.setField(tokenService, "tokenTtlSeconds", 3600L);
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
        
        // Verify the call was made
        verify(restTemplate).exchange(
                anyString(), 
                eq(HttpMethod.POST), 
                any(HttpEntity.class), 
                eq(String.class));
    }
    
    @Test
    void getToken_ShouldReturnCachedToken_WhenValidTokenExists() {
        // Arrange - mock the API response for first call
        String mockResponse = "{\"token\": \"abc123\", \"expires_in\": 3600}";
        ResponseEntity<String> mockResponseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);
        
        when(restTemplate.exchange(
                anyString(), 
                eq(HttpMethod.POST), 
                any(HttpEntity.class), 
                eq(String.class)))
            .thenReturn(mockResponseEntity);
        
        // Act - call getToken twice
        String token1 = tokenService.getToken();
        String token2 = tokenService.getToken();
        
        // Assert
        assertEquals("Bearer abc123", token1);
        assertEquals("Bearer abc123", token2);
        
        // Verify API was called only once
        verify(restTemplate, times(1)).exchange(
                anyString(), 
                eq(HttpMethod.POST), 
                any(HttpEntity.class), 
                eq(String.class));
    }
    
    @Test
    void refreshToken_ShouldGenerateNewToken_EvenIfCachedTokenExists() {
        // Arrange - mock the API responses
        String mockResponse1 = "{\"token\": \"abc123\", \"expires_in\": 3600}";
        String mockResponse2 = "{\"token\": \"xyz789\", \"expires_in\": 3600}";
        
        ResponseEntity<String> mockResponseEntity1 = new ResponseEntity<>(mockResponse1, HttpStatus.OK);
        ResponseEntity<String> mockResponseEntity2 = new ResponseEntity<>(mockResponse2, HttpStatus.OK);
        
        when(restTemplate.exchange(
                anyString(), 
                eq(HttpMethod.POST), 
                any(HttpEntity.class), 
                eq(String.class)))
            .thenReturn(mockResponseEntity1)
            .thenReturn(mockResponseEntity2);
        
        // Act - get a token, then refresh
        String token1 = tokenService.getToken();
        String token2 = tokenService.refreshToken();
        
        // Assert
        assertEquals("Bearer abc123", token1);
        assertEquals("Bearer xyz789", token2);
        
        // Verify API was called twice
        verify(restTemplate, times(2)).exchange(
                anyString(), 
                eq(HttpMethod.POST), 
                any(HttpEntity.class), 
                eq(String.class));
    }
    
    @Test
    void getAuthenticationDetails_ShouldReturnCompleteDetails() {
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
        AuthenticationDetails authDetails = tokenService.getAuthenticationDetails();
        
        // Assert
        assertNotNull(authDetails);
        assertEquals("https://api.test.com/auth", authDetails.getAuthUrl());
        assertEquals("testuser", authDetails.getUsername());
        assertEquals("*****", authDetails.getPassword()); // Password should be masked
        assertEquals("Bearer abc123", authDetails.getToken());
    }
}