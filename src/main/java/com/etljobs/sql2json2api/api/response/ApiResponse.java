package com.etljobs.sql2json2api.api.response;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Représente une réponse d'une API externe.
 * Cette classe enrichit l'ancienne implémentation avec des fonctionnalités
 * supplémentaires pour l'analyse et la manipulation des réponses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse {
    
    /**
     * Code de statut HTTP
     */
    private int statusCode;
    
    /**
     * Corps de la réponse (généralement JSON)
     */
    private String body;
    
    /**
     * Horodatage de réception de la réponse
     */
    @Builder.Default
    private Instant receivedAt = Instant.now();
    
    /**
     * Temps d'exécution de la requête en millisecondes
     */
    private long executionTimeMs;
    
    /**
     * Identifiant de la requête associée
     */
    private String requestId;
    
    /**
     * URL de la requête d'origine
     */
    private String requestUrl;
    
    /**
     * Nombre de tentatives effectuées pour obtenir cette réponse
     */
    @Builder.Default
    private int attemptNumber = 1;
    
    // Référence au parser pour cette réponse, créé à la demande
    private transient ApiResponseParser parser;
    
    /**
     * Obtient un parser pour analyser le contenu de cette réponse.
     * Le parser est créé à la demande et mis en cache.
     * 
     * @return Un ApiResponseParser pour cette réponse
     */
    public ApiResponseParser parser() {
        if (parser == null) {
            parser = new ApiResponseParser(this);
        }
        return parser;
    }
    
    /**
     * Indique si la réponse est un succès (code 2xx)
     * 
     * @return true si le statut indique un succès, false sinon
     */
    public boolean isSuccess() {
        return statusCode >= 200 && statusCode < 300;
    }
    
    /**
     * Indique si la réponse est une erreur client (code 4xx)
     * 
     * @return true si le statut indique une erreur client, false sinon
     */
    public boolean isClientError() {
        return statusCode >= 400 && statusCode < 500;
    }
    
    /**
     * Indique si la réponse est une erreur serveur (code 5xx)
     * 
     * @return true si le statut indique une erreur serveur, false sinon
     */
    public boolean isServerError() {
        return statusCode >= 500 && statusCode < 600;
    }
    
    /**
     * Indique si la réponse est une erreur d'authentification ou d'autorisation
     * 
     * @return true si le statut indique une erreur d'auth, false sinon
     */
    public boolean isAuthError() {
        return statusCode == 401 || statusCode == 403;
    }
    
    /**
     * Indique si le corps de la réponse est vide
     * 
     * @return true si le corps est null ou vide, false sinon
     */
    public boolean hasEmptyBody() {
        return body == null || body.trim().isEmpty();
    }
    
    /**
     * Crée une version simple de la réponse pour les logs
     * 
     * @return Chaîne de caractères résumant la réponse
     */
    public String toLogString() {
        return String.format("ApiResponse[status=%d, success=%b, length=%d, time=%dms]", 
                statusCode, isSuccess(), 
                (body != null) ? body.length() : 0, 
                executionTimeMs);
    }
}