package com.etljobs.sql2json2api.service.http;

import java.util.Map;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import com.etljobs.sql2json2api.api.execution.ApiCallExecutor;
import com.etljobs.sql2json2api.api.request.ApiRequest;
import com.etljobs.sql2json2api.api.request.ApiRequestBuilder;
import com.etljobs.sql2json2api.api.request.ApiRequestFactory;
import com.etljobs.sql2json2api.api.response.ApiResponse;
import com.etljobs.sql2json2api.api.response.ApiResponseAdapter;
import com.etljobs.sql2json2api.exception.ApiCallException;
import com.etljobs.sql2json2api.model.ApiTemplateResult;

import lombok.extern.slf4j.Slf4j;

/**
 * Service pour effectuer des appels API avec authentification.
 * Version refactorisée qui délègue l'exécution des appels à ApiCallExecutor.
 */
@Service
@Slf4j
public class ApiClientService {
    
    private final ApiCallExecutor apiCallExecutor;
    private final TokenService tokenService;
    private final ApiRequestFactory requestFactory;
    private final ApiResponseAdapter responseAdapter;
    
    private static final int DEFAULT_MAX_RETRIES = 2;
    private static final long DEFAULT_RETRY_DELAY_MS = 1000;
    
    public ApiClientService(
            ApiCallExecutor apiCallExecutor,
            TokenService tokenService,
            ApiRequestFactory requestFactory,
            ApiResponseAdapter responseAdapter) {
        this.apiCallExecutor = apiCallExecutor;
        this.tokenService = tokenService;
        this.requestFactory = requestFactory;
        this.responseAdapter = responseAdapter;
    }
    
    /**
     * Méthode principale pour effectuer un appel API.
     * Compatibilité avec l'ancienne API du service.
     * 
     * @param route La route/URL de l'API
     * @param method La méthode HTTP
     * @param payload Le contenu JSON (pour POST/PUT)
     * @param headers En-têtes HTTP additionnels
     * @param urlParams Paramètres d'URL
     * @return Réponse de l'API au format legacy
     */
    public com.etljobs.sql2json2api.model.ApiResponse callApi(String route, HttpMethod method, String payload, 
                              Map<String, String> headers, Map<String, Object> urlParams) {
        try {
            log.debug("Préparation de l'appel API {} {}", method, route);
            
            // Créer la requête avec le builder
            ApiRequest request = ApiRequestBuilder.create()
                    .url(route)
                    .method(method)
                    .payload(payload)
                    .headers(headers)
                    .urlParams(urlParams)
                    .build();
            
            // Exécuter l'appel via l'executor
            ApiResponse response = apiCallExecutor.execute(request);
            
            // Convertir la réponse au format legacy
            return responseAdapter.toLegacy(response);
            
        } catch (Exception e) {
            log.error("Erreur lors de l'appel API", e);
            throw new ApiCallException("Échec de l'appel API: " + e.getMessage(), e);
        }
    }
    
    /**
     * Version simplifiée pour les appels sans en-têtes ni paramètres.
     */
    public com.etljobs.sql2json2api.model.ApiResponse callApi(String route, HttpMethod method, String payload) {
        return callApi(route, method, payload, null, null);
    }
    
    /**
     * Effectue un appel API à partir d'un résultat de template.
     * 
     * @param templateResult Résultat du template contenant les infos d'API
     * @return Réponse de l'API au format legacy
     */
    public com.etljobs.sql2json2api.model.ApiResponse callApiFromTemplate(ApiTemplateResult templateResult) {
        if (templateResult == null || templateResult.getEndpointInfo() == null) {
            throw new IllegalArgumentException("Le résultat du template est invalide");
        }
        
        // Créer la requête depuis le résultat du template
        ApiRequest request = requestFactory.createFromTemplateResult(
                templateResult, tokenService.getToken());
        
        // Exécuter l'appel via l'executor
        ApiResponse response = apiCallExecutor.execute(request);
        
        // Convertir au format legacy
        return responseAdapter.toLegacy(response);
    }
    
    /**
     * Réessaie un appel API avec un nouveau token.
     * 
     * @param route La route/URL de l'API
     * @param method La méthode HTTP
     * @param payload Le contenu JSON
     * @param headers En-têtes HTTP additionnels
     * @param urlParams Paramètres d'URL
     * @return Réponse de l'API au format legacy
     */
    public com.etljobs.sql2json2api.model.ApiResponse retryWithNewToken(String route, HttpMethod method, String payload, 
                                      Map<String, String> headers, Map<String, Object> urlParams) {
        log.debug("Réessai de l'appel API avec un nouveau token");
        
        // Créer la requête avec le flag refreshToken
        ApiRequest request = ApiRequestBuilder.create()
                .url(route)
                .method(method)
                .payload(payload)
                .headers(headers)
                .urlParams(urlParams)
                .refreshToken()
                .build();
        
        // Exécuter l'appel
        ApiResponse response = apiCallExecutor.execute(request);
        
        // Convertir au format legacy
        return responseAdapter.toLegacy(response);
    }
    
    /**
     * Effectue un appel API avec mécanisme de réessai automatique.
     * 
     * @param route La route/URL de l'API
     * @param method La méthode HTTP
     * @param payload Le contenu JSON
     * @param headers En-têtes HTTP additionnels
     * @param urlParams Paramètres d'URL
     * @param maxRetries Nombre maximum de réessais
     * @param refreshTokenOnRetry Rafraîchir le token lors des réessais
     * @return Réponse de l'API au format legacy
     */
    public com.etljobs.sql2json2api.model.ApiResponse callApiWithRetry(String route, HttpMethod method, String payload, 
                                   Map<String, String> headers, Map<String, Object> urlParams,
                                   int maxRetries, boolean refreshTokenOnRetry) {
        
        // Créer la requête
        ApiRequest request = ApiRequestBuilder.create()
                .url(route)
                .method(method)
                .payload(payload)
                .headers(headers)
                .urlParams(urlParams)
                .build();
        
        // Préparer le callback pour le rafraîchissement du token
        Runnable retryCallback = refreshTokenOnRetry ? 
                () -> request.setAuthToken(tokenService.refreshToken()) : null;
        
        // Exécuter avec réessai
        ApiResponse response = apiCallExecutor.executeWithRetry(
                request, retryCallback, maxRetries, DEFAULT_RETRY_DELAY_MS);
        
        // Convertir au format legacy
        return responseAdapter.toLegacy(response);
    }
    
    /**
     * Version simplifiée avec valeurs par défaut pour les réessais.
     */
    public com.etljobs.sql2json2api.model.ApiResponse callApiWithRetry(String route, HttpMethod method, String payload) {
        return callApiWithRetry(route, method, payload, null, null, DEFAULT_MAX_RETRIES, true);
    }
}