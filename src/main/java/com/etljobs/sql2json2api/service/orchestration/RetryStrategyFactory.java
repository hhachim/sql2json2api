package com.etljobs.sql2json2api.service.orchestration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Factory pour créer des instances de RetryStrategy
 * basées sur la configuration de l'application.
 */
@Component
public class RetryStrategyFactory {
    
    @Value("${app.retry.max-attempts:3}")
    private int maxAttempts;
    
    @Value("${app.retry.delay-ms:2000}")
    private long initialDelayMs;
    
    @Value("${app.retry.backoff-multiplier:1.5}")
    private double backoffMultiplier;
    
    /**
     * Crée une instance de RetryStrategy avec les paramètres
     * de configuration de l'application.
     * 
     * @return Une nouvelle instance de RetryStrategy
     */
    public RetryStrategy create() {
        return new RetryStrategy(maxAttempts, initialDelayMs, backoffMultiplier);
    }
    
    /**
     * Crée une instance de RetryStrategy avec des paramètres personnalisés.
     * 
     * @param maxAttempts Nombre maximum de tentatives
     * @param initialDelayMs Délai initial entre les tentatives
     * @param backoffMultiplier Multiplicateur pour le backoff exponentiel
     * @return Une nouvelle instance de RetryStrategy
     */
    public RetryStrategy create(int maxAttempts, long initialDelayMs, double backoffMultiplier) {
        return new RetryStrategy(maxAttempts, initialDelayMs, backoffMultiplier);
    }
}