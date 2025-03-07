package com.etljobs.sql2json2api.service.http;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.etljobs.sql2json2api.model.AuthenticationDetails;

/**
 * A mock implementation of TokenService for testing purposes.
 * This will be used only when the 'api-call-demo' profile is active.
 */
@Service
@Profile("api-call-demo")
public class MockTokenService extends TokenService {
    
    public MockTokenService() {
        super(null, null); // Null arguments will be ignored
    }
    
    /**
     * Returns a fake token for testing.
     */
    @Override
    public String getToken() {
        return "Bearer mock-token-for-demo-purposes";
    }
    
    /**
     * Returns the fake authentication details.
     */
    @Override
    public AuthenticationDetails getAuthenticationDetails() {
        return AuthenticationDetails.builder()
                .authUrl("https://httpbin.org/post")
                .username("demo_user")
                .password("*****")
                .token(getToken())
                .build();
    }
    
    /**
     * Simulates refreshing the token.
     */
    @Override
    public String refreshToken() {
        return "Bearer refreshed-mock-token-" + System.currentTimeMillis();
    }
}