package com.etljobs.sql2json2api.service.threading;

import java.util.Map;
import java.util.concurrent.Callable;

import org.springframework.http.HttpMethod;

import com.etljobs.sql2json2api.api.response.ApiResponse;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Représente une tâche d'appel API qui peut être exécutée par un pool de
 * threads. Cette classe abstraite sert de base pour les implémentations
 * spécifiques.
 */
@Slf4j
public abstract class ApiCallTask implements Callable<ApiResponse> {

    @Getter
    protected final String url;

    @Getter
    protected final HttpMethod method;

    @Getter
    protected final String payload;

    @Getter
    protected final Map<String, String> headers;

    @Getter
    protected final Map<String, Object> urlParams;

    @Getter
    protected final int rowIndex;

    @Getter
    protected final String rowIdentifier;

    // ID de corrélation propre à cette tâche
    @Getter
    protected final String correlationId;

    /**
     * Constructeur avec tous les paramètres nécessaires pour un appel API.
     */
    protected ApiCallTask(String url, HttpMethod method, String payload,
            Map<String, String> headers, Map<String, Object> urlParams,
            int rowIndex, String rowIdentifier) {
        this.url = url;
        this.method = method;
        this.payload = payload;
        this.headers = headers;
        this.urlParams = urlParams;
        this.rowIndex = rowIndex;
        this.rowIdentifier = rowIdentifier;
        // Générer un ID de corrélation unique pour cette tâche
        this.correlationId = com.etljobs.sql2json2api.util.correlation.CorrelationContext.generateId();
    }

    @Override
    public ApiResponse call() throws Exception {
        // Stocker l'ID de corrélation parent avant de le remplacer
        String parentCorrelationId = com.etljobs.sql2json2api.util.correlation.CorrelationContext.getId();

        // Définir l'ID de corrélation de cette tâche
        com.etljobs.sql2json2api.util.correlation.CorrelationContext.setId(correlationId);

        log.debug("[{}] Exécution de l'appel API {} {} pour la ligne {}",
                correlationId, method, url, rowIdentifier);

        try {
            long startTime = System.currentTimeMillis();
            ApiResponse response = executeApiCall();
            long duration = System.currentTimeMillis() - startTime;

            // S'assurer que l'ID de corrélation est défini dans la réponse
            if (response != null && (response.getRequestId() == null || response.getRequestId().isEmpty())) {
                response.setRequestId(correlationId);
            }

            log.debug("[{}] Appel API {} {} pour la ligne {} terminé en {}ms avec statut {}",
                    correlationId, method, url, rowIdentifier, duration,
                    response != null ? response.getStatusCode() : "N/A");

            return response;
        } catch (Exception e) {
            log.error("[{}] Erreur lors de l'appel API {} {} pour la ligne {}: {}",
                    correlationId, method, url, rowIdentifier, e.getMessage());
            throw e;
        } finally {
            // Nettoyer l'ID de corrélation de cette tâche
            com.etljobs.sql2json2api.util.correlation.CorrelationContext.clear();

            // Restaurer l'ID de corrélation parent s'il existait
            if (parentCorrelationId != null) {
                com.etljobs.sql2json2api.util.correlation.CorrelationContext.setId(parentCorrelationId);
            }

            log.debug("[{}] ID de corrélation nettoyé après l'exécution de la tâche pour la ligne {}",
                    correlationId, rowIdentifier);
        }
    }

    /**
     * Méthode abstraite à implémenter par les sous-classes pour exécuter
     * l'appel API réel.
     *
     * @return Réponse de l'API
     * @throws Exception si l'appel échoue
     */
    protected abstract ApiResponse executeApiCall() throws Exception;
}
