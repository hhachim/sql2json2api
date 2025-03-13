package com.etljobs.sql2json2api.service.threading;

import java.util.Map;
import java.util.concurrent.Callable;

import org.springframework.http.HttpMethod;

import com.etljobs.sql2json2api.api.response.ApiResponse;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Représente une tâche d'appel API qui peut être exécutée par un pool de threads.
 * Cette classe abstraite sert de base pour les implémentations spécifiques.
 */
@Slf4j
public abstract class ApiCallTask implements Callable<ApiResponse> {
    
    @Getter
    protected final String url;
    
    @Getter
    protected final HttpMethod method;
    
    @Getter
    protected final String payload;
    
    @Getter
    protected final Map<String, String> headers;
    
    @Getter
    protected final Map<String, Object> urlParams;
    
    @Getter
    protected final int rowIndex;
    
    @Getter
    protected final String rowIdentifier;
    
    /**
     * Constructeur avec tous les paramètres nécessaires pour un appel API.
     */
    protected ApiCallTask(String url, HttpMethod method, String payload, 
                        Map<String, String> headers, Map<String, Object> urlParams,
                        int rowIndex, String rowIdentifier) {
        this.url = url;
        this.method = method;
        this.payload = payload;
        this.headers = headers;
        this.urlParams = urlParams;
        this.rowIndex = rowIndex;
        this.rowIdentifier = rowIdentifier;
    }
    
    @Override
    public ApiResponse call() throws Exception {
        log.debug("Exécution de l'appel API {} {} pour la ligne {}", 
                method, url, rowIdentifier);
        
        try {
            long startTime = System.currentTimeMillis();
            ApiResponse response = executeApiCall();
            long duration = System.currentTimeMillis() - startTime;
            
            log.debug("Appel API {} {} pour la ligne {} terminé en {}ms avec statut {}",
                    method, url, rowIdentifier, duration, response.getStatusCode());
            
            return response;
        } catch (Exception e) {
            log.error("Erreur lors de l'appel API {} {} pour la ligne {}: {}", 
                    method, url, rowIdentifier, e.getMessage());
            throw e;
        }
    }
    
    /**
     * Méthode abstraite à implémenter par les sous-classes pour exécuter
     * l'appel API réel.
     * 
     * @return Réponse de l'API
     * @throws Exception si l'appel échoue
     */
    protected abstract ApiResponse executeApiCall() throws Exception;
}