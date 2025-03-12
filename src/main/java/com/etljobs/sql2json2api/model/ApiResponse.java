package com.etljobs.sql2json2api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a response from an API call.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse {
    
    /**
     * HTTP status code
     */
    private int statusCode;
    
    /**
     * Response body content
     */
    private String body;
    
    /**
     * Returns true if the status code indicates success (2xx)
     */
    public boolean isSuccess() {
        return statusCode >= 200 && statusCode < 300;
    }
}