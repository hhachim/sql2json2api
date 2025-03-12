package com.etljobs.sql2json2api.api.response;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;

import com.etljobs.sql2json2api.api.request.ApiRequest;

/**
 * Factory pour créer des instances d'ApiResponse.
 * Permet de standardiser la création des réponses API dans l'application.
 */
@Component
public class ApiResponseFactory {
    
    /**
     * Crée une ApiResponse à partir d'une ResponseEntity de Spring.
     * 
     * @param response Réponse Spring à convertir
     * @param executionTimeMs Temps d'exécution en millisecondes
     * @return Une ApiResponse correspondante
     */
    public ApiResponse fromResponseEntity(ResponseEntity<String> response, long executionTimeMs) {
        if (response == null) {
            return createErrorResponse(500, "Null response entity", executionTimeMs);
        }
        
        return ApiResponse.builder()
                .statusCode(response.getStatusCode().value())
                .body(response.getBody())
                .executionTimeMs(executionTimeMs)
                .receivedAt(Instant.now())
                .build();
    }
    
    /**
     * Crée une ApiResponse à partir d'une ResponseEntity de Spring et d'une requête.
     * 
     * @param response Réponse Spring à convertir
     * @param request Requête d'origine
     * @param executionTimeMs Temps d'exécution en millisecondes
     * @param attemptNumber Numéro de tentative
     * @return Une ApiResponse correspondante
     */
    public ApiResponse fromResponseEntity(ResponseEntity<String> response, ApiRequest request, 
            long executionTimeMs, int attemptNumber) {
        ApiResponse apiResponse = fromResponseEntity(response, executionTimeMs);
        
        if (request != null) {
            apiResponse.setRequestId(request.getRequestId());
            apiResponse.setRequestUrl(request.getUrl());
        }
        
        apiResponse.setAttemptNumber((int)attemptNumber);
        return apiResponse;
    }
    
    /**
     * Crée une ApiResponse d'erreur à partir d'une exception HTTP.
     * 
     * @param exception Exception HTTP à convertir
     * @param executionTimeMs Temps d'exécution en millisecondes
     * @return Une ApiResponse correspondante
     */
    public ApiResponse fromHttpException(HttpStatusCodeException exception, long executionTimeMs) {
        return ApiResponse.builder()
                .statusCode(exception.getStatusCode().value())
                .body(exception.getResponseBodyAsString())
                .executionTimeMs(executionTimeMs)
                .receivedAt(Instant.now())
                .build();
    }
    
    /**
     * Crée une ApiResponse d'erreur à partir d'une exception HTTP et d'une requête.
     * 
     * @param exception Exception HTTP à convertir
     * @param request Requête d'origine
     * @param executionTimeMs Temps d'exécution en millisecondes
     * @param attemptNumber Numéro de tentative
     * @return Une ApiResponse correspondante
     */
    public ApiResponse fromHttpException(HttpStatusCodeException exception, ApiRequest request, 
            long executionTimeMs, int attemptNumber) {
        ApiResponse apiResponse = fromHttpException(exception, executionTimeMs);
        
        if (request != null) {
            apiResponse.setRequestId(request.getRequestId());
            apiResponse.setRequestUrl(request.getUrl());
        }
        
        apiResponse.setAttemptNumber((int)attemptNumber);
        return apiResponse;
    }
    
    /**
     * Crée une ApiResponse d'erreur à partir d'une exception générale.
     * 
     * @param exception Exception à convertir
     * @param executionTimeMs Temps d'exécution en millisecondes
     * @return Une ApiResponse correspondante
     */
    public ApiResponse fromException(Exception exception, long executionTimeMs) {
        // Si c'est une exception HTTP, utiliser la méthode spécifique
        if (exception instanceof HttpStatusCodeException) {
            return fromHttpException((HttpStatusCodeException) exception, executionTimeMs);
        }
        
        return ApiResponse.builder()
                .statusCode(500)
                .body("{\"error\":\"" + exception.getMessage() + "\"}")
                .executionTimeMs(executionTimeMs)
                .receivedAt(Instant.now())
                .build();
    }
    
    /**
     * Crée une ApiResponse d'erreur avec un code et un message personnalisés.
     * 
     * @param statusCode Code de statut HTTP
     * @param errorMessage Message d'erreur
     * @param executionTimeMs Temps d'exécution en millisecondes
     * @return Une ApiResponse correspondante
     */
    public ApiResponse createErrorResponse(int statusCode, String errorMessage, long executionTimeMs) {
        return ApiResponse.builder()
                .statusCode(statusCode)
                .body("{\"error\":\"" + errorMessage + "\"}")
                .executionTimeMs(executionTimeMs)
                .receivedAt(Instant.now())
                .build();
    }
    
    /**
     * Crée une ApiResponse de succès avec un statut 200 OK.
     * 
     * @param body Corps de la réponse
     * @param executionTimeMs Temps d'exécution en millisecondes
     * @return Une ApiResponse correspondante
     */
    public ApiResponse createSuccessResponse(String body, long executionTimeMs) {
        return ApiResponse.builder()
                .statusCode(HttpStatus.OK.value())
                .body(body)
                .executionTimeMs(executionTimeMs)
                .receivedAt(Instant.now())
                .build();
    }
    
    /**
     * Crée une ApiResponse vide avec un statut 204 No Content.
     * 
     * @param executionTimeMs Temps d'exécution en millisecondes
     * @return Une ApiResponse correspondante
     */
    public ApiResponse createNoContentResponse(long executionTimeMs) {
        return ApiResponse.builder()
                .statusCode(HttpStatus.NO_CONTENT.value())
                .body("")
                .executionTimeMs(executionTimeMs)
                .receivedAt(Instant.now())
                .build();
    }
}