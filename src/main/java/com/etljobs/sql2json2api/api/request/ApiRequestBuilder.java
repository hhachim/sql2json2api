package com.etljobs.sql2json2api.api.request;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpMethod;

import com.etljobs.sql2json2api.model.ApiEndpointInfo;

/**
 * Builder spécialisé pour construire des objets ApiRequest de manière fluide.
 * Cette classe offre une API plus intuitive et extensible que le Builder généré par Lombok.
 */
public class ApiRequestBuilder {
    
    private String url;
    private HttpMethod method;
    private String payload;
    private String authToken;
    private Map<String, String> headers = new HashMap<>();
    private Map<String, Object> urlParams = new HashMap<>();
    private String requestId = UUID.randomUUID().toString().substring(0, 8);
    private boolean refreshToken = false;
    private int timeoutMs = -1;
    
    /**
     * Crée un nouveau builder avec des valeurs par défaut.
     * 
     * @return Une nouvelle instance de ApiRequestBuilder
     */
    public static ApiRequestBuilder create() {
        return new ApiRequestBuilder();
    }
    
    /**
     * Crée un builder à partir d'informations d'API existantes.
     * 
     * @param endpointInfo Informations d'API à utiliser comme base
     * @return Un ApiRequestBuilder pré-rempli avec les informations d'API
     */
    public static ApiRequestBuilder fromEndpointInfo(ApiEndpointInfo endpointInfo) {
        ApiRequestBuilder builder = new ApiRequestBuilder();
        
        if (endpointInfo != null) {
            builder.url(endpointInfo.getRoute())
                   .method(endpointInfo.getMethod());
            
            // Ajouter les en-têtes s'ils existent
            if (endpointInfo.getHeaders() != null) {
                endpointInfo.getHeaders().forEach(builder::header);
            }
            
            // Ajouter les paramètres d'URL s'ils existent
            if (endpointInfo.getUrlParams() != null) {
                endpointInfo.getUrlParams().forEach(builder::urlParam);
            }
        }
        
        return builder;
    }
    
    /**
     * Définit l'URL de la requête API.
     * 
     * @param url URL à utiliser
     * @return Ce builder pour chaînage
     */
    public ApiRequestBuilder url(String url) {
        this.url = url;
        return this;
    }
    
    /**
     * Définit la méthode HTTP de la requête.
     * 
     * @param method Méthode HTTP à utiliser
     * @return Ce builder pour chaînage
     */
    public ApiRequestBuilder method(HttpMethod method) {
        this.method = method;
        return this;
    }
    
    /**
     * Définit le corps de la requête.
     * 
     * @param payload Corps de la requête (généralement JSON)
     * @return Ce builder pour chaînage
     */
    public ApiRequestBuilder payload(String payload) {
        this.payload = payload;
        return this;
    }
    
    /**
     * Définit le token d'authentification.
     * 
     * @param token Token d'authentification (format Bearer token)
     * @return Ce builder pour chaînage
     */
    public ApiRequestBuilder authToken(String token) {
        this.authToken = token;
        return this;
    }
    
    /**
     * Ajoute un en-tête HTTP à la requête.
     * 
     * @param name Nom de l'en-tête
     * @param value Valeur de l'en-tête
     * @return Ce builder pour chaînage
     */
    public ApiRequestBuilder header(String name, String value) {
        this.headers.put(name, value);
        return this;
    }
    
    /**
     * Ajoute tous les en-têtes HTTP donnés à la requête.
     * 
     * @param headers Map d'en-têtes à ajouter
     * @return Ce builder pour chaînage
     */
    public ApiRequestBuilder headers(Map<String, String> headers) {
        if (headers != null) {
            this.headers.putAll(headers);
        }
        return this;
    }
    
