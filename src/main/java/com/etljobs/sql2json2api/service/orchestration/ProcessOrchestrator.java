package com.etljobs.sql2json2api.service.orchestration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.etljobs.sql2json2api.exception.ProcessingException;
import com.etljobs.sql2json2api.exception.TemplateProcessingException;
import com.etljobs.sql2json2api.model.ApiResponse;
import com.etljobs.sql2json2api.model.ApiTemplateResult;
import com.etljobs.sql2json2api.model.SqlFile;
import com.etljobs.sql2json2api.service.http.ApiClientService;
import com.etljobs.sql2json2api.service.http.TokenService;
import com.etljobs.sql2json2api.service.orchestration.RetryStrategy.RetryContext;
import com.etljobs.sql2json2api.service.sql.SqlExecutionService;
import com.etljobs.sql2json2api.service.sql.SqlFileService;
import com.etljobs.sql2json2api.service.template.TemplateProcessingService;

import lombok.extern.slf4j.Slf4j;

/**
 * Orchestrateur du processus global de traitement des fichiers SQL
 * vers des appels API.
 * 
 * Cette classe implémente le flux complet de traitement:
 * - Lecture et exécution du fichier SQL
 * - Traitement des lignes de résultat
 * - Transformation en JSON via templates
 * - Appels API avec gestion des erreurs et réessais
 */
@Service
@Slf4j
public class ProcessOrchestrator {
    
    private final SqlFileService sqlFileService;
    private final SqlExecutionService sqlExecutionService;
    private final TemplateProcessingService templateService;
    private final TokenService tokenService;
    private final ApiClientService apiClientService;
    private final RetryStrategyFactory retryStrategyFactory;
    
    /**
     * Structure interne pour stocker les détails d'erreur par ligne.
     */
    private static class RowError {
        private final int rowIndex;
        private final Map<String, Object> rowData;
        private final String errorMessage;
        private final Exception exception;
        private final int attempts;
        
        public RowError(int rowIndex, Map<String, Object> rowData, String errorMessage, Exception exception, int attempts) {
            this.rowIndex = rowIndex;
            this.rowData = rowData;
            this.errorMessage = errorMessage;
            this.exception = exception;
            this.attempts = attempts;
        }
        
        @Override
        public String toString() {
            return String.format("Erreur à la ligne %d (après %d tentatives): %s", 
                    rowIndex + 1, attempts, errorMessage);
        }
    }
    
    /**
     * Constructeur avec injection de dépendances.
     */
    @Autowired
    public ProcessOrchestrator(
            SqlFileService sqlFileService,
            SqlExecutionService sqlExecutionService,
            TemplateProcessingService templateService,
            TokenService tokenService,
            ApiClientService apiClientService,
            RetryStrategyFactory retryStrategyFactory) {
        this.sqlFileService = sqlFileService;
        this.sqlExecutionService = sqlExecutionService;
        this.templateService = templateService;
        this.tokenService = tokenService;
        this.apiClientService = apiClientService;
        this.retryStrategyFactory = retryStrategyFactory;
    }
    
