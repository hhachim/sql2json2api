package com.etljobs.sql2json2api.service.threading;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import com.etljobs.sql2json2api.api.request.ApiRequest;
import com.etljobs.sql2json2api.model.ApiTemplateResult;
import com.etljobs.sql2json2api.service.http.ApiClientService;
import com.etljobs.sql2json2api.service.http.TokenService;

/**
 * Fabrique pour créer des tâches d'appel API.
 * Cette classe simplifie la création des tâches en fournissant
 * des méthodes utilitaires pour les différents cas d'utilisation.
 */
@Component
public class ApiCallTaskFactory {
    
    private final ApiClientService apiClientService;
    private final TokenService tokenService;
    
    @Autowired
    public ApiCallTaskFactory(ApiClientService apiClientService, TokenService tokenService) {
        this.apiClientService = apiClientService;
        this.tokenService = tokenService;
    }
    
    /**
     * Crée une tâche d'appel API à partir d'un résultat de template.
     * 
     * @param templateResult Résultat du traitement du template
     * @param rowIndex Index de la ligne de données
     * @param rowIdentifier Identifiant de la ligne pour le suivi
     * @return Tâche d'appel API
     */
    public ApiCallTask createFromTemplateResult(
            ApiTemplateResult templateResult, 
            int rowIndex, 
            String rowIdentifier) {
        
        return new ApiClientServiceCallTask(
                templateResult.getEndpointInfo().getRoute(),
                templateResult.getEndpointInfo().getMethod(),
                templateResult.getJsonPayload(),
                templateResult.getEndpointInfo().getHeaders(),
                templateResult.getEndpointInfo().getUrlParams(),
                rowIndex,
                rowIdentifier,
                apiClientService,
                tokenService.getToken()
        );
    }
    
    /**
     * Crée une tâche d'appel API à partir de paramètres détaillés.
     * 
     * @param url URL de l'appel API
     * @param method Méthode HTTP
     * @param payload Corps de la requête
     * @param headers En-têtes HTTP
     * @param urlParams Paramètres d'URL
     * @param rowIndex Index de la ligne de données
     * @param rowIdentifier Identifiant de la ligne pour le suivi
     * @return Tâche d'appel API
     */
    public ApiCallTask create(
            String url,
            HttpMethod method,
            String payload,
            Map<String, String> headers,
            Map<String, Object> urlParams,
            int rowIndex,
            String rowIdentifier) {
        
        return new ApiClientServiceCallTask(
                url,
                method,
                payload,
                headers,
                urlParams,
                rowIndex,
                rowIdentifier,
                apiClientService,
                tokenService.getToken()
        );
    }
    
    /**
     * Crée une tâche d'appel API à partir d'un objet ApiRequest.
     * 
     * @param request Requête API
     * @param rowIndex Index de la ligne de données
     * @param rowIdentifier Identifiant de la ligne pour le suivi
     * @return Tâche d'appel API
     */
    public ApiCallTask createFromRequest(
            ApiRequest request,
            int rowIndex,
            String rowIdentifier) {
        
        return new ApiClientServiceCallTask(
                request.getUrl(),
                request.getMethod(),
                request.getPayload(),
                request.getHeaders(),
                request.getUrlParams(),
                rowIndex,
                rowIdentifier,
                apiClientService,
                request.getAuthToken() != null ? 
                        request.getAuthToken() : tokenService.getToken()
        );
    }
}