    /**
     * Ajoute un paramètre d'URL à la requête.
     * 
     * @param name Nom du paramètre
     * @param value Valeur du paramètre
     * @return Ce builder pour chaînage
     */
    public ApiRequestBuilder urlParam(String name, Object value) {
        this.urlParams.put(name, value);
        return this;
    }
    
    /**
     * Ajoute tous les paramètres d'URL donnés à la requête.
     * 
     * @param params Map de paramètres à ajouter
     * @return Ce builder pour chaînage
     */
    public ApiRequestBuilder urlParams(Map<String, Object> params) {
        if (params != null) {
            this.urlParams.putAll(params);
        }
        return this;
    }
    
    /**
     * Définit un identifiant personnalisé pour la requête.
     * 
     * @param requestId Identifiant de requête pour suivi
     * @return Ce builder pour chaînage
     */
    public ApiRequestBuilder requestId(String requestId) {
        this.requestId = requestId;
        return this;
    }
    
    /**
     * Indique qu'un nouveau token doit être généré pour cette requête.
     * 
     * @return Ce builder pour chaînage
     */
    public ApiRequestBuilder refreshToken() {
        this.refreshToken = true;
        return this;
    }
    
    /**
     * Définit un timeout personnalisé pour cette requête.
     * 
     * @param timeoutMs Timeout en millisecondes
     * @return Ce builder pour chaînage
     */
    public ApiRequestBuilder timeout(int timeoutMs) {
        this.timeoutMs = timeoutMs;
        return this;
    }
    
    /**
     * Construit une requête GET.
     * 
     * @param url URL de la requête
     * @return Ce builder pour chaînage
     */
    public ApiRequestBuilder get(String url) {
        this.url = url;
        this.method = HttpMethod.GET;
        return this;
    }
    
    /**
     * Construit une requête POST.
     * 
     * @param url URL de la requête
     * @return Ce builder pour chaînage
     */
    public ApiRequestBuilder post(String url) {
        this.url = url;
        this.method = HttpMethod.POST;
        return this;
    }
    
    /**
     * Construit une requête PUT.
     * 
     * @param url URL de la requête
     * @return Ce builder pour chaînage
     */
    public ApiRequestBuilder put(String url) {
        this.url = url;
        this.method = HttpMethod.PUT;
        return this;
    }
    
    /**
     * Construit une requête DELETE.
     * 
     * @param url URL de la requête
     * @return Ce builder pour chaînage
     */
    public ApiRequestBuilder delete(String url) {
        this.url = url;
        this.method = HttpMethod.DELETE;
        return this;
    }
    
    /**
     * Ajoute l'en-tête Content-Type JSON.
     * 
     * @return Ce builder pour chaînage
     */
    public ApiRequestBuilder jsonContentType() {
        return this.header("Content-Type", "application/json");
    }
    
    /**
     * Ajoute les en-têtes standard pour une requête JSON.
     * 
     * @return Ce builder pour chaînage
     */
    public ApiRequestBuilder standardJsonHeaders() {
        return this.header("Content-Type", "application/json")
                   .header("Accept", "application/json");
    }
    
    /**
     * Construit l'objet ApiRequest final.
     * 
     * @return Un objet ApiRequest configuré avec tous les paramètres définis
     * @throws IllegalStateException si l'URL ou la méthode HTTP est manquante
     */
    public ApiRequest build() {
        // Vérifier les paramètres obligatoires
        if (url == null || url.isEmpty()) {
            throw new IllegalStateException("L'URL est obligatoire pour une requête API");
        }
        
        if (method == null) {
            throw new IllegalStateException("La méthode HTTP est obligatoire pour une requête API");
        }
        
        // Construire l'objet ApiRequest
        return ApiRequest.builder()
                .url(url)
                .method(method)
                .payload(payload)
                .authToken(authToken)
                .headers(new HashMap<>(headers))
                .urlParams(new HashMap<>(urlParams))
                .requestId(requestId)
                .refreshToken(refreshToken)
                .timeoutMs(timeoutMs)
                .build();
    }
}