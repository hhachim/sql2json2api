package com.etljobs.sql2json2api.api.request;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpMethod;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Classe qui encapsule toutes les informations nécessaires pour faire une requête API.
 * Cette classe regroupe les données de route, méthode HTTP, en-têtes, paramètres et corps de la requête.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiRequest {
    
    /**
     * URL de la requête API
     */
    private String url;
    
    /**
     * Méthode HTTP (GET, POST, PUT, DELETE, etc.)
     */
    private HttpMethod method;
    
    /**
     * Corps de la requête (généralement JSON)
     */
    private String payload;
    
    /**
     * Token d'authentification (format Bearer token)
     */
    private String authToken;
    
    /**
     * En-têtes HTTP additionnels
     */
    @Builder.Default
    private Map<String, String> headers = new HashMap<>();
    
    /**
     * Paramètres d'URL pour les requêtes GET
     */
    @Builder.Default
    private Map<String, Object> urlParams = new HashMap<>();
    
    /**
     * Identificateur de la requête pour le suivi et les logs
     */
    private String requestId;
    
    /**
     * Indique si un nouveau token doit être généré pour cette requête
     */
    @Builder.Default
    private boolean refreshToken = false;
    
    /**
     * Timeout personnalisé pour cette requête (en millisecondes)
     * Valeur de -1 indique d'utiliser le timeout par défaut
     */
    @Builder.Default
    private int timeoutMs = -1;
    
    /**
     * Vérifie si la requête a une URL et une méthode valides
     * 
     * @return true si la requête est valide pour exécution
     */
    public boolean isValid() {
        return url != null && !url.isEmpty() && method != null;
    }
    
    /**
     * Ajoute un en-tête à la requête
     * 
     * @param name Nom de l'en-tête
     * @param value Valeur de l'en-tête
     * @return Cette instance de ApiRequest pour chaînage
     */
    public ApiRequest addHeader(String name, String value) {
        if (headers == null) {
            headers = new HashMap<>();
        }
        headers.put(name, value);
        return this;
    }
    
    /**
     * Ajoute un paramètre d'URL à la requête
     * 
     * @param name Nom du paramètre
     * @param value Valeur du paramètre
     * @return Cette instance de ApiRequest pour chaînage
     */
    public ApiRequest addUrlParam(String name, Object value) {
        if (urlParams == null) {
            urlParams = new HashMap<>();
        }
        urlParams.put(name, value);
        return this;
    }
    
    /**
     * Génère une représentation textuelle de la requête pour les logs
     * 
     * @return Représentation de la requête pour logs
     */
    public String toLogString() {
        StringBuilder sb = new StringBuilder();
        sb.append(method).append(" ").append(url);
        
        if (requestId != null) {
            sb.append(" [ID: ").append(requestId).append("]");
        }
        
        if (urlParams != null && !urlParams.isEmpty()) {
            sb.append(" Params: ").append(urlParams);
        }
        
        return sb.toString();
    }
}