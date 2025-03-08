package com.etljobs.sql2json2api.service.http;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.etljobs.sql2json2api.config.AuthPayloadConfig;
import com.etljobs.sql2json2api.exception.ApiCallException;
import com.etljobs.sql2json2api.model.AuthenticationDetails;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Service responsible for generating and managing authentication tokens.
 */
@Service
@Slf4j
public class TokenService {
    
    @Value("${api.auth.url}")
    private String authUrl;
    
    @Value("${api.auth.username}")
    private String username;
    
    @Value("${api.auth.password}")
    private String password;
    
    @Value("${api.auth.token-ttl:3600}")
    private long tokenTtlSeconds;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final AuthPayloadConfig authPayloadConfig;

    
    private String cachedToken;
    private Instant tokenExpiration;
    
    public TokenService(RestTemplate restTemplate, ObjectMapper objectMapper, AuthPayloadConfig authPayloadConfig) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.authPayloadConfig = authPayloadConfig;
    }

    /**
     * Crée un payload d'authentification selon le format configuré
     * 
     * @return Map contenant le payload
     */
    protected Map<String, Object> createAuthPayload() {
        Map<String, Object> payload = new HashMap<>();
        String format = authPayloadConfig.getPayloadFormat();
        
        // Ajouter username et password avec les noms de champs configurés
        payload.put(authPayloadConfig.getUsernameField(), username);
        payload.put(authPayloadConfig.getPasswordField(), password);
        
        // Selon le format, ajouter des champs supplémentaires
        switch (format) {
            case "api-context":
                // Format spécifique avec champ "context": "api"
                payload.put("context", "api");
                break;
            case "jwt":
                // Format pour certains serveurs JWT
                payload.put("grant_type", "password");
                break;
            case "custom":
                // Utiliser les champs additionnels configurés
                payload.putAll(authPayloadConfig.getAdditionalFields());
                break;
            case "default":
            default:
                // Format simple username/password, rien à ajouter
                break;
        }
        
        return payload;
    }
    
    /**
     * Generates or returns a cached valid authentication token.
     * 
     * @return A valid Bearer token
     * @throws ApiCallException if token generation fails
     */
    public String getToken() {
        // Check if we have a cached token that's still valid
        if (cachedToken != null && tokenExpiration != null && 
                tokenExpiration.isAfter(Instant.now().plus(Duration.ofMinutes(5)))) {
            log.debug("Using cached token (expires in {} seconds)", 
                    Duration.between(Instant.now(), tokenExpiration).getSeconds());
            return cachedToken;
        }
        
        // Otherwise, generate a new token
        log.debug("Generating new authentication token...");
        return generateNewToken();
    }
    
    /**
     * Makes an API call to generate a new authentication token.
     * 
     * @return A new Bearer token
     * @throws ApiCallException if token generation fails
     */
    private String generateNewToken() {
        try {
            // Prepare request headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            
            // Préparer le payload selon la configuration
            Map<String, Object> requestBody = createAuthPayload();
            
            // Create HTTP entity with headers and body
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            // Make the API call
            ResponseEntity<String> response = restTemplate.exchange(
                    authUrl, HttpMethod.POST, entity, String.class);
            
            // Process the response
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String tokenResponse = response.getBody();
                
                // Extract token from response (format may vary depending on your API)
                JsonNode rootNode = objectMapper.readTree(tokenResponse);
                String token = extractToken(rootNode);
                
                // Cache the token
                cachedToken = "Bearer " + token;
                
                // Set expiration (default to configured TTL or 1 hour if not specified)
                tokenExpiration = Instant.now().plus(Duration.ofSeconds(tokenTtlSeconds));
                
                log.debug("New token generated, expires at: {}", tokenExpiration);
                
                return cachedToken;
            } else {
                throw new ApiCallException("Failed to get authentication token. Status: " 
                        + response.getStatusCode());
            }
        } catch (JsonProcessingException e) {
            throw new ApiCallException("Failed to parse authentication token response", e);
        } catch (Exception e) {
            throw new ApiCallException("Failed to generate authentication token", e);
        }
    }
    
    /**
     * Extract the token from the API response.
     * This method can be customized based on the actual response format from your auth API.
     */
    private String extractToken(JsonNode rootNode) {
        // Depending on your API, adapt this to extract the token correctly
        // Example formats: { "token": "xyz" } or { "access_token": "xyz" }
        if (rootNode.has("token")) {
            return rootNode.get("token").asText();
        } else if (rootNode.has("access_token")) {
            return rootNode.get("access_token").asText();
        } else {
            throw new ApiCallException("Could not find token in authentication response");
        }
    }
    
    /**
     * Returns the authentication details, including the token.
     * 
     * @return AuthenticationDetails containing auth info and token
     */
    public AuthenticationDetails getAuthenticationDetails() {
        return AuthenticationDetails.builder()
                .authUrl(authUrl)
                .username(username)
                .password("*****") // Mask password for security
                .token(getToken())
                .build();
    }
    
    /**
     * Force a refresh of the token regardless of its current state.
     * 
     * @return The newly generated token
     */
    public String refreshToken() {
        cachedToken = null;
        tokenExpiration = null;
        return getToken();
    }
}