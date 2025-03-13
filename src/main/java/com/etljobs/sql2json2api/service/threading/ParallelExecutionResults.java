package com.etljobs.sql2json2api.service.threading;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Classe utilitaire pour attendre et récupérer les résultats de tâches parallèles.
 * Cette classe simplifie la gestion des futures et des exceptions.
 * 
 * @param <T> Type des résultats attendus
 */
@Slf4j
public class ParallelExecutionResults<T> {
    
    @Getter
    private final List<T> successfulResults = new ArrayList<>();
    
    @Getter
    private final List<ExecutionError> errors = new ArrayList<>();
    
    @Getter
    private int completedCount = 0;
    
    @Getter
    private int timeoutCount = 0;
    
    @Getter
    private int errorCount = 0;
    
    /**
     * Attend la complétion de toutes les tâches et collecte les résultats.
     * 
     * @param futures Liste des futures à attendre
     * @param timeoutSeconds Timeout en secondes pour chaque future
     * @return Liste des résultats obtenus avec succès
     */
    public List<T> waitForAll(List<Future<T>> futures, int timeoutSeconds) {
        completedCount = 0;
        timeoutCount = 0;
        errorCount = 0;
        
        log.info("Attente des résultats pour {} tâches avec timeout de {}s", 
                futures.size(), timeoutSeconds);
        
        for (int i = 0; i < futures.size(); i++) {
            Future<T> future = futures.get(i);
            try {
                T result = future.get(timeoutSeconds, TimeUnit.SECONDS);
                successfulResults.add(result);
                completedCount++;
                
                // Log progressif pour les grands ensembles de tâches
                if (completedCount % 10 == 0 || completedCount == futures.size()) {
                    log.debug("Progression: {}/{} tâches complétées", completedCount, futures.size());
                }
                
            } catch (TimeoutException e) {
                timeoutCount++;
                errors.add(new ExecutionError(i, "Timeout après " + timeoutSeconds + "s", e));
                future.cancel(true);
                log.warn("Timeout pour la tâche #{}", i);
                
            } catch (InterruptedException e) {
                errorCount++;
                errors.add(new ExecutionError(i, "Interruption de la tâche", e));
                Thread.currentThread().interrupt();
                log.warn("Tâche #{} interrompue", i);
                
            } catch (ExecutionException e) {
                errorCount++;
                errors.add(new ExecutionError(i, "Erreur d'exécution: " + e.getCause().getMessage(), e.getCause()));
                log.warn("Erreur dans la tâche #{}: {}", i, e.getCause().getMessage());
            }
        }
        
        // Résumé des résultats
        log.info("Résultats: {} succès, {} timeouts, {} erreurs", 
                completedCount, timeoutCount, errorCount);
        
        return successfulResults;
    }
    
    /**
     * Classe interne représentant une erreur d'exécution.
     */
    @Getter
    public static class ExecutionError {
        private final int taskIndex;
        private final String message;
        private final Throwable cause;
        
        public ExecutionError(int taskIndex, String message, Throwable cause) {
            this.taskIndex = taskIndex;
            this.message = message;
            this.cause = cause;
        }
    }
}