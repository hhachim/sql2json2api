package com.etljobs.sql2json2api.service.http;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;

import com.etljobs.sql2json2api.api.execution.ApiCallExecutor;
import com.etljobs.sql2json2api.api.request.ApiRequest;
import com.etljobs.sql2json2api.api.request.ApiRequestFactory;
import com.etljobs.sql2json2api.api.response.ApiResponse;
import com.etljobs.sql2json2api.api.response.ApiResponseAdapter;
import com.etljobs.sql2json2api.exception.ApiCallException;
import com.etljobs.sql2json2api.model.ApiEndpointInfo;
import com.etljobs.sql2json2api.model.ApiTemplateResult;

@ExtendWith(MockitoExtension.class)
class ApiClientServiceTest {

    @Mock
    private ApiCallExecutor apiCallExecutor;
    
    @Mock
    private TokenService tokenService;
    
    @Mock
    private ApiRequestFactory requestFactory;
    
    @Mock
    private ApiResponseAdapter responseAdapter;
    
    private ApiClientService apiClientService;
    
    @BeforeEach
    void setUp() {
        apiClientService = new ApiClientService(
            apiCallExecutor, 
            tokenService, 
            requestFactory, 
            responseAdapter
        );
    }
    
    @Test
    void callApi_ShouldDelegateToApiCallExecutor() {
        // Arrange
        String url = "https://api.example.com/data";
        HttpMethod method = HttpMethod.GET;
        String token = "Bearer test-token";
        
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        
        Map<String, Object> params = new HashMap<>();
        params.put("param1", "value1");
        
        // Mocks for new API
        ApiResponse newApiResponse = ApiResponse.builder()
                .statusCode(200)
                .body("{\"success\":true}")
                .build();
        
        // Mock for legacy response
        com.etljobs.sql2json2api.model.ApiResponse legacyResponse = 
                com.etljobs.sql2json2api.model.ApiResponse.builder()
                .statusCode(200)
                .body("{\"success\":true}")
                .build();
        
        when(apiCallExecutor.execute(any(ApiRequest.class))).thenReturn(newApiResponse);
        when(responseAdapter.toLegacy(newApiResponse)).thenReturn(legacyResponse);
        
        // Act
        com.etljobs.sql2json2api.model.ApiResponse result = 
                apiClientService.callApi(url, method, null, headers, params);
        
        // Assert
        assertEquals(legacyResponse, result);
        
        // Verify executor was called with correct params
        verify(apiCallExecutor).execute(any(ApiRequest.class));
        verify(responseAdapter).toLegacy(newApiResponse);
    }
    
    @Test
    void callApi_ShouldHandleExceptionsAndThrowApiCallException() {
        // Arrange
        String url = "https://api.example.com/data";
        HttpMethod method = HttpMethod.GET;
        
        // Mock exception in executor
        RuntimeException executorException = new RuntimeException("Executor error");
        when(apiCallExecutor.execute(any(ApiRequest.class))).thenThrow(executorException);
        
        // Act & Assert
        assertThrows(ApiCallException.class, () -> {
            apiClientService.callApi(url, method, null);
        });
    }
    
    @Test
    void callApiFromTemplate_ShouldUseRequestFactoryAndExecutor() {
        // Arrange
        ApiEndpointInfo endpointInfo = new ApiEndpointInfo();
        endpointInfo.setRoute("https://api.example.com/data");
        endpointInfo.setMethod(HttpMethod.POST);
        
        ApiTemplateResult templateResult = new ApiTemplateResult(
                "{\"data\":\"test\"}", 
                endpointInfo);
        
        ApiRequest request = ApiRequest.builder()
                .url(endpointInfo.getRoute())
                .method(endpointInfo.getMethod())
                .payload(templateResult.getJsonPayload())
                .build();
        
        ApiResponse newApiResponse = ApiResponse.builder()
                .statusCode(201)
                .body("{\"id\":123}")
                .build();
        
        com.etljobs.sql2json2api.model.ApiResponse legacyResponse = 
                com.etljobs.sql2json2api.model.ApiResponse.builder()
                .statusCode(201)
                .body("{\"id\":123}")
                .build();
        
        when(tokenService.getToken()).thenReturn("Bearer token");
        when(requestFactory.createFromTemplateResult(eq(templateResult), anyString()))
                .thenReturn(request);
        when(apiCallExecutor.execute(request)).thenReturn(newApiResponse);
        when(responseAdapter.toLegacy(newApiResponse)).thenReturn(legacyResponse);
        
        // Act
        com.etljobs.sql2json2api.model.ApiResponse result = 
                apiClientService.callApiFromTemplate(templateResult);
        
        // Assert
        assertEquals(legacyResponse, result);
        
        verify(requestFactory).createFromTemplateResult(eq(templateResult), anyString());
        verify(apiCallExecutor).execute(request);
        verify(responseAdapter).toLegacy(newApiResponse);
    }
    
