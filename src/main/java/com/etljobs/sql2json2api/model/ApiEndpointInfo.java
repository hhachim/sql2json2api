package com.etljobs.sql2json2api.model;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpMethod;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Contains information about an API endpoint extracted from templates.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiEndpointInfo {
    
    /**
     * The route or URL of the API endpoint
     */
    private String route;
    
    /**
     * The HTTP method to use (GET, POST, PUT, DELETE, etc.)
     */
    private HttpMethod method;
    
    /**
     * HTTP headers to include in the request
     */
    @Builder.Default
    private Map<String, String> headers = new HashMap<>();
    
    /**
     * URL parameters to include in the request
     */
    @Builder.Default
    private Map<String, Object> urlParams = new HashMap<>();
}