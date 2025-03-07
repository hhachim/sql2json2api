package com.etljobs.sql2json2api.service.template;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import com.etljobs.sql2json2api.exception.TemplateProcessingException;
import com.etljobs.sql2json2api.model.ApiEndpointInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for extracting API metadata from Freemarker templates.
 */
@Service
@Slf4j
public class TemplateMetadataService {
    
    // Modifié pour mieux capturer les métadonnées dans différents formats
    private static final Pattern METADATA_PATTERN = Pattern.compile("@api-(\\w+):\\s*([^@\\n]+)", Pattern.DOTALL);
    private final ObjectMapper objectMapper;
    
    public TemplateMetadataService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    /**
     * Extracts API endpoint information from the template content.
     * 
     * @param templateContent The content of the template
     * @return ApiEndpointInfo object containing the extracted metadata
     * @throws TemplateProcessingException if metadata extraction fails
     */
    public ApiEndpointInfo extractMetadataFromTemplate(String templateContent) {
        Map<String, String> metadata = new HashMap<>();
        
        // Extract metadata using regex
        Matcher matcher = METADATA_PATTERN.matcher(templateContent);
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = matcher.group(2).trim();
            metadata.put(key, value);
            log.debug("Extracted metadata: {}={}", key, value);
        }
        
        // Require essential metadata
        if (!metadata.containsKey("route") || !metadata.containsKey("method")) {
            throw new TemplateProcessingException(
                    "Template metadata is incomplete. Required fields: @api-route, @api-method");
        }
        
        try {
            // Create ApiEndpointInfo object
            ApiEndpointInfo endpointInfo = new ApiEndpointInfo();
            endpointInfo.setRoute(metadata.get("route"));
            endpointInfo.setMethod(HttpMethod.valueOf(metadata.get("method").toUpperCase()));
            
            // Parse headers if present
            if (metadata.containsKey("headers")) {
                Map<String, String> headers = parseJsonMap(metadata.get("headers"), 
                        new TypeReference<Map<String, String>>() {});
                endpointInfo.setHeaders(headers);
            }
            
            // Parse URL parameters if present
            if (metadata.containsKey("params")) {
                Map<String, Object> params = parseJsonMap(metadata.get("params"), 
                        new TypeReference<Map<String, Object>>() {});
                endpointInfo.setUrlParams(params);
            }
            
            return endpointInfo;
            
        } catch (Exception e) {
            throw new TemplateProcessingException("Failed to parse template metadata", e);
        }
    }
    
    /**
     * Parses a JSON string into a Map.
     * 
     * @param <T> The type for the map value
     * @param json The JSON string to parse
     * @param typeRef The TypeReference for deserialization
     * @return The parsed Map
     * @throws JsonProcessingException If JSON parsing fails
     */
    private <T> Map<String, T> parseJsonMap(String json, TypeReference<Map<String, T>> typeRef) 
            throws JsonProcessingException {
        return objectMapper.readValue(json, typeRef);
    }
}