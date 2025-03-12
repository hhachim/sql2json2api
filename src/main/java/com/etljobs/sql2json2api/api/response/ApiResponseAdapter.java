package com.etljobs.sql2json2api.api.response;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

/**
 * Adaptateur pour convertir entre les différentes versions de classes de réponse API.
 * Cette classe est utile pendant la période de transition entre l'ancien et le nouveau modèle.
 */
@Component
public class ApiResponseAdapter {
    
    /**
     * Convertit un ApiResponse du nouveau package vers l'ancien format.
     * 
     * @param response Réponse au nouveau format
     * @return Réponse au format legacy
     */
    public com.etljobs.sql2json2api.model.ApiResponse toLegacy(ApiResponse response) {
        if (response == null) {
            return null;
        }
        
        return com.etljobs.sql2json2api.model.ApiResponse.builder()
                .statusCode(response.getStatusCode())
                .body(response.getBody())
                .build();
    }
    
    /**
     * Convertit une liste d'ApiResponse du nouveau package vers l'ancien format.
     * 
     * @param responses Liste de réponses au nouveau format
     * @return Liste de réponses au format legacy
     */
    public List<com.etljobs.sql2json2api.model.ApiResponse> toLegacyList(List<ApiResponse> responses) {
        if (responses == null) {
            return null;
        }
        
        return responses.stream()
                .map(this::toLegacy)
                .collect(Collectors.toList());
    }
    
    /**
     * Convertit un ApiResponse de l'ancien format vers le nouveau package.
     * 
     * @param legacyResponse Réponse au format legacy
     * @return Réponse au nouveau format
     */
    public ApiResponse fromLegacy(com.etljobs.sql2json2api.model.ApiResponse legacyResponse) {
        if (legacyResponse == null) {
            return null;
        }
        
        return ApiResponse.builder()
                .statusCode(legacyResponse.getStatusCode())
                .body(legacyResponse.getBody())
                .build();
    }
    
    /**
     * Convertit une liste d'ApiResponse de l'ancien format vers le nouveau package.
     * 
     * @param legacyResponses Liste de réponses au format legacy
     * @return Liste de réponses au nouveau format
     */
    public List<ApiResponse> fromLegacyList(List<com.etljobs.sql2json2api.model.ApiResponse> legacyResponses) {
        if (legacyResponses == null) {
            return null;
        }
        
        return legacyResponses.stream()
                .map(this::fromLegacy)
                .collect(Collectors.toList());
    }
    
    /**
     * Enrichit un ApiResponse legacy avec des informations supplémentaires.
     * 
     * @param legacyResponse Réponse au format legacy
     * @param requestId Identifiant de la requête
     * @param requestUrl URL de la requête
     * @param executionTimeMs Temps d'exécution en millisecondes
     * @return Réponse enrichie au nouveau format
     */
    public ApiResponse enrichFromLegacy(
            com.etljobs.sql2json2api.model.ApiResponse legacyResponse,
            String requestId,
            String requestUrl,
            long executionTimeMs) {
        
        if (legacyResponse == null) {
            return null;
        }
        
        return ApiResponse.builder()
                .statusCode(legacyResponse.getStatusCode())
                .body(legacyResponse.getBody())
                .requestId(requestId)
                .requestUrl(requestUrl)
                .executionTimeMs(executionTimeMs)
                .build();
    }
}