    /**
     * Traite un fichier SQL en exécutant la requête et en traitant toutes les lignes
     * de résultat pour générer des appels API, avec stratégie de réessai en cas d'erreur.
     * 
     * @param sqlFileName Nom du fichier SQL à traiter
     * @return Liste des réponses API pour chaque ligne traitée avec succès
     * @throws ProcessingException en cas d'erreur globale de traitement
     */
    public List<ApiResponse> processSqlFile(String sqlFileName) {
        List<ApiResponse> responses = new ArrayList<>();
        List<RowError> rowErrors = new ArrayList<>();
        
        try {
            log.info("Début du traitement du fichier SQL: {}", sqlFileName);
            
            // 1. Lire le fichier SQL
            SqlFile sqlFile = sqlFileService.readSqlFile(sqlFileName);
            
            // 2. Exécuter la requête SQL
            List<Map<String, Object>> results = sqlExecutionService.executeQuery(sqlFile.getContent());
            log.info("Requête SQL exécutée avec succès, {} résultats obtenus", results.size());
            
            // 3. Si aucun résultat, retourner une liste vide
            if (results.isEmpty()) {
                log.info("Aucun résultat à traiter pour ce fichier SQL");
                return responses;
            }
            
            // 4. Générer le token d'authentification (une seule fois pour tous les appels)
            String token = tokenService.getToken();
            log.debug("Token d'authentification généré avec succès");
            
            // 5. Traiter chaque ligne de résultat
            int totalRows = results.size();
            log.info("Début du traitement des {} lignes de résultat", totalRows);
            
            for (int i = 0; i < totalRows; i++) {
                Map<String, Object> row = results.get(i);
                String rowIdentifier = extractRowIdentifier(row);
                log.debug("Traitement de la ligne {}/{}: {}", i + 1, totalRows, rowIdentifier);
                
                // Traiter cette ligne avec un mécanisme de réessai
                ApiResponse response = processRow(sqlFile, row, i, rowIdentifier, rowErrors);
                
                // Important: ajouter la réponse à la liste même si elle contient une erreur
                if (response != null) {
                    responses.add(response);
                }
            }
            
            log.info("Traitement terminé: {}/{} lignes traitées avec succès ({} erreurs)", 
                    responses.size(), totalRows, rowErrors.size());
            
        } catch (Exception e) {
            log.error("Erreur globale lors du traitement du fichier SQL: {}", sqlFileName, e);
            throw new ProcessingException("Erreur lors du traitement du fichier SQL: " + sqlFileName, e);
        }
        
        return responses;
    }
    
