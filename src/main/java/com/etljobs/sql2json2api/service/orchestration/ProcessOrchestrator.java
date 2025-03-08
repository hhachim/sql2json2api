package com.etljobs.sql2json2api.service.orchestration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.etljobs.sql2json2api.exception.ApiCallException;
import com.etljobs.sql2json2api.exception.ProcessingException;
import com.etljobs.sql2json2api.exception.TemplateProcessingException;
import com.etljobs.sql2json2api.model.ApiResponse;
import com.etljobs.sql2json2api.model.ApiTemplateResult;
import com.etljobs.sql2json2api.model.SqlFile;
import com.etljobs.sql2json2api.service.http.ApiClientService;
import com.etljobs.sql2json2api.service.http.TokenService;
import com.etljobs.sql2json2api.service.sql.SqlExecutionService;
import com.etljobs.sql2json2api.service.sql.SqlFileService;
import com.etljobs.sql2json2api.service.template.TemplateProcessingService;

import lombok.extern.slf4j.Slf4j;

/**
 * Orchestrateur du processus global de traitement des fichiers SQL
 * vers des appels API.
 * 
 * Cette classe implémente les étapes 8.1 à 8.5 :
 * - 8.1 : Lecture et exécution du fichier SQL
 * - 8.2 : Traitement d'une ligne de résultat
 * - 8.3 : Extension au traitement multi-lignes
 * - 8.4 : Gestion des erreurs par ligne
 * - 8.5 : Stratégie de reprise
 */
@Service
@Slf4j
public class ProcessOrchestrator {
    
    private final SqlFileService sqlFileService;
    private final SqlExecutionService sqlExecutionService;
    private final TemplateProcessingService templateService;
    private final TokenService tokenService;
    private final ApiClientService apiClientService;
    
    // Configuration pour la stratégie de réessai
    @Value("${app.retry.max-attempts:3}")
    private int maxRetryAttempts;
    
    @Value("${app.retry.delay-ms:2000}")
    private long retryDelayMs;
    
    @Value("${app.retry.backoff-multiplier:1.5}")
    private double backoffMultiplier;
    
