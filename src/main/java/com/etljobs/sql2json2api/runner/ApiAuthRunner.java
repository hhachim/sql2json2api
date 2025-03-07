package com.etljobs.sql2json2api.runner;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import com.etljobs.sql2json2api.model.ApiResponse;
import com.etljobs.sql2json2api.model.AuthenticationDetails;
import com.etljobs.sql2json2api.service.http.ApiClientService;
import com.etljobs.sql2json2api.service.http.TokenService;

import lombok.extern.slf4j.Slf4j;

/**
 * Command line runner for testing API authentication functionality.
 * This will run when the 'api-auth-demo' profile is active.
 */
@Component
@Slf4j
@Profile("api-auth-demo")
public class ApiAuthRunner implements CommandLineRunner, ExitCodeGenerator {
    
    private final TokenService tokenService;
    private final ApiClientService apiClientService;
    private int exitCode = 0;
    
    @Autowired
    public ApiAuthRunner(TokenService tokenService, ApiClientService apiClientService) {
        this.tokenService = tokenService;
        this.apiClientService = apiClientService;
    }
    
    @Override
    public void run(String... args) throws Exception {
        try {
            log.info("=== API Authentication Demo ===");
            
            // Get authentication details
            AuthenticationDetails authDetails = tokenService.getAuthenticationDetails();
            log.info("Auth URL: {}", authDetails.getAuthUrl());
            log.info("Username: {}", authDetails.getUsername());
            log.info("Generated Token: {}", authDetails.getToken());
            
            // Test API call with token
            log.info("\nTesting API call with authentication...");
            
            // Example API endpoint to test (should be configurable in a real scenario)
            String testApiUrl = "https://httpbin.org/get";
            
            // Prepare headers
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            headers.put("Accept", "application/json");
            
            // Prepare URL parameters
            Map<String, Object> urlParams = new HashMap<>();
            urlParams.put("testParam", "testValue");
            
            // Make the API call
            ApiResponse response = apiClientService.callApi(
                    testApiUrl, HttpMethod.GET, null, headers, urlParams);
            
            // Log results
            log.info("API Call Status: {}", response.getStatusCode());
            log.info("API Call Success: {}", response.isSuccess());
            log.info("Response Body: {}", response.getBody());
            
            // Test token refresh
            log.info("\nTesting token refresh...");
            String refreshedToken = tokenService.refreshToken();
            log.info("Refreshed Token: {}", refreshedToken);
            
            // Make another API call with refreshed token
            ApiResponse secondResponse = apiClientService.callApi(
                    testApiUrl, HttpMethod.GET, null, headers, urlParams);
            
            log.info("Second API Call Status: {}", secondResponse.getStatusCode());
            log.info("Second API Call Success: {}", secondResponse.isSuccess());
            
            log.info("=== Demo Complete ===");
            
        } catch (Exception e) {
            log.error("Error during API authentication demo", e);
            exitCode = 1;
        }
    }
    
    @Override
    public int getExitCode() {
        return exitCode;
    }
}