    /**
     * Traite une seule ligne avec un mécanisme de réessai.
     * 
     * @param sqlFile Le fichier SQL traité
     * @param row La ligne de données à traiter
     * @param rowIndex L'index de la ligne (pour les logs)
     * @param rowIdentifier L'identifiant lisible de la ligne (pour les logs)
     * @param rowErrors Liste pour collecter les erreurs par ligne
     * @return La réponse API générée, ou null si une erreur non récupérable est survenue
     */
    protected ApiResponse processRow(SqlFile sqlFile, Map<String, Object> row, int rowIndex, 
            String rowIdentifier, List<RowError> rowErrors) {
        try {
            // Créer la stratégie de réessai
            RetryStrategy retryStrategy = retryStrategyFactory.create();
            RetryContext retryContext = retryStrategy.createContext();
            
            // Traiter le template (une seule fois car le contenu ne change pas)
            ApiTemplateResult templateResult = templateService.processTemplate(sqlFile.getTemplateName(), row);
            
            // Variables pour stocker le résultat et l'erreur
            ApiResponse response = null;
            Exception lastError = null;
            
            // Tenter l'appel API avec réessai
            while (retryContext.getCurrentAttempt() <= retryStrategy.getMaxAttempts()) {
                int attempt = retryContext.getCurrentAttempt();
                
                if (attempt > 1) {
                    log.info("Tentative {} pour la ligne {}", attempt, rowIdentifier);
                }
                
                try {
                    // Appeler l'API
                    response = apiClientService.callApi(
                            templateResult.getEndpointInfo().getRoute(),
                            templateResult.getEndpointInfo().getMethod(),
                            templateResult.getJsonPayload(),
                            templateResult.getEndpointInfo().getHeaders(),
                            templateResult.getEndpointInfo().getUrlParams());
                    
                    // Sauvegarder le code de statut dans le contexte
                    retryContext.setLastStatusCode(response.getStatusCode());
                    
                    // Vérifier si l'appel a réussi ou si c'est une erreur non récupérable
                    if (response.isSuccess() || 
                            !retryStrategy.shouldRetry(response.getStatusCode(), attempt)) {
                        return response; // On retourne la réponse même en cas d'erreur
                    }
                    
                    // C'est une erreur récupérable, et on n'a pas atteint le max de tentatives
                    log.warn("API a répondu avec le code {} pour la ligne {}. Réessai...", 
                            response.getStatusCode(), rowIdentifier);
                    
                    // Préparer la prochaine tentative
                    retryContext.incrementAttempt();
                    retryStrategy.sleep(attempt + 1);
                    
                } catch (Exception e) {
                    lastError = e;
                    retryContext.setLastException(e);
                    
                    if (!retryStrategy.shouldRetry(e, attempt)) {
                        // Erreur non récupérable
                        log.error("Erreur non récupérable pour la ligne {}: {}", 
                                rowIdentifier, e.getMessage());
                        
                        // Ajouter aux erreurs
                        rowErrors.add(new RowError(rowIndex, row, e.getMessage(), e, attempt));
                        
                        // Retourner une réponse d'erreur
                        return ApiResponse.builder()
                                .statusCode(500)
                                .body("Erreur non récupérable: " + e.getMessage())
                                .build();
                    }
                    
                    if (attempt == retryStrategy.getMaxAttempts()) {
                        // On a atteint le max de tentatives
                        log.error("Échec définitif pour la ligne {} après {} tentatives: {}", 
                                rowIdentifier, attempt, e.getMessage());
                        
                        // Ajouter aux erreurs
                        rowErrors.add(new RowError(rowIndex, row, e.getMessage(), e, attempt));
                        
                        // Retourner une réponse d'erreur
                        return ApiResponse.builder()
                                .statusCode(500)
                                .body("Erreur après " + attempt + " tentatives: " + e.getMessage())
                                .build();
                    }
                    
                    // Attendre avant de réessayer
                    log.warn("Erreur récupérable lors de l'appel API pour la ligne {}: {}. Réessai...", 
                            rowIdentifier, e.getMessage());
                    
                    // Préparer la prochaine tentative
                    retryContext.incrementAttempt();
                    retryStrategy.sleep(attempt + 1);
                }
            }
            
            // Si on arrive ici, c'est qu'il y a eu une erreur mais on n'a pas atteint maxRetryAttempts
            // Ce cas est théoriquement impossible avec la logique ci-dessus
            if (lastError != null) {
                return ApiResponse.builder()
                        .statusCode(500)
                        .body("Erreur non gérée: " + lastError.getMessage())
                        .build();
            } else {
                return ApiResponse.builder()
                        .statusCode(500)
                        .body("Erreur inconnue: aucune réponse ni erreur générée")
                        .build();
            }
            
        } catch (TemplateProcessingException e) {
            // Erreur lors du traitement du template, non récupérable
            log.error("Erreur de template pour la ligne {}: {}", rowIdentifier, e.getMessage());
            rowErrors.add(new RowError(rowIndex, row, e.getMessage(), e, 1));
            
            return ApiResponse.builder()
                    .statusCode(500)
                    .body("Erreur de template: " + e.getMessage())
                    .build();
        } catch (Exception e) {
            // Toute autre erreur non prévue
            log.error("Erreur inattendue pour la ligne {}: {}", rowIdentifier, e.getMessage());
            rowErrors.add(new RowError(rowIndex, row, e.getMessage(), e, 1));
            
            return ApiResponse.builder()
                    .statusCode(500)
                    .body("Erreur inattendue: " + e.getMessage())
                    .build();
        }
    }
    
    /**
     * Extrait un identifiant de la ligne pour faciliter les logs et le suivi.
     * Essaie de trouver un champ 'id', 'ID', 'uuid', ou similaire.
     * 
     * @param row La ligne de données
     * @return Un identifiant lisible pour la ligne
     */
    private String extractRowIdentifier(Map<String, Object> row) {
        // Essayer d'extraire un ID lisible
        for (String idField : new String[]{"id", "ID", "Id", "uuid", "UUID", "key", "KEY"}) {
            if (row.containsKey(idField) && row.get(idField) != null) {
                return idField + "=" + row.get(idField);
            }
        }
        
        // Utiliser la première clé/valeur disponible
        if (!row.isEmpty()) {
            Map.Entry<String, Object> firstEntry = row.entrySet().iterator().next();
            return firstEntry.getKey() + "=" + firstEntry.getValue();
        }
        
        // Si tout échoue, utiliser un identifiant arbitraire
        return "row@" + System.identityHashCode(row);
    }
}