package com.etljobs.sql2json2api.util.correlation;

import java.util.concurrent.Callable;

import lombok.extern.slf4j.Slf4j;

/**
 * Utilitaire simple pour propager les IDs de corrélation aux threads enfants.
 */
@Slf4j
public class CorrelationPropagator {
    
    /**
     * Enveloppe une tâche Callable pour propager l'ID de corrélation courant.
     * 
     * @param <T> Type de retour de la tâche
     * @param task Tâche à envelopper
     * @return Tâche enveloppée qui propage l'ID de corrélation
     */
    public static <T> Callable<T> wrap(Callable<T> task) {
        // Capturer l'ID de corrélation du thread appelant
        final String parentCorrelationId = CorrelationContext.getId();
        
        if (parentCorrelationId == null) {
            // Si pas d'ID parent, retourner la tâche telle quelle
            return task;
        }
        
        // Créer une tâche qui propage l'ID
        return () -> {
            // Définir l'ID de corrélation dans le thread enfant
            CorrelationContext.setId(parentCorrelationId);
            try {
                // Exécuter la tâche originale
                return task.call();
            } finally {
                // Nettoyer l'ID de corrélation
                CorrelationContext.clear();
            }
        };
    }
}