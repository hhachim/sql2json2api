package com.etljobs.sql2json2api.util.correlation;

import java.util.UUID;
import java.util.function.Supplier;

import org.slf4j.MDC;

/**
 * Utilitaire pour gérer les identifiants de corrélation.
 * Permet de générer et stocker des identifiants uniques pour tracer les requêtes.
 */
public class CorrelationContext {
    
    /**
     * Clé utilisée pour stocker l'identifiant dans le MDC
     */
    public static final String CORRELATION_ID_KEY = "correlationId";
    
    /**
     * Préfixe pour les identifiants de corrélation (pour mieux les identifier dans les logs)
     */
    private static final String CORRELATION_ID_PREFIX = "cid-";
    
    /**
     * Génère un nouvel identifiant de corrélation et le stocke dans le MDC.
     * 
     * @return L'identifiant généré
     */
    public static String setId() {
        String correlationId = generateId();
        MDC.put(CORRELATION_ID_KEY, correlationId);
        return correlationId;
    }
    
    /**
     * Stocke l'identifiant de corrélation spécifié dans le MDC.
     * 
     * @param correlationId Identifiant à stocker
     */
    public static void setId(String correlationId) {
        if (correlationId != null && !correlationId.trim().isEmpty()) {
            MDC.put(CORRELATION_ID_KEY, correlationId);
        }
    }
    
    /**
     * Obtient l'identifiant de corrélation actuel depuis le MDC.
     * 
     * @return L'identifiant stocké ou null si aucun n'est défini
     */
    public static String getId() {
        return MDC.get(CORRELATION_ID_KEY);
    }
    
    /**
     * Nettoie l'identifiant de corrélation du MDC.
     * Cette méthode doit être appelée à la fin du traitement pour éviter les fuites.
     */
    public static void clear() {
        MDC.remove(CORRELATION_ID_KEY);
    }
    
    /**
     * Génère un nouvel identifiant de corrélation unique.
     * 
     * @return Un identifiant au format préfixe + UUID court
     */
    public static String generateId() {
        return CORRELATION_ID_PREFIX + UUID.randomUUID().toString().substring(0, 8);
    }
    
    /**
     * Exécute une action avec un ID de corrélation garanti.
     * Si un ID existe déjà, il sera utilisé, sinon un nouveau sera créé.
     * 
     * @param <T> Type de retour de l'action
     * @param action Action à exécuter
     * @return Résultat de l'action
     */
    public static <T> T withCorrelationId(Supplier<T> action) {
        String existingId = getId();
        boolean created = false;
        
        if (existingId == null) {
            setId();
            created = true;
        }
        
        try {
            return action.get();
        } finally {
            if (created) {
                clear();
            }
        }
    }
    
    /**
     * Exécute une action sans retour avec un ID de corrélation garanti.
     * 
     * @param action Action à exécuter
     */
    public static void withCorrelationId(Runnable action) {
        withCorrelationId(() -> {
            action.run();
            return null;
        });
    }
}