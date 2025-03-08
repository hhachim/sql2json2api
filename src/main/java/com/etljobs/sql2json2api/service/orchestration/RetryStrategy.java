package com.etljobs.sql2json2api.service.orchestration;

import java.util.List;

import org.springframework.http.HttpStatus;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Classe responsable de la stratégie de réessai pour les appels API.
 * Cette classe contient la logique et les paramètres de configuration
 * liés aux réessais des opérations qui peuvent échouer temporairement.
 */
@Slf4j
public class RetryStrategy {
    
    /**
     * Nombre maximum de tentatives de réessai
     */
    private final int maxAttempts;
    
    /**
     * Délai initial entre les tentatives (en millisecondes)
     */
    private final long initialDelayMs;
    
    /**
     * Multiplicateur pour le backoff exponentiel
     */
    private final double backoffMultiplier;
    
    /**
     * Liste des codes de statut HTTP qui méritent un réessai
     */
    private static final List<Integer> RETRYABLE_STATUS_CODES = List.of(
        HttpStatus.TOO_MANY_REQUESTS.value(),  // 429
        HttpStatus.SERVICE_UNAVAILABLE.value(), // 503
        HttpStatus.GATEWAY_TIMEOUT.value(),     // 504
        HttpStatus.INTERNAL_SERVER_ERROR.value(), // 500
        HttpStatus.BAD_GATEWAY.value(),         // 502
        HttpStatus.REQUEST_TIMEOUT.value()      // 408
    );
    
    /**
     * Constructeur avec les paramètres de réessai
     * 
     * @param maxAttempts Nombre maximum de tentatives
     * @param initialDelayMs Délai initial entre les tentatives (en ms)
     * @param backoffMultiplier Multiplicateur pour le backoff exponentiel
     */
    public RetryStrategy(int maxAttempts, long initialDelayMs, double backoffMultiplier) {
        this.maxAttempts = maxAttempts;
        this.initialDelayMs = initialDelayMs;
        this.backoffMultiplier = backoffMultiplier;
        
        log.debug("RetryStrategy initialisée avec maxAttempts={}, initialDelayMs={}, backoffMultiplier={}",
                maxAttempts, initialDelayMs, backoffMultiplier);
    }
    
    /**
     * Détermine si un code de statut HTTP mérite un réessai
     * 
     * @param statusCode Code de statut HTTP
     * @return true si le statut mérite un réessai, false sinon
     */
    public boolean isRetryableStatusCode(int statusCode) {
        return RETRYABLE_STATUS_CODES.contains(statusCode);
    }
    
    /**
     * Détermine si une exception mérite un réessai
     * 
     * @param exception L'exception à analyser
     * @return true si l'exception mérite un réessai, false sinon
     */
    public boolean isRetryableException(Exception exception) {
        if (exception == null || exception.getMessage() == null) {
            return false;
        }
        
        String message = exception.getMessage().toLowerCase();
        return message.contains("timeout") || 
               message.contains("connection") || 
               message.contains("temporary") || 
               message.contains("overloaded");
    }
    
    /**
     * Calcule le délai à attendre avant la prochaine tentative
     * 
     * @param attempt Numéro de la tentative actuelle (1-based)
     * @return Délai à attendre en millisecondes
     */
    public long calculateDelay(int attempt) {
        if (attempt <= 1) {
            return initialDelayMs;
        }
        
        // Backoff exponentiel: délai * (multiplicateur ^ (tentative - 1))
        double multiplier = Math.pow(backoffMultiplier, attempt - 2);
        return Math.round(initialDelayMs * multiplier);
    }
    
    /**
     * Vérifie si une nouvelle tentative devrait être effectuée
     * 
     * @param currentAttempt Numéro de la tentative actuelle (1-based)
     * @return true si une nouvelle tentative devrait être effectuée, false sinon
     */
    public boolean shouldRetry(int currentAttempt) {
        return currentAttempt < maxAttempts;
    }
    
    /**
     * Vérifie si une nouvelle tentative devrait être effectuée en fonction du statut HTTP
     * 
     * @param statusCode Code de statut HTTP
     * @param currentAttempt Numéro de la tentative actuelle (1-based)
     * @return true si une nouvelle tentative devrait être effectuée, false sinon
     */
    public boolean shouldRetry(int statusCode, int currentAttempt) {
        return shouldRetry(currentAttempt) && isRetryableStatusCode(statusCode);
    }
    
    /**
     * Vérifie si une nouvelle tentative devrait être effectuée en fonction de l'exception
     * 
     * @param exception L'exception survenue
     * @param currentAttempt Numéro de la tentative actuelle (1-based)
     * @return true si une nouvelle tentative devrait être effectuée, false sinon
     */
    public boolean shouldRetry(Exception exception, int currentAttempt) {
        return shouldRetry(currentAttempt) && isRetryableException(exception);
    }
    
    /**
     * Crée un nouveau contexte de réessai
     * 
     * @return Un nouveau contexte de réessai initialisé
     */
    public RetryContext createContext() {
        return RetryContext.builder()
                .currentAttempt(1)
                .totalDelayMs(0)
                .build();
    }
    
    /**
     * Effectue une pause avant la prochaine tentative
     * 
     * @param attempt Numéro de la tentative actuelle (1-based)
     */
    public void sleep(int attempt) {
        long delayMs = calculateDelay(attempt);
        try {
            log.debug("Attente de {} ms avant la tentative {}", delayMs, attempt);
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interruption pendant l'attente avant réessai");
        }
    }
    
    /**
     * Obtient le nombre maximum de tentatives configuré
     * 
     * @return Nombre maximum de tentatives
     */
    public int getMaxAttempts() {
        return maxAttempts;
    }
    
    /**
     * Classe contenant des informations sur l'état actuel d'une séquence de réessai
     */
    @Data
    @Builder
    public static class RetryContext {
        private int currentAttempt;
        private long totalDelayMs;
        private Exception lastException;
        private int lastStatusCode;
        
        /**
         * Incrémente la tentative actuelle
         */
        public void incrementAttempt() {
            currentAttempt++;
        }
        
        /**
         * Ajoute un délai au délai total
         * 
         * @param delayMs Délai à ajouter en millisecondes
         */
        public void addDelay(long delayMs) {
            totalDelayMs += delayMs;
        }
    }
}