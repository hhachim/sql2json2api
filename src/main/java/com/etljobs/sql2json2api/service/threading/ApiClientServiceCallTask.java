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
        long startTime = System.currentTimeMillis();
        log.debug("Délégation de l'appel API à ApiClientService: {} {}", getMethod(), getUrl());
        
        // Utiliser le service existant pour effectuer l'appel
        com.etljobs.sql2json2api.model.ApiResponse legacyResponse = 
                apiClientService.callApi(getUrl(), getMethod(), getPayload(), getHeaders(), getUrlParams());
        
        long executionTime = System.currentTimeMillis() - startTime;
        
        // Logguer la réponse pour débogage
        if (legacyResponse != null) {
            log.debug("Réponse API reçue: statut={}, taille corps={}", 
                    legacyResponse.getStatusCode(), 
                    (legacyResponse.getBody() != null ? legacyResponse.getBody().length() : 0));
            
            if (legacyResponse.getBody() != null && legacyResponse.getBody().length() < 1000) {
                log.debug("Corps de la réponse: {}", legacyResponse.getBody());
            }
        } else {
            log.warn("Réponse API nulle reçue");
        }
        
        // Convertir la réponse legacy au nouveau format ApiResponse
        return convertToApiResponse(legacyResponse, executionTime);
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
                    .requestId(getRowIdentifier())
                    .attemptNumber(1)
                    .executionTimeMs(executionTime)
                    .build();
        }
        
        return ApiResponse.builder()
                .statusCode(legacyResponse.getStatusCode())
                .body(legacyResponse.getBody())
                .requestUrl(getUrl())
                .requestId(getRowIdentifier())
                .attemptNumber(1)
                .executionTimeMs(executionTime)
                .build();
    }
}