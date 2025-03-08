package com.etljobs.sql2json2api.api.execution;

import com.etljobs.sql2json2api.api.request.ApiRequest;
import com.etljobs.sql2json2api.api.response.ApiResponse;

/**
 * Interface définissant une stratégie d'exécution des appels API.
 * Permet de mettre en œuvre différentes stratégies d'appel selon les besoins.
 */
public interface ApiCallStrategy {
    
    /**
     * Exécute un appel API selon la stratégie implémentée.
     * 
     * @param request La requête à exécuter
     * @return La réponse de l'API
     */
    ApiResponse execute(ApiRequest request);
    
    /**
     * Vérifie si une réponse nécessite un réessai selon la stratégie.
     * 
     * @param response La réponse à évaluer
     * @return true si un réessai est nécessaire, false sinon
     */
    boolean shouldRetry(ApiResponse response);
    
    /**
     * Prépare une requête pour un réessai.
     * 
     * @param originalRequest La requête originale
     * @param previousResponse La réponse précédente
     * @param attemptNumber Le numéro de la tentative actuelle
     * @return Une nouvelle requête préparée pour le réessai
     */
    ApiRequest prepareForRetry(ApiRequest originalRequest, ApiResponse previousResponse, int attemptNumber);
}