package com.etljobs.sql2json2api.service.http;

import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.etljobs.sql2json2api.exception.ApiCallException;
import com.etljobs.sql2json2api.model.AuthenticationDetails;
import com.etljobs.sql2json2api.util.FileUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;

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
    
    @Value("${api.auth.payload-template-path:auth/auth-payload.ftlh}")
    private String payloadTemplatePath;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final Configuration freemarkerConfiguration;
    
    private String cachedToken;
    private Instant tokenExpiration;
    
    @Autowired
    public TokenService(RestTemplate restTemplate, ObjectMapper objectMapper, 
                       @Qualifier("freemarkerConfiguration") Configuration freemarkerConfiguration) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.freemarkerConfiguration = freemarkerConfiguration;
    }

    /**
     * Génère le payload JSON à partir du template Freemarker et des variables d'authentification
     * 
     * @return Le payload JSON généré
     * @throws Exception Si une erreur survient lors du traitement du template
     */
    protected String generatePayload() throws Exception {
        // Préparer le modèle de données pour Freemarker
        Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("username", username);
        dataModel.put("password", password);
        
        StringWriter writer = new StringWriter();
        
        // Vérifier si le chemin du template est absolu
        if (FileUtils.isAbsolutePath(payloadTemplatePath)) {
            // Utiliser le système de fichiers pour charger le template
            log.debug("Chargement du template d'authentification depuis le chemin absolu: {}", payloadTemplatePath);
            Path templatePath = Paths.get(payloadTemplatePath);
            
            if (!Files.exists(templatePath)) {
                throw new ApiCallException("Le template d'authentification n'existe pas: " + payloadTemplatePath);
            }
            
            // Charger et parser le contenu du template directement
            String templateContent = FileUtils.readFileContent(templatePath);
            Template template = new Template("auth-template", templateContent, freemarkerConfiguration);
            
            // Traiter le template
            template.process(dataModel, writer);
        } else {
            // Comportement original : utiliser le ClassLoader de Freemarker
            log.debug("Chargement du template d'authentification depuis le classpath: {}", payloadTemplatePath);
            Template template = freemarkerConfiguration.getTemplate(payloadTemplatePath);
            template.process(dataModel, writer);
        }
        
        return writer.toString();
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
            
            // Générer le payload JSON en utilisant Freemarker
            String payload = generatePayload();
            log.debug("Auth request payload (generated with Freemarker): {}", payload);
            
            // Create HTTP entity with headers and payload
            HttpEntity<String> entity = new HttpEntity<>(payload, headers);
            
            // Make the API call
            ResponseEntity<String> response = restTemplate.exchange(
                    authUrl, HttpMethod.POST, entity, String.class);
            
            // Process the response
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String tokenResponse = response.getBody();
                log.debug("Auth response received: {}", tokenResponse);
                
                // Extract token from response
                JsonNode rootNode = objectMapper.readTree(tokenResponse);
                String token = extractToken(rootNode);
                
                // Cache the token
                cachedToken = "Bearer " + token;
                
                // Set expiration
                tokenExpiration = Instant.now().plus(Duration.ofSeconds(tokenTtlSeconds));
                
                log.debug("New token generated, expires at: {}", tokenExpiration);
                
                return cachedToken;
            } else {
                throw new ApiCallException("Failed to get authentication token. Status: " 
                        + response.getStatusCode());
            }
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