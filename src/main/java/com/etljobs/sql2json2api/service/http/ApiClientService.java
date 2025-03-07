package com.etljobs.sql2json2api.service.http;

import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.etljobs.sql2json2api.exception.ApiCallException;
import com.etljobs.sql2json2api.model.ApiResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for making API calls with authentication.
 */
@Service
@Slf4j
public class ApiClientService {
    
    private final RestTemplate restTemplate;
    private final TokenService tokenService;
    
    public ApiClientService(RestTemplate restTemplate, TokenService tokenService) {
        this.restTemplate = restTemplate;
        this.tokenService = tokenService;
    }
    
    /**
     * Makes an authenticated API call.
     * 
     * @param route The API route/URL to call
     * @param method The HTTP method to use
     * @param payload The JSON payload to send (for POST/PUT)
     * @param headers Additional headers to include
     * @param urlParams URL parameters to include
     * @return ApiResponse containing status code and response body
     * @throws ApiCallException if the API call fails
     */
    public ApiResponse callApi(String route, HttpMethod method, String payload, 
                              Map<String, String> headers, Map<String, Object> urlParams) {
        try {
            log.debug("Making {} call to {}", method, route);
            
            // Build URL with parameters
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(route);
            if (urlParams != null && !urlParams.isEmpty()) {
                urlParams.forEach((key, value) -> {
                    if (value != null) {
                        builder.queryParam(key, value);
                    }
                });
            }
            String url = builder.build().toUriString();
            
            // Get authentication token
            String token = tokenService.getToken();
            
            // Prepare HTTP headers
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.set("Authorization", token);
            
            // Add custom headers
            if (headers != null && !headers.isEmpty()) {
                headers.forEach(httpHeaders::set);
            }
            
            // Create HTTP entity with headers and payload
            HttpEntity<String> entity = new HttpEntity<>(payload, httpHeaders);
            
            // Make the API call
            ResponseEntity<String> response = restTemplate.exchange(
                url, method, entity, String.class);
            
            log.debug("API call successful. Status: {}", response.getStatusCode());
            
            // Create and return the response
            return new ApiResponse(response.getStatusCode().value(), response.getBody());
            
        } catch (HttpStatusCodeException e) {
            // Capture HTTP error responses (4xx, 5xx)
            log.error("API call failed with status {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return new ApiResponse(e.getStatusCode().value(), e.getResponseBodyAsString());
            
        } catch (Exception e) {
            // Handle other exceptions
            log.error("API call failed", e);
            throw new ApiCallException("Failed to make API call to " + route, e);
        }
    }
    
    /**
     * Simplified version of callApi with fewer parameters.
     */
    public ApiResponse callApi(String route, HttpMethod method, String payload) {
        return callApi(route, method, payload, null, null);
    }
    
    /**
     * Retry an API call if it failed due to authentication issues.
     * This will refresh the token and try again.
     */
    public ApiResponse retryWithNewToken(String route, HttpMethod method, String payload, 
                                      Map<String, String> headers, Map<String, Object> urlParams) {
        log.debug("Retrying API call with new token...");
        
        // Force token refresh
        tokenService.refreshToken();
        
        // Try the call again
        return callApi(route, method, payload, headers, urlParams);
    }
}