package com.etljobs.sql2json2api.api.response;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Gestionnaire de réponses API qui facilite le traitement des réponses
 * selon leur statut et permet d'appliquer des actions conditionnelles.
 */
@Component
@Slf4j
public class ApiResponseHandler {
    
    /**
     * Traite une réponse API en fonction de son statut.
     * 
     * @param response La réponse API à traiter
     * @param onSuccess Action à exécuter en cas de succès
     * @param onError Action à exécuter en cas d'erreur
     */
    public void handle(ApiResponse response, Consumer<ApiResponse> onSuccess, Consumer<ApiResponse> onError) {
        if (response == null) {
            log.warn("Tentative de traitement d'une réponse null");
            return;
        }
        
        if (response.isSuccess()) {
            if (onSuccess != null) {
                onSuccess.accept(response);
            }
        } else {
            if (onError != null) {
                onError.accept(response);
            }
        }
    }
    
    /**
     * Traite une réponse API avec des actions spécifiques selon le type d'erreur.
     * 
     * @param response La réponse API à traiter
     * @param onSuccess Action à exécuter en cas de succès
     * @param onClientError Action à exécuter en cas d'erreur client (4xx)
     * @param onServerError Action à exécuter en cas d'erreur serveur (5xx)
     * @param onAuthError Action à exécuter en cas d'erreur d'authentification (401/403)
     */
    public void handleWithErrorTypes(ApiResponse response, 
            Consumer<ApiResponse> onSuccess,
            Consumer<ApiResponse> onClientError,
            Consumer<ApiResponse> onServerError,
            Consumer<ApiResponse> onAuthError) {
        
        if (response == null) {
            log.warn("Tentative de traitement d'une réponse null");
            return;
        }
        
        if (response.isSuccess()) {
            if (onSuccess != null) {
                onSuccess.accept(response);
            }
        } else if (response.isAuthError() && onAuthError != null) {
            onAuthError.accept(response);
        } else if (response.isClientError() && onClientError != null) {
            onClientError.accept(response);
        } else if (response.isServerError() && onServerError != null) {
            onServerError.accept(response);
        }
    }
    
    /**
     * Transforme une réponse API en un résultat typé en fonction de son statut.
     * 
     * @param <T> Type du résultat
     * @param response La réponse API à transformer
     * @param successTransformer Fonction à appliquer en cas de succès
     * @param errorTransformer Fonction à appliquer en cas d'erreur
     * @return Résultat de la transformation
     */
    public <T> T transform(ApiResponse response, 
            Function<ApiResponse, T> successTransformer,
            Function<ApiResponse, T> errorTransformer) {
        
        if (response == null) {
            log.warn("Tentative de transformation d'une réponse null");
            return null;
        }
        
        if (response.isSuccess()) {
            return successTransformer.apply(response);
        } else {
            return errorTransformer.apply(response);
        }
    }
    
    /**
     * Extrait une valeur typée d'une réponse API en cas de succès.
     * 
     * @param <T> Type de la valeur à extraire
     * @param response La réponse API
     * @param extractor Fonction d'extraction à appliquer en cas de succès
     * @return Optional contenant la valeur extraite, ou vide en cas d'erreur
     */
    public <T> Optional<T> extract(ApiResponse response, Function<ApiResponseParser, Optional<T>> extractor) {
        if (response == null || !response.isSuccess()) {
            return Optional.empty();
        }
        
        try {
            ApiResponseParser parser = response.parser();
            return extractor.apply(parser);
        } catch (Exception e) {
            log.warn("Erreur lors de l'extraction de données de la réponse: {}", e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Vérifie si une réponse API contient une valeur à un chemin spécifique.
     * 
     * @param response La réponse API
     * @param path Chemin à vérifier
     * @return true si le chemin existe et que la réponse est un succès, false sinon
     */
    public boolean hasValue(ApiResponse response, String path) {
        if (response == null || !response.isSuccess()) {
            return false;
        }
        
        try {
            return response.parser().hasPath(path);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Extrait un message d'erreur standardisé d'une réponse.
     * 
     * @param response La réponse API
     * @return Message d'erreur, ou description du statut HTTP si non trouvé
     */
    public String extractErrorMessage(ApiResponse response) {
        if (response == null) {
            return "Réponse non disponible";
        }
        
        if (response.isSuccess()) {
            return "Aucune erreur (statut " + response.getStatusCode() + ")";
        }
        
        try {
            Optional<String> errorMessage = response.parser().getStandardErrorMessage();
            return errorMessage.orElse("Erreur HTTP " + response.getStatusCode());
        } catch (Exception e) {
            return "Erreur HTTP " + response.getStatusCode();
        }
    }
}