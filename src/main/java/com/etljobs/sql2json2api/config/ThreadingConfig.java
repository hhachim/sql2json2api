package com.etljobs.sql2json2api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Configuration pour les fonctionnalités de multithreading.
 * Ces paramètres contrôlent le comportement du pool de threads
 * utilisé pour les appels API parallèles.
 */
@Configuration
@ConfigurationProperties(prefix = "app.threading")
@Getter
@Setter
@Slf4j
public class ThreadingConfig {
    
    /**
     * Indique si le multithreading est activé pour les appels API.
     */
    private boolean enabled = false;
    
    /**
     * Nombre de threads dans le pool.
     * Par défaut, utilise le nombre de processeurs disponibles.
     */
    private int poolSize = Runtime.getRuntime().availableProcessors();
    
    /**
     * Capacité de la file d'attente des tâches.
     */
    private int queueCapacity = 100;
    
    /**
     * Délai d'attente en secondes avant de considérer
     * qu'une tâche a expiré.
     */
    private int timeoutSeconds = 60;
    
    /**
     * Délai en millisecondes entre les soumissions de tâches pour éviter
     * de surcharger les API cibles (throttling).
     */
    private long submissionDelayMs = 50;
}