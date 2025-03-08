package com.etljobs.sql2json2api.api.request;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import com.etljobs.sql2json2api.model.ApiEndpointInfo;

class ApiRequestBuilderTest {

    @Test
    void testCreate_ShouldReturnNewInstance() {
        // Act
        ApiRequestBuilder builder = ApiRequestBuilder.create();
        
        // Assert
        assertNotNull(builder);
    }
    
    @Test
    void testFromEndpointInfo_ShouldPopulateFromEndpointInfo() {
        // Arrange
        ApiEndpointInfo endpointInfo = new ApiEndpointInfo();
        endpointInfo.setRoute("https://api.example.com/users");
        endpointInfo.setMethod(HttpMethod.GET);
        
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        endpointInfo.setHeaders(headers);
        
        Map<String, Object> params = new HashMap<>();
        params.put("filter", "active");
        endpointInfo.setUrlParams(params);
        
        // Act
        ApiRequestBuilder builder = ApiRequestBuilder.fromEndpointInfo(endpointInfo);
        ApiRequest request = builder.build();
        
        // Assert
        assertEquals("https://api.example.com/users", request.getUrl());
        assertEquals(HttpMethod.GET, request.getMethod());
        assertEquals("application/json", request.getHeaders().get("Content-Type"));
        assertEquals("active", request.getUrlParams().get("filter"));
    }
    
    @Test
    void testChainedMethods_ShouldSetAllProperties() {
        // Act
        ApiRequest request = ApiRequestBuilder.create()
                .url("https://api.example.com/users")
                .method(HttpMethod.POST)
                .payload("{\"name\":\"test\"}")
                .authToken("Bearer token123")
                .header("X-Custom", "value")
                .urlParam("id", 123)
                .requestId("test-req-001")
                .refreshToken()
                .timeout(5000)
                .build();
        
        // Assert
        assertEquals("https://api.example.com/users", request.getUrl());
        assertEquals(HttpMethod.POST, request.getMethod());
        assertEquals("{\"name\":\"test\"}", request.getPayload());
        assertEquals("Bearer token123", request.getAuthToken());
        assertEquals("value", request.getHeaders().get("X-Custom"));
        assertEquals(123, request.getUrlParams().get("id"));
        assertEquals("test-req-001", request.getRequestId());
        assertTrue(request.isRefreshToken());
        assertEquals(5000, request.getTimeoutMs());
    }
    
    @Test
    void testHttpMethodHelpers_ShouldSetCorrectMethod() {
        // Act
        ApiRequest getRequest = ApiRequestBuilder.create()
                .get("https://api.example.com/users")
                .build();
        
        ApiRequest postRequest = ApiRequestBuilder.create()
                .post("https://api.example.com/users")
                .build();
        
        ApiRequest putRequest = ApiRequestBuilder.create()
                .put("https://api.example.com/users/1")
                .build();
        
        ApiRequest deleteRequest = ApiRequestBuilder.create()
                .delete("https://api.example.com/users/1")
                .build();
        
        // Assert
        assertEquals(HttpMethod.GET, getRequest.getMethod());
        assertEquals(HttpMethod.POST, postRequest.getMethod());
        assertEquals(HttpMethod.PUT, putRequest.getMethod());
        assertEquals(HttpMethod.DELETE, deleteRequest.getMethod());
    }
    
    @Test
    void testStandardJsonHeaders_ShouldAddCorrectHeaders() {
        // Act
        ApiRequest request = ApiRequestBuilder.create()
                .url("https://api.example.com/users")
                .method(HttpMethod.GET)
                .standardJsonHeaders()
                .build();
        
        // Assert
        assertEquals("application/json", request.getHeaders().get("Content-Type"));
        assertEquals("application/json", request.getHeaders().get("Accept"));
    }
    
    @Test
    void testBuild_ShouldThrowException_WhenUrlMissing() {
        // Arrange
        ApiRequestBuilder builder = ApiRequestBuilder.create()
                .method(HttpMethod.GET);
        
        // Act & Assert
        assertThrows(IllegalStateException.class, () -> builder.build());
    }
    
    @Test
    void testBuild_ShouldThrowException_WhenMethodMissing() {
        // Arrange
        ApiRequestBuilder builder = ApiRequestBuilder.create()
                .url("https://api.example.com/users");
        
        // Act & Assert
        assertThrows(IllegalStateException.class, () -> builder.build());
    }
}