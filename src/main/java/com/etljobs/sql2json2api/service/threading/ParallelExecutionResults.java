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
     * @param tasks Liste des tâches correspondantes (pour récupérer les IDs de corrélation)
     * @param timeoutSeconds Timeout en secondes pour chaque future
     * @return Liste des résultats obtenus avec succès
     */
    public List<T> waitForAll(List<Future<T>> futures, List<ApiCallTask> tasks, int timeoutSeconds) {
        completedCount = 0;
        timeoutCount = 0;
        errorCount = 0;
        successfulResults.clear();
        errors.clear();
        
        log.info("Attente des résultats pour {} tâches avec timeout de {}s", 
                futures.size(), timeoutSeconds);
        
        for (int i = 0; i < futures.size(); i++) {
            Future<T> future = futures.get(i);
            ApiCallTask task = (i < tasks.size()) ? tasks.get(i) : null;
            String correlationId = (task != null) ? task.getCorrelationId() : "unknown";
            
            try {
                // Log de progression
                if (i % 5 == 0 || i == futures.size() - 1) {
                    log.debug("[{}] Traitement de la future {}/{}", 
                            correlationId, i+1, futures.size());
                }
                
                T result = future.get(timeoutSeconds, TimeUnit.SECONDS);
                successfulResults.add(result);
                completedCount++;
                
                // Log progressif pour les grands ensembles de tâches
                if (completedCount % 10 == 0 || completedCount == futures.size()) {
                    log.debug("Progression: {}/{} tâches complétées", completedCount, futures.size());
                }
                
            } catch (TimeoutException e) {
                timeoutCount++;
                errors.add(new ExecutionError(i, correlationId, "Timeout après " + timeoutSeconds + "s", e));
                future.cancel(true);
                log.warn("[{}] Timeout pour la tâche #{}", correlationId, i);
                
            } catch (InterruptedException e) {
                errorCount++;
                errors.add(new ExecutionError(i, correlationId, "Interruption de la tâche", e));
                Thread.currentThread().interrupt();
                log.warn("[{}] Tâche #{} interrompue", correlationId, i);
                
            } catch (ExecutionException e) {
                errorCount++;
                String errorMsg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
                errors.add(new ExecutionError(i, correlationId, "Erreur d'exécution: " + errorMsg, e.getCause()));
                log.warn("[{}] Erreur dans la tâche #{}: {}", correlationId, i, errorMsg);
                
            } catch (Exception e) {
                // Capturer toute autre exception non prévue
                errorCount++;
                errors.add(new ExecutionError(i, correlationId, "Exception inattendue: " + e.getMessage(), e));
                log.error("[{}] Exception inattendue dans la tâche #{}: {}", correlationId, i, e.getMessage(), e);
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
        private final String correlationId;
        private final String message;
        private final Throwable cause;
        
        public ExecutionError(int taskIndex, String correlationId, String message, Throwable cause) {
            this.taskIndex = taskIndex;
            this.correlationId = correlationId;
            this.message = message;
            this.cause = cause;
        }
    }
}