    @Test
    void retryWithNewToken_ShouldCreateRequestWithRefreshTokenFlag() {
        // Arrange
        String url = "https://api.example.com/data";
        HttpMethod method = HttpMethod.GET;
        
        ApiResponse newApiResponse = ApiResponse.builder()
                .statusCode(200)
                .body("{\"success\":true}")
                .build();
        
        com.etljobs.sql2json2api.model.ApiResponse legacyResponse = 
                com.etljobs.sql2json2api.model.ApiResponse.builder()
                .statusCode(200)
                .body("{\"success\":true}")
                .build();
        
        when(apiCallExecutor.execute(any(ApiRequest.class))).thenReturn(newApiResponse);
        when(responseAdapter.toLegacy(newApiResponse)).thenReturn(legacyResponse);
        
        // Act
        com.etljobs.sql2json2api.model.ApiResponse result = 
                apiClientService.retryWithNewToken(url, method, null, null, null);
        
        // Assert
        assertEquals(legacyResponse, result);
        
        // Verify - important to check the refreshToken flag was set
        verify(apiCallExecutor).execute(any(ApiRequest.class));
    }
    
    @Test
    void callApiWithRetry_ShouldUseExecuteWithRetry() {
        // Arrange
        String url = "https://api.example.com/data";
        HttpMethod method = HttpMethod.GET;
        int maxRetries = 3;
        boolean refreshTokenOnRetry = true;
        
        ApiResponse newApiResponse = ApiResponse.builder()
                .statusCode(200)
                .body("{\"success\":true}")
                .build();
        
        com.etljobs.sql2json2api.model.ApiResponse legacyResponse = 
                com.etljobs.sql2json2api.model.ApiResponse.builder()
                .statusCode(200)
                .body("{\"success\":true}")
                .build();
        
        when(apiCallExecutor.executeWithRetry(
                any(ApiRequest.class), any(), eq(maxRetries), anyLong()))
                .thenReturn(newApiResponse);
        when(responseAdapter.toLegacy(newApiResponse)).thenReturn(legacyResponse);
        
        // Act
        com.etljobs.sql2json2api.model.ApiResponse result = 
                apiClientService.callApiWithRetry(url, method, null, null, null, maxRetries, refreshTokenOnRetry);
        
        // Assert
        assertEquals(legacyResponse, result);
        
        // Verify executeWithRetry was called instead of normal execute
        verify(apiCallExecutor).executeWithRetry(
                any(ApiRequest.class), any(), eq(maxRetries), anyLong());
        verify(apiCallExecutor, never()).execute(any(ApiRequest.class));
    }
    
    @Test
    void callApiWithRetry_SimplifiedMethod_ShouldUseDefaultValues() {
        // Arrange
        String url = "https://api.example.com/data";
        HttpMethod method = HttpMethod.GET;
        
        ApiResponse newApiResponse = ApiResponse.builder()
                .statusCode(200)
                .body("{\"success\":true}")
                .build();
        
        com.etljobs.sql2json2api.model.ApiResponse legacyResponse = 
                com.etljobs.sql2json2api.model.ApiResponse.builder()
                .statusCode(200)
                .body("{\"success\":true}")
                .build();
        
        when(apiCallExecutor.executeWithRetry(
                any(ApiRequest.class), any(), anyInt(), anyLong()))
                .thenReturn(newApiResponse);
        when(responseAdapter.toLegacy(newApiResponse)).thenReturn(legacyResponse);
        
        // Act
        com.etljobs.sql2json2api.model.ApiResponse result = 
                apiClientService.callApiWithRetry(url, method, null);
        
        // Assert
        assertEquals(legacyResponse, result);
        
        // Verify executeWithRetry was called with default values
        verify(apiCallExecutor).executeWithRetry(
                any(ApiRequest.class), any(), eq(2), anyLong());
    }
}