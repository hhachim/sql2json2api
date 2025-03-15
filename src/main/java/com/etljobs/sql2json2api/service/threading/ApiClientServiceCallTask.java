package com.etljobs.sql2json2api.service.threading;

import java.util.Map;

import org.springframework.http.HttpMethod;

import com.etljobs.sql2json2api.api.response.ApiResponse;
import com.etljobs.sql2json2api.service.http.ApiClientService;

import lombok.extern.slf4j.Slf4j;

/**
 * Implémentation concrète de ApiCallTask qui délègue l'exécution
 * au service ApiClientService existant.
 */
@Slf4j
public class ApiClientServiceCallTask extends ApiCallTask {
    
    private final ApiClientService apiClientService;
    private final String token;
    
    /**
     * Constructeur avec tous les paramètres nécessaires pour un appel API.
     */
    public ApiClientServiceCallTask(
            String url, 
            HttpMethod method, 
            String payload, 
            Map<String, String> headers, 
            Map<String, Object> urlParams,
            int rowIndex, 
            String rowIdentifier,
            ApiClientService apiClientService,
            String token) {
        super(url, method, payload, headers, urlParams, rowIndex, rowIdentifier);
        this.apiClientService = apiClientService;
        this.token = token;
    }
    
    @Override
    protected ApiResponse executeApiCall() throws Exception {
        log.debug("Délégation de l'appel API à ApiClientService: {} {} (correlationId: {})", 
                getMethod(), getUrl(), correlationId);
        
        try {
            long startTime = System.currentTimeMillis();
            
            // Utiliser le service existant pour effectuer l'appel
            com.etljobs.sql2json2api.model.ApiResponse legacyResponse = 
                    apiClientService.callApi(getUrl(), getMethod(), getPayload(), getHeaders(), getUrlParams());
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Logguer la réponse pour débogage
            if (legacyResponse != null) {
                log.debug("Réponse API reçue: statut={}, taille corps={} (correlationId: {})", 
                        legacyResponse.getStatusCode(), 
                        (legacyResponse.getBody() != null ? legacyResponse.getBody().length() : 0),
                        correlationId);
                
                if (legacyResponse.getBody() != null && legacyResponse.getBody().length() < 1000) {
                    log.debug("Corps de la réponse (correlationId: {}): {}", correlationId, legacyResponse.getBody());
                }
            } else {
                log.warn("Réponse API nulle reçue (correlationId: {})", correlationId);
            }
            
            // Convertir la réponse legacy au nouveau format ApiResponse
            ApiResponse response = convertToApiResponse(legacyResponse, executionTime);
            
            // Ajouter l'ID de corrélation à la réponse
            response.setRequestId(correlationId);
            
            return response;
        } catch (Exception e) {
            log.error("Erreur lors de l'appel API à ApiClientService (correlationId: {}): {}", 
                    correlationId, e.getMessage());
            throw e;
        }
    }
    
    /**
     * Convertit une réponse legacy vers le nouveau format ApiResponse.
     */
    private ApiResponse convertToApiResponse(com.etljobs.sql2json2api.model.ApiResponse legacyResponse, long executionTime) {
        if (legacyResponse == null) {
            return ApiResponse.builder()
                    .statusCode(500)
                    .body("Réponse nulle reçue du service API")
                    .requestUrl(getUrl())
                    .requestId(correlationId)
                    .attemptNumber(1)
                    .executionTimeMs(executionTime)
                    .build();
        }
        
        return ApiResponse.builder()
                .statusCode(legacyResponse.getStatusCode())
                .body(legacyResponse.getBody())
                .requestUrl(getUrl())
                .requestId(correlationId)
                .attemptNumber(1)
                .executionTimeMs(executionTime)
                .build();
    }
}