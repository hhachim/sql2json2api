package com.etljobs.sql2json2api.api.execution;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.etljobs.sql2json2api.api.request.ApiRequest;
import com.etljobs.sql2json2api.api.request.ApiRequestBuilder;
import com.etljobs.sql2json2api.api.response.ApiResponse;
import com.etljobs.sql2json2api.service.http.TokenService;

import lombok.extern.slf4j.Slf4j;

/**
 * Implémentation par défaut de la stratégie d'appel API.
 * Gère les cas classiques de réessai et de rafraîchissement de token.
 */
@Component
@Slf4j
public class DefaultApiCallStrategy implements ApiCallStrategy {

    private final TokenService tokenService;
    
    // Liste des codes HTTP qui justifient un réessai
    private static final List<Integer> RETRY_STATUS_CODES = List.of(408, 429, 500, 502, 503, 504);
    
    @Autowired
    public DefaultApiCallStrategy(TokenService tokenService) {
        this.tokenService = tokenService;
    }
    
    @Override
    public ApiResponse execute(ApiRequest request) {
        // Cette méthode est laissée vide car l'exécution réelle
        // est déléguée à ApiCallExecutor. Cette classe ne gère que
        // la logique de stratégie (quand et comment réessayer).
        throw new UnsupportedOperationException(
                "Cette méthode ne doit pas être appelée directement. Utilisez ApiCallExecutor.");
    }

    @Override
    public boolean shouldRetry(ApiResponse response) {
        // Vérifier si la réponse a un code qui justifie un réessai
        if (response == null) {
            return false;
        }
        
        // Réessayer pour les erreurs temporaires
        if (RETRY_STATUS_CODES.contains(response.getStatusCode())) {
            log.debug("Réponse avec statut {} justifie un réessai", response.getStatusCode());
            return true;
        }
        
        // Réessayer pour les erreurs d'authentification (avec refresh token)
        if (response.isAuthError()) {
            log.debug("Erreur d'authentification ({}), un réessai avec nouveau token est nécessaire", 
                    response.getStatusCode());
            return true;
        }
        
        return false;
    }

    @Override
    public ApiRequest prepareForRetry(ApiRequest originalRequest, ApiResponse previousResponse, int attemptNumber) {
        if (originalRequest == null) {
            throw new IllegalArgumentException("La requête originale ne peut pas être nulle");
        }
        
        log.debug("Préparation du réessai {} pour {}", attemptNumber, originalRequest.getUrl());
        
        // Builder pour la nouvelle requête
        ApiRequestBuilder builder = ApiRequestBuilder.create()
                .url(originalRequest.getUrl())
                .method(originalRequest.getMethod())
                .payload(originalRequest.getPayload())
                .headers(originalRequest.getHeaders())
                .urlParams(originalRequest.getUrlParams())
                .requestId(originalRequest.getRequestId() + "-retry" + attemptNumber)
                .timeout(calculateTimeoutForRetry(originalRequest.getTimeoutMs(), attemptNumber));
        
        // Si l'erreur précédente était une erreur d'authentification, rafraîchir le token
        if (previousResponse != null && previousResponse.isAuthError()) {
            log.debug("Rafraîchissement du token pour le réessai");
            String newToken = tokenService.refreshToken();
            builder.authToken(newToken);
        } else {
            // Sinon, conserver le token original
            builder.authToken(originalRequest.getAuthToken());
        }
        
        return builder.build();
    }
    
    /**
     * Calcule un timeout adapté au numéro de tentative.
     * Utilise un backoff exponentiel pour augmenter le timeout à chaque réessai.
     * 
     * @param originalTimeout Le timeout original
     * @param attemptNumber Le numéro de la tentative
     * @return Le nouveau timeout
     */
    private int calculateTimeoutForRetry(int originalTimeout, int attemptNumber) {
        // Si le timeout original n'est pas défini, utiliser une valeur par défaut
        if (originalTimeout <= 0) {
            originalTimeout = 10000; // 10 secondes par défaut
        }
        
        // Augmenter de 50% à chaque tentative
        return (int) (originalTimeout * Math.pow(1.5, attemptNumber - 1));
    }
}