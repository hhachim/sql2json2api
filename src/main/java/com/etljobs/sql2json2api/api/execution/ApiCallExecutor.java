package com.etljobs.sql2json2api.api.execution;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.etljobs.sql2json2api.api.request.ApiRequest;
import com.etljobs.sql2json2api.api.response.ApiResponse;
import com.etljobs.sql2json2api.api.response.ApiResponseFactory;
import com.etljobs.sql2json2api.exception.ApiCallException;
import com.etljobs.sql2json2api.service.http.TokenService;

import lombok.extern.slf4j.Slf4j;

/**
 * Classe responsable d'exécuter les appels API.
 * Cette classe se concentre uniquement sur l'exécution des appels, 
 * en séparant clairement cette responsabilité des autres aspects du service.
 */
@Component
@Slf4j
public class ApiCallExecutor {
    
    private final RestTemplate restTemplate;
    private final TokenService tokenService;
    private final ApiResponseFactory responseFactory;
    private final DefaultApiCallStrategy defaultStrategy;
    
    /**
     * Constructeur avec injection des dépendances.
     */
    @Autowired
    public ApiCallExecutor(
            RestTemplate restTemplate, 
            TokenService tokenService, 
            ApiResponseFactory responseFactory,
            DefaultApiCallStrategy defaultStrategy) {
        this.restTemplate = restTemplate;
        this.tokenService = tokenService;
        this.responseFactory = responseFactory;
        this.defaultStrategy = defaultStrategy;
    }
    
    /**
     * Exécute un appel API selon la configuration dans l'objet ApiRequest.
     * 
     * @param request La requête à exécuter
     * @return La réponse de l'API
     * @throws ApiCallException en cas d'erreur d'exécution
     */
    public ApiResponse execute(ApiRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La requête ne peut pas être nulle");
        }
        
        log.debug("Exécution de la requête API: {}", request.toLogString());
        
        // Moment de début de l'exécution
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. Préparer l'URL avec les paramètres
            String url = buildUrl(request);
            
            // 2. Préparer le token d'authentification
            String token = prepareAuthToken(request);
            
            // 3. Préparer les en-têtes HTTP
            HttpHeaders httpHeaders = prepareHeaders(request, token);
            
            // 4. Créer l'entité HTTP avec le payload et les en-têtes
            HttpEntity<String> entity = new HttpEntity<>(request.getPayload(), httpHeaders);
            
            // 5. Exécuter l'appel API
            ResponseEntity<String> response = restTemplate.exchange(
                url, request.getMethod(), entity, String.class);
            
            // 6. Calculer le temps d'exécution
            long executionTime = System.currentTimeMillis() - startTime;
            log.debug("Appel API réussi en {}ms: {} {}", executionTime, request.getMethod(), url);
            
            // 7. Créer et retourner la réponse
            return responseFactory.fromResponseEntity(response, request, executionTime, 1);
            
        } catch (HttpStatusCodeException e) {
            // Gérer les erreurs HTTP (4xx, 5xx)
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("Erreur HTTP {} lors de l'appel API: {}", e.getStatusCode(), e.getMessage());
            return responseFactory.fromHttpException(e, request, executionTime, 1);
            
        } catch (Exception e) {
            // Gérer les autres exceptions
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("Erreur lors de l'appel API: {}", e.getMessage());
            throw new ApiCallException("Échec de l'exécution de la requête API: " + e.getMessage(), e);
        }
    }
    
    /**
     * Construit l'URL complète avec les paramètres.
     * 
     * @param request La requête contenant l'URL et les paramètres
     * @return L'URL complète
     */
    private String buildUrl(ApiRequest request) {
        if (request.getUrlParams() == null || request.getUrlParams().isEmpty()) {
            return request.getUrl();
        }
        
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(request.getUrl());
        request.getUrlParams().forEach((key, value) -> {
            if (value != null) {
                builder.queryParam(key, value);
            }
        });
        
        return builder.build().toUriString();
    }
    
    /**
     * Prépare le token d'authentification.
     * 
     * @param request La requête contenant les informations d'authentification
     * @return Le token d'authentification
     */
    private String prepareAuthToken(ApiRequest request) {
        // Si la requête demande un rafraîchissement du token
        if (request.isRefreshToken()) {
            return tokenService.refreshToken();
        }
        
        // Si la requête a déjà un token, l'utiliser
        if (request.getAuthToken() != null && !request.getAuthToken().isEmpty()) {
            return request.getAuthToken();
        }
        
        // Sinon, obtenir un nouveau token
        return tokenService.getToken();
    }
    
    /**
     * Prépare les en-têtes HTTP pour la requête.
     * 
     * @param request La requête contenant les en-têtes
     * @param token Le token d'authentification
     * @return Les en-têtes HTTP
     */
    private HttpHeaders prepareHeaders(ApiRequest request, String token) {
        HttpHeaders httpHeaders = new HttpHeaders();
        
        // Ajouter le token d'authentification
        if (token != null && !token.isEmpty()) {
            httpHeaders.set("Authorization", token);
        }
        
        // Ajouter les en-têtes personnalisés
        if (request.getHeaders() != null && !request.getHeaders().isEmpty()) {
            request.getHeaders().forEach(httpHeaders::set);
        }
        
        return httpHeaders;
    }
    
    /**
     * Exécute un appel API avec réessai en cas d'échec, en utilisant la stratégie par défaut.
     * 
     * @param request La requête à exécuter
     * @param retryCallback Callback à appeler avant chaque réessai
     * @param maxRetries Nombre maximum de réessais
     * @param retryDelayMs Délai entre les réessais en millisecondes
     * @return La réponse de l'API
     */
    public ApiResponse executeWithRetry(ApiRequest request, Runnable retryCallback, 
                                    int maxRetries, long retryDelayMs) {
        return executeWithStrategy(request, defaultStrategy, retryCallback, maxRetries, retryDelayMs);
    }
    
    /**
     * Exécute un appel API avec réessai en cas d'échec, en utilisant une stratégie spécifique.
     * 
     * @param request La requête à exécuter
     * @param strategy La stratégie d'appel à utiliser
     * @param retryCallback Callback à appeler avant chaque réessai
     * @param maxRetries Nombre maximum de réessais
     * @param retryDelayMs Délai entre les réessais en millisecondes
     * @return La réponse de l'API
     */
    public ApiResponse executeWithStrategy(ApiRequest request, ApiCallStrategy strategy,
                                      Runnable retryCallback, int maxRetries, long retryDelayMs) {
        ApiResponse response = null;
        ApiRequest currentRequest = request;
        int attempts = 0;
        
        do {
            attempts++;
            if (attempts > 1) {
                log.info("Tentative {} sur {}", attempts, maxRetries + 1);
                
                // Préparer la requête pour le réessai selon la stratégie
                currentRequest = strategy.prepareForRetry(request, response, attempts);
                
                // Exécuter le callback si fourni
                if (retryCallback != null) {
                    retryCallback.run();
                }
                
                // Attendre avant le réessai
                try {
                    Thread.sleep(retryDelayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new ApiCallException("Interruption pendant l'attente avant réessai", e);
                }
            }
            
            // Exécuter la requête
            response = execute(currentRequest);
            
        } while (attempts < maxRetries && strategy.shouldRetry(response));
        
        if (response.isSuccess()) {
            if (attempts > 1) {
                log.info("Succès après {} tentatives", attempts);
            }
        } else {
            log.warn("Échec après {} tentatives", attempts);
        }
        
        return response;
    }
}