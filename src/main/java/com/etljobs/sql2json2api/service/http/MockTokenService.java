package com.etljobs.sql2json2api.service.http;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.etljobs.sql2json2api.model.AuthenticationDetails;
import com.fasterxml.jackson.databind.ObjectMapper;

import freemarker.template.Configuration;

/**
 * A mock implementation of TokenService for testing purposes.
 */
@Service
@Profile("api-call-demo")
@Primary
public class MockTokenService extends TokenService {
    
    public MockTokenService(RestTemplate restTemplate, ObjectMapper objectMapper, Configuration freemarkerConfiguration) {
        super(restTemplate, objectMapper, freemarkerConfiguration);
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