    // Liste des codes HTTP qui méritent un réessai
    private static final List<Integer> RETRYABLE_STATUS_CODES = List.of(
            HttpStatus.TOO_MANY_REQUESTS.value(),  // 429
            HttpStatus.SERVICE_UNAVAILABLE.value(), // 503
            HttpStatus.GATEWAY_TIMEOUT.value(),     // 504
            HttpStatus.INTERNAL_SERVER_ERROR.value() // 500
    );
    
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
            ApiClientService apiClientService) {
        this.sqlFileService = sqlFileService;
        this.sqlExecutionService = sqlExecutionService;
        this.templateService = templateService;
        this.tokenService = tokenService;
        this.apiClientService = apiClientService;
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
        try {
            log.info("Début du traitement du fichier SQL: {}", sqlFileName);
            
            // 1. Lire le fichier SQL
            SqlFile sqlFile = sqlFileService.readSqlFile(sqlFileName);
            log.debug("Fichier SQL lu: {}", sqlFile.getFileName());
            
            // 2. Exécuter la requête SQL
            List<Map<String, Object>> results = sqlExecutionService.executeQuery(sqlFile.getContent());
            log.info("Requête SQL exécutée avec succès, {} résultats obtenus", results.size());
            
            // 3. Préparer la liste pour stocker les réponses
            List<ApiResponse> responses = new ArrayList<>();
            
            // 4. Préparer la liste pour stocker les erreurs
            List<RowError> rowErrors = new ArrayList<>();
            
            // 5. Si aucun résultat, retourner une liste vide
            if (results.isEmpty()) {
                log.info("Aucun résultat à traiter pour ce fichier SQL");
                return responses;
            }
            
            // 6. Générer le token d'authentification (une seule fois pour tous les appels)
            String token = tokenService.getToken();
            log.debug("Token d'authentification généré avec succès");
            
            // 7. Traiter chaque ligne de résultat
            int totalRows = results.size();
            log.info("Début du traitement des {} lignes de résultat", totalRows);
            
            for (int i = 0; i < totalRows; i++) {
                Map<String, Object> row = results.get(i);
                String rowIdentifier = extractRowIdentifier(row);
                log.debug("Traitement de la ligne {}/{}: {}", i + 1, totalRows, rowIdentifier);
                
                ApiResponse response = processRowWithRetry(sqlFile, row, i, rowIdentifier, rowErrors);
                
                if (response != null) {
                    responses.add(response);
                    log.debug("Réponse ajoutée pour la ligne {}", rowIdentifier);
                }
                
                // Si c'est un paquet de 10 lignes ou la dernière ligne, on fait un log
                if ((i + 1) % 10 == 0 || i == totalRows - 1) {
                    log.info("Progression: {}/{} lignes traitées ({} réussies, {} erreurs)", 
                            i + 1, totalRows, responses.size(), rowErrors.size());
                }
            }
            
            // 8. Rapport de fin de traitement
            log.info("Traitement terminé: {}/{} lignes traitées avec succès ({} erreurs)", 
                    responses.size(), totalRows, rowErrors.size());
            
            // 9. Si des erreurs ont été détectées, les journaliser de manière détaillée
            if (!rowErrors.isEmpty()) {
                log.warn("Résumé des erreurs par ligne:");
                for (RowError error : rowErrors) {
                    log.warn(" - {}", error);
                    if (error.exception != null) {
                        log.debug("   Détail de l'erreur: ", error.exception);
                    }
                }
            }
            
            return responses;
            
        } catch (Exception e) {
            log.error("Erreur globale lors du traitement du fichier SQL: {}", sqlFileName, e);
            throw new ProcessingException("Erreur lors du traitement du fichier SQL: " + sqlFileName, e);
        }
    }
    
    /**
     * Traite une ligne avec stratégie de réessai.
     * 
     * @param sqlFile Le fichier SQL en cours de traitement
     * @param row La ligne à traiter
     * @param rowIndex L'index de la ligne
     * @param rowIdentifier L'identifiant lisible de la ligne pour les logs
     * @param rowErrors Liste pour collecter les erreurs
     * @return La réponse API, ou null si le traitement a échoué définitivement
     */
    private ApiResponse processRowWithRetry(
            SqlFile sqlFile, 
            Map<String, Object> row, 
            int rowIndex, 
            String rowIdentifier,
            List<RowError> rowErrors) {
        
        // Traiter le template pour cette ligne (une seule fois, car le contenu ne change pas)
        ApiTemplateResult templateResult;
        try {
            templateResult = templateService.processTemplate(sqlFile.getTemplateName(), row);
            log.debug("Template traité avec succès pour la ligne {}", rowIdentifier);
        } catch (TemplateProcessingException e) {
            // Erreur de template - pas la peine de réessayer si c'est la même donnée
            String errorMsg = "Erreur lors du traitement du template pour la ligne " + rowIdentifier;
            log.error("{}: {}", errorMsg, e.getMessage());
            rowErrors.add(new RowError(rowIndex, row, errorMsg, e, 1));
            return null; // Pas de réessai pour les erreurs de template
        }
        
        // Si le template est traité avec succès, tenter les appels API avec réessai
        int attempt = 1;
        long currentDelay = retryDelayMs;
        
        while (attempt <= maxRetryAttempts) {
            if (attempt > 1) {
                log.info("Tentative {} pour la ligne {}", attempt, rowIdentifier);
            }
            
            try {
                // Faire l'appel API pour cette ligne
                ApiResponse response = apiClientService.callApi(
                        templateResult.getEndpointInfo().getRoute(),
                        templateResult.getEndpointInfo().getMethod(),
                        templateResult.getJsonPayload(),
                        templateResult.getEndpointInfo().getHeaders(),
                        templateResult.getEndpointInfo().getUrlParams());
                
                log.debug("Appel API effectué pour la ligne {}, statut: {}", 
                        rowIdentifier, response.getStatusCode());
                
                // Vérifier si l'appel a réussi (code 2xx)
                if (!response.isSuccess()) {
                    // Si c'est un code d'erreur qui mérite un réessai
                    if (isRetryableStatusCode(response.getStatusCode()) && attempt < maxRetryAttempts) {
                        log.warn("L'API a répondu avec un code {} pour la ligne {}. Réessai dans {} ms...", 
                                response.getStatusCode(), rowIdentifier, currentDelay);
                        
                        // Attendre avant de réessayer
                        sleep(currentDelay);
                        
                        // Augmenter le délai pour le prochain réessai (backoff exponentiel)
                        currentDelay = Math.round(currentDelay * backoffMultiplier);
                        attempt++;
                        continue; // Passer à la prochaine tentative
                    } else {
                        // Code d'erreur qui ne mérite pas de réessai ou max tentatives atteint
                        String errorMsg = "L'API a répondu avec un code d'erreur " + response.getStatusCode() + 
                                " pour la ligne " + rowIdentifier;
                        log.warn("{}: {}", errorMsg, response.getBody());
                        rowErrors.add(new RowError(rowIndex, row, errorMsg, null, attempt));
                        
                        // Dans certains cas, on peut vouloir retourner la réponse même en cas d'erreur
                        return response;
                    }
                }
                
                // Succès, on retourne la réponse
                return response;
                
            } catch (ApiCallException e) {
                // Erreur lors de l'appel API
                String errorMsg = "Erreur lors de l'appel API pour la ligne " + rowIdentifier;
                log.error("{}: {}", errorMsg, e.getMessage());
                
                // Si on n'a pas atteint le max de tentatives, on réessaie
                if (attempt < maxRetryAttempts) {
                    log.info("Réessai pour la ligne {} dans {} ms...", rowIdentifier, currentDelay);
                    sleep(currentDelay);
                    currentDelay = Math.round(currentDelay * backoffMultiplier);
                    attempt++;
                    continue; // Passer à la prochaine tentative
                } else {
                    // Max tentatives atteint
                    rowErrors.add(new RowError(rowIndex, row, errorMsg, e, attempt));
                    
                    // Créer une réponse d'erreur
                    return ApiResponse.builder()
                            .statusCode(500)
                            .body("Erreur d'appel API après " + attempt + " tentatives: " + e.getMessage())
                            .build();
                }
            } catch (Exception e) {
                // Capture des erreurs non spécifiques
                String errorMsg = "Erreur inattendue lors de l'appel API pour la ligne " + rowIdentifier;
                log.error("{}: {}", errorMsg, e.getMessage(), e);
                
                // Si on n'a pas atteint le max de tentatives, on réessaie
                if (attempt < maxRetryAttempts) {
                    log.info("Réessai pour la ligne {} dans {} ms...", rowIdentifier, currentDelay);
                    sleep(currentDelay);
                    currentDelay = Math.round(currentDelay * backoffMultiplier);
                    attempt++;
                    continue; // Passer à la prochaine tentative
                } else {
                    // Max tentatives atteint
                    rowErrors.add(new RowError(rowIndex, row, errorMsg, e, attempt));
                    return null;
                }
            }
        }
        
        // Ce code ne devrait jamais être atteint
        return null;
    }
    
    /**
     * Détermine si un code de statut HTTP mérite un réessai.
     * 
     * @param statusCode Code de statut HTTP
     * @return true si l'erreur est temporaire et mérite un réessai
     */
    private boolean isRetryableStatusCode(int statusCode) {
        return RETRYABLE_STATUS_CODES.contains(statusCode);
    }
    
    /**
     * Méthode utilitaire pour attendre un certain temps.
     * 
     * @param milliseconds Temps à attendre en millisecondes
     */
    private void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interruption pendant l'attente avant réessai");
        }
    }
    
    /**
     * Extrait un identifiant de la ligne pour faciliter les logs et le suivi.
     * Essaie de trouver un champ 'id', 'ID', 'uuid', ou similaire.
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