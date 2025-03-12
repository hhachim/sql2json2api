package com.etljobs.sql2json2api.api.request;

import org.springframework.stereotype.Component;

import com.etljobs.sql2json2api.model.ApiTemplateResult;

/**
 * Factory pour créer des objets ApiRequest à partir d'autres sources de données.
 * Cette classe sert d'adaptateur entre les anciens modèles et les nouvelles classes de requête.
 */
@Component
public class ApiRequestFactory {
    
    /**
     * Crée une ApiRequest à partir d'un résultat de template.
     * 
     * @param templateResult Résultat du traitement du template
     * @param authToken Token d'authentification à utiliser
     * @return Une ApiRequest configurée avec les informations du templateResult
     */
    public ApiRequest createFromTemplateResult(ApiTemplateResult templateResult, String authToken) {
        // Vérification des paramètres
        if (templateResult == null || templateResult.getEndpointInfo() == null) {
            throw new IllegalArgumentException("Le résultat du template ou ses informations d'endpoint sont nulles");
        }
        
        // Utiliser le builder pour construire la requête
        return ApiRequestBuilder.create()
                .url(templateResult.getEndpointInfo().getRoute())
                .method(templateResult.getEndpointInfo().getMethod())
                .payload(templateResult.getJsonPayload())
                .authToken(authToken)
                .headers(templateResult.getEndpointInfo().getHeaders())
                .urlParams(templateResult.getEndpointInfo().getUrlParams())
                .standardJsonHeaders() // Ajout des en-têtes JSON standard
                .build();
    }
    
    /**
     * Crée une ApiRequest GET simple vers l'URL spécifiée.
     * 
     * @param url URL de la requête
     * @param authToken Token d'authentification à utiliser
     * @return Une ApiRequest GET simple
     */
    public ApiRequest createGetRequest(String url, String authToken) {
        return ApiRequestBuilder.create()
                .get(url)
                .authToken(authToken)
                .standardJsonHeaders()
                .build();
    }
    
    /**
     * Crée une ApiRequest POST avec le payload JSON spécifié.
     * 
     * @param url URL de la requête
     * @param jsonPayload Corps de la requête JSON
     * @param authToken Token d'authentification à utiliser
     * @return Une ApiRequest POST configurée
     */
    public ApiRequest createPostRequest(String url, String jsonPayload, String authToken) {
        return ApiRequestBuilder.create()
                .post(url)
                .payload(jsonPayload)
                .authToken(authToken)
                .standardJsonHeaders()
                .build();
    }
    
    /**
     * Crée une ApiRequest PUT avec le payload JSON spécifié.
     * 
     * @param url URL de la requête
     * @param jsonPayload Corps de la requête JSON
     * @param authToken Token d'authentification à utiliser
     * @return Une ApiRequest PUT configurée
     */
    public ApiRequest createPutRequest(String url, String jsonPayload, String authToken) {
        return ApiRequestBuilder.create()
                .put(url)
                .payload(jsonPayload)
                .authToken(authToken)
                .standardJsonHeaders()
                .build();
    }
    
    /**
     * Crée une ApiRequest DELETE vers l'URL spécifiée.
     * 
     * @param url URL de la requête
     * @param authToken Token d'authentification à utiliser
     * @return Une ApiRequest DELETE simple
     */
    public ApiRequest createDeleteRequest(String url, String authToken) {
        return ApiRequestBuilder.create()
                .delete(url)
                .authToken(authToken)
                .standardJsonHeaders()
                .build();
    }
    
    /**
     * Crée une copie d'une ApiRequest avec des propriétés spécifiques modifiées.
     * 
     * @param original Requête originale à copier
     * @param forceRefreshToken Indique si le token doit être rafraîchi
     * @return Une nouvelle ApiRequest basée sur l'originale
     */
    public ApiRequest createCopy(ApiRequest original, boolean forceRefreshToken) {
        if (original == null) {
            throw new IllegalArgumentException("La requête originale ne peut pas être nulle");
        }
        
        ApiRequestBuilder builder = ApiRequestBuilder.create()
                .url(original.getUrl())
                .method(original.getMethod())
                .payload(original.getPayload())
                .authToken(original.getAuthToken())
                .headers(original.getHeaders())
                .urlParams(original.getUrlParams())
                .requestId(original.getRequestId() + "-retry");
        
        if (forceRefreshToken) {
            builder.refreshToken();
        }
        
        return builder.build();
    }
}