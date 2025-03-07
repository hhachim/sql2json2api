package com.etljobs.sql2json2api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Contains the authentication details for API calls.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticationDetails {
    
    /**
     * Base URL for authentication
     */
    private String authUrl;
    
    /**
     * Username for authentication
     */
    private String username;
    
    /**
     * Password for authentication
     */
    private String password;
    
    /**
     * Generated token (Bearer token)
     */
    private String token;
}