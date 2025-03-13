package com.etljobs.sql2json2api.service.threading;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.etljobs.sql2json2api.api.response.ApiResponse;
import com.etljobs.sql2json2api.exception.ProcessingException;
import com.etljobs.sql2json2api.model.ApiTemplateResult;
import com.etljobs.sql2json2api.model.RowError;
import com.etljobs.sql2json2api.model.SqlFile;
import com.etljobs.sql2json2api.service.http.ApiClientService;
import com.etljobs.sql2json2api.service.http.TokenService;
import com.etljobs.sql2json2api.service.sql.SqlExecutionService;
import com.etljobs.sql2json2api.service.template.TemplateProcessingService;

import lombok.extern.slf4j.Slf4j;

/**
 * Service responsable de l'exécution parallèle des appels API
 * basés sur les résultats d'un fichier SQL.
 */
@Service
@Slf4j
public class SqlBasedParallelApiExecutionService {
    
    private final ApiClientService apiClientService;
    private final TemplateProcessingService templateService;
    private final SqlExecutionService sqlExecutionService; 
    private final TokenService tokenService;
    private final ThreadPoolManager threadPoolManager;
    private final ApiCallTaskFactory apiCallTaskFactory;
    
    @Autowired
    public SqlBasedParallelApiExecutionService(
            ApiClientService apiClientService,
            TemplateProcessingService templateService,
            SqlExecutionService sqlExecutionService,
            TokenService tokenService,
            ThreadPoolManager threadPoolManager,
            ApiCallTaskFactory apiCallTaskFactory) {
        this.apiClientService = apiClientService;
        this.templateService = templateService;
        this.sqlExecutionService = sqlExecutionService;
        this.tokenService = tokenService;
        this.threadPoolManager = threadPoolManager;
        this.apiCallTaskFactory = apiCallTaskFactory;
    }
    
    /**
     * Exécute la requête SQL et traite les résultats avec des appels API parallèles.
     * 
     * @param sqlFile Le fichier SQL à exécuter
     * @return Résultats des appels API
     */
    public ApiCallResults executeAndWaitCompletion(SqlFile sqlFile) {
        try {
            log.info("Exécution du fichier SQL: {} avec traitement parallèle", sqlFile.getFileName());
            
            // 1. Exécuter la requête SQL
            List<Map<String, Object>> results = sqlExecutionService.executeQuery(sqlFile.getContent());
            log.info("SQL exécuté, {} lignes obtenues", results.size());
            
            if (results.isEmpty()) {
                log.info("Aucun résultat à traiter pour {}", sqlFile.getFileName());
                return new ApiCallResults();
            }
            
            // 2. Créer un gestionnaire de résultats
            ApiCallResults callResults = new ApiCallResults();
            
            // 3. Exécuter le traitement parallèle des résultats
            if (threadPoolManager.isEnabled()) {
                processInParallel(sqlFile, results, callResults);
            } else {
                processSequentially(sqlFile, results, callResults);
            }
            
            // 4. Marquer le traitement comme terminé et renvoyer les résultats
            callResults.markComplete();
            log.info(callResults.getSummary());
            
            return callResults;
            
        } catch (Exception e) {
            throw new ProcessingException("Erreur lors du traitement parallèle du fichier SQL: " 
                    + sqlFile.getFileName(), e);
        }
    }
    
    /**
     * Traite les résultats SQL en parallèle en utilisant le pool de threads.
     * 
     * @param sqlFile Le fichier SQL traité
     * @param results Les résultats SQL à traiter
     * @param callResults Le gestionnaire de résultats
     */
    private void processInParallel(SqlFile sqlFile, List<Map<String, Object>> results, ApiCallResults callResults) {
        log.info("Traitement parallèle de {} lignes pour {}", results.size(), sqlFile.getFileName());
        
        // Liste pour stocker les tâches d'appel API
        List<ApiCallTask> tasks = new ArrayList<>(results.size());
        
        // Obtenir un token partagé pour tous les appels
        String token = tokenService.getToken();
        
        // Préparer les tâches d'appel API pour chaque ligne
        for (int i = 0; i < results.size(); i++) {
            Map<String, Object> row = results.get(i);
            String rowIdentifier = extractRowIdentifier(row, i);
            
            try {
                // Traiter le template pour cette ligne
                ApiTemplateResult templateResult = templateService.processTemplate(
                        sqlFile.getTemplateName(), row);
                
                // Afficher le template généré pour débogage
                log.debug("Template généré pour la ligne {}: {}", 
                          rowIdentifier, templateResult.getJsonPayload());
                
                log.info("URL de l'appel API: {} {}", 
                          templateResult.getEndpointInfo().getMethod(),
                          templateResult.getEndpointInfo().getRoute());
                
                // Créer une tâche d'appel API
                ApiCallTask task = apiCallTaskFactory.createFromTemplateResult(
                        templateResult, i, rowIdentifier);
                
                tasks.add(task);
                
            } catch (Exception e) {
                log.error("Erreur lors de la préparation de la tâche pour la ligne {}: {}", 
                        rowIdentifier, e.getMessage());
                
                // Enregistrer l'erreur dans les résultats
                callResults.addError(new RowError(i, row, e.getMessage(), e, 0));
            }
        }
        
        if (tasks.isEmpty()) {
            log.warn("Aucune tâche d'appel API n'a pu être créée");
            return;
        }
        
        // Soumettre les tâches au pool et attendre les résultats
        try {
            log.info("Soumission de {} tâches au pool de threads", tasks.size());
            List<Future<ApiResponse>> futures = threadPoolManager.submitTasks(tasks);
            
            // Attendre et traiter les résultats
            ParallelExecutionResults<ApiResponse> executionResults = new ParallelExecutionResults<>();
            
            // Log avant l'attente
            log.info("En attente de la complétion de {} tâches...", futures.size());
            
            List<ApiResponse> responses = executionResults.waitForAll(futures, threadPoolManager.getTimeoutSeconds());
            
            // Vérifier si des réponses ont été obtenues
            log.info("Réception de {} réponses sur {} attendues", 
                    responses.size(), futures.size());
            
            // Afficher les réponses en détail
            for (int i = 0; i < responses.size(); i++) {
                ApiResponse response = responses.get(i);
                log.info("Réponse {}/{} - Statut: {}, Temps: {}ms", 
                        i+1, responses.size(), response.getStatusCode(), 
                        response.getExecutionTimeMs());
                
                // Afficher le corps de la réponse
                if (response.getBody() != null) {
                    String truncatedBody = truncateIfNeeded(response.getBody(), 500);
                    log.info("Corps: {}", truncatedBody);
                }
            }
            
            // Ajouter explicitement chaque réponse au gestionnaire de résultats
            for (ApiResponse response : responses) {
                callResults.addResponse(response);
            }
            
            // Ajouter les erreurs d'exécution au gestionnaire de résultats
            for (ParallelExecutionResults.ExecutionError error : executionResults.getErrors()) {
                int taskIndex = error.getTaskIndex();
                if (taskIndex < tasks.size()) {
                    ApiCallTask failedTask = tasks.get(taskIndex);
                    
                    RowError rowError = new RowError(
                            failedTask.getRowIndex(),
                            results.get(failedTask.getRowIndex()),
                            "Erreur d'exécution de la tâche: " + error.getMessage(),
                            error.getCause() instanceof Exception ? (Exception) error.getCause() : 
                                new Exception(error.getMessage(), error.getCause()),
                            1
                    );
                    
                    callResults.addError(rowError);
                    log.error("Erreur pour la tâche #{}: {}", taskIndex, rowError.getFormattedMessage());
                }
            }
            
            log.info("Traitement parallèle terminé: {} succès, {} timeouts, {} erreurs",
                    executionResults.getCompletedCount(),
                    executionResults.getTimeoutCount(),
                    executionResults.getErrorCount());
            
        } catch (Exception e) {
            log.error("Erreur lors de l'exécution parallèle: {}", e.getMessage(), e);
            throw new ProcessingException("Erreur lors du traitement parallèle", e);
        }
    }
    
