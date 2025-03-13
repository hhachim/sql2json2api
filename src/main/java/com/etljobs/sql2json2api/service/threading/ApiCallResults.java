package com.etljobs.sql2json2api.service.threading;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.stream.Collectors;

import com.etljobs.sql2json2api.api.response.ApiResponse;
import com.etljobs.sql2json2api.model.RowError;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Gestionnaire des résultats d'appels API.
 * Cette classe collecte et analyse les résultats des appels API,
 * et fournit des statistiques et des fonctionnalités de suivi.
 */
@Slf4j
public class ApiCallResults {
    
    @Getter
    private final List<ApiResponse> responses = Collections.synchronizedList(new ArrayList<>());
    
    @Getter
    private final List<RowError> errors = Collections.synchronizedList(new ArrayList<>());
    
    private final Instant startTime = Instant.now();
    
    @Getter
    private Instant endTime;
    
    /**
     * Ajoute une réponse API au gestionnaire.
     * 
     * @param response Réponse API à ajouter
     */
    public void addResponse(ApiResponse response) {
        if (response != null) {
            synchronized(responses) {
                responses.add(response);
            }
        }
    }
    
    /**
     * Ajoute une erreur de ligne au gestionnaire.
     * 
     * @param error Erreur de ligne à ajouter
     */
    public void addError(RowError error) {
        if (error != null) {
            synchronized(errors) {
                errors.add(error);
            }
        }
    }
    
    /**
     * Marque le traitement comme terminé et enregistre l'heure de fin.
     */
    public void markComplete() {
        this.endTime = Instant.now();
    }
    
    /**
     * Obtient la durée totale d'exécution.
     * 
     * @return Durée d'exécution ou null si le traitement n'est pas terminé
     */
    public Duration getDuration() {
        if (endTime == null) {
            return Duration.between(startTime, Instant.now());
        } else {
            return Duration.between(startTime, endTime);
        }
    }
    
    /**
     * Obtient le nombre de réponses avec succès.
     * 
     * @return Nombre de réponses avec succès
     */
    public int getSuccessCount() {
        synchronized(responses) {
            return (int) responses.stream()
                    .filter(ApiResponse::isSuccess)
                    .count();
        }
    }
    
    /**
     * Obtient le nombre de réponses avec erreur.
     * 
     * @return Nombre de réponses avec erreur
     */
    public int getErrorResponseCount() {
        synchronized(responses) {
            return (int) responses.stream()
                    .filter(r -> !r.isSuccess())
                    .count();
        }
    }
    
    /**
     * Obtient le nombre total d'erreurs (réponses erreur + erreurs de ligne).
     * 
     * @return Nombre total d'erreurs
     */
    public int getTotalErrorCount() {
        return getErrorResponseCount() + errors.size();
    }
    
    /**
     * Obtient des statistiques sur les temps d'exécution des appels API.
     * 
     * @return Résumé statistique des temps d'exécution
     */
    public LongSummaryStatistics getExecutionTimeStats() {
        synchronized(responses) {
            return responses.stream()
                    .collect(Collectors.summarizingLong(ApiResponse::getExecutionTimeMs));
        }
    }
    
    /**
     * Obtient un résumé textuel des résultats.
     * 
     * @return Résumé des résultats
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        
        sb.append(String.format("Résultats: %d succès, %d erreurs, durée: %s", 
                getSuccessCount(), getTotalErrorCount(), formatDuration(getDuration())));
        
        if (!responses.isEmpty()) {
            LongSummaryStatistics stats = getExecutionTimeStats();
            sb.append(String.format(", temps d'exécution - min: %dms, max: %dms, moy: %.1fms", 
                    stats.getMin(), stats.getMax(), stats.getAverage()));
        }
        
        return sb.toString();
    }
    
    /**
     * Formate une durée en chaîne lisible.
     * 
     * @param duration Durée à formater
     * @return Chaîne formatée
     */
    private String formatDuration(Duration duration) {
        long totalSeconds = duration.getSeconds();
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        long millis = duration.toMillis() % 1000;
        
        if (minutes > 0) {
            return String.format("%dm %ds %dms", minutes, seconds, millis);
        } else {
            return String.format("%ds %dms", seconds, millis);
        }
    }
}