    /**
     * Traite les résultats SQL séquentiellement (mode compatibilité).
     * 
     * @param sqlFile Le fichier SQL traité
     * @param results Les résultats SQL à traiter
     * @param callResults Le gestionnaire de résultats
     */
    private void processSequentially(SqlFile sqlFile, List<Map<String, Object>> results, ApiCallResults callResults) {
        log.info("Traitement séquentiel de {} lignes pour {}", results.size(), sqlFile.getFileName());
        
        // Obtenir un token partagé pour tous les appels
        String token = tokenService.getToken();
        
        // Traiter chaque ligne séquentiellement
        for (int i = 0; i < results.size(); i++) {
            Map<String, Object> row = results.get(i);
            String rowIdentifier = extractRowIdentifier(row, i);
            
            try {
                // Traiter le template pour cette ligne
                ApiTemplateResult templateResult = templateService.processTemplate(
                        sqlFile.getTemplateName(), row);
                
                // Afficher les informations de l'appel API
                log.info("Appel API pour la ligne {}: {} {}", 
                        rowIdentifier,
                        templateResult.getEndpointInfo().getMethod(),
                        templateResult.getEndpointInfo().getRoute());
                
                log.debug("Payload JSON: {}", templateResult.getJsonPayload());
                
                // Faire l'appel API directement
                com.etljobs.sql2json2api.model.ApiResponse legacyResponse = apiClientService.callApi(
                        templateResult.getEndpointInfo().getRoute(),
                        templateResult.getEndpointInfo().getMethod(),
                        templateResult.getJsonPayload(),
                        templateResult.getEndpointInfo().getHeaders(),
                        templateResult.getEndpointInfo().getUrlParams());
                
                // Convertir et ajouter la réponse aux résultats
                ApiResponse response = ApiResponse.builder()
                        .statusCode(legacyResponse.getStatusCode())
                        .body(legacyResponse.getBody())
                        .requestUrl(templateResult.getEndpointInfo().getRoute())
                        .requestId(rowIdentifier)
                        .attemptNumber(1)
                        .build();
                
                // Afficher la réponse
                log.info("Réponse de l'API - Statut: {}, Corps: {}", 
                        response.getStatusCode(), 
                        truncateIfNeeded(response.getBody(), 500));
                
                callResults.addResponse(response);
                
                log.debug("Ligne {} traitée avec statut: {}", 
                        rowIdentifier, legacyResponse.getStatusCode());
                
            } catch (Exception e) {
                log.error("Erreur lors du traitement de la ligne {}: {}", 
                        rowIdentifier, e.getMessage());
                
                // Enregistrer l'erreur dans les résultats
                callResults.addError(new RowError(i, row, e.getMessage(), e, 1));
            }
        }
        
        log.info("Traitement séquentiel terminé");
    }
    
    /**
     * Tronque une chaîne si elle dépasse une longueur maximale
     */
    private String truncateIfNeeded(String text, int maxLength) {
        if (text == null) {
            return "null";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
    
    /**
     * Extrait un identifiant lisible d'une ligne de résultat.
     * 
     * @param row La ligne de résultat
     * @param fallbackIndex Index à utiliser si aucun identifiant n'est trouvé
     * @return Identifiant de ligne lisible
     */
    private String extractRowIdentifier(Map<String, Object> row, int fallbackIndex) {
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
        
        // Si tout échoue, utiliser l'index
        return "row#" + (fallbackIndex + 1);
    }
}