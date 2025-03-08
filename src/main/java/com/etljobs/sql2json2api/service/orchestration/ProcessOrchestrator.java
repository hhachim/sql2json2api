package com.etljobs.sql2json2api.service.orchestration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
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

@Service
@Slf4j
public class ProcessOrchestrator {
    
    private final SqlFileService sqlFileService;
    private final SqlExecutionService sqlExecutionService;
    private final TemplateProcessingService templateService;
    private final TokenService tokenService;
    private final ApiClientService apiClientService;
    
    // Structure pour stocker les détails d'erreur par ligne
    private static class RowError {
        private final int rowIndex;
        private final Map<String, Object> rowData;
        private final String errorMessage;
        private final Exception exception;
        
        public RowError(int rowIndex, Map<String, Object> rowData, String errorMessage, Exception exception) {
            this.rowIndex = rowIndex;
            this.rowData = rowData;
            this.errorMessage = errorMessage;
            this.exception = exception;
        }
        
        @Override
        public String toString() {
            return String.format("Erreur à la ligne %d: %s", rowIndex + 1, errorMessage);
        }
    }
    
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
     * de résultat pour générer des appels API.
     * 
     * @param sqlFileName Nom du fichier SQL à traiter
     * @return Liste des réponses API pour chaque ligne traitée
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
            
            // Collecter les réponses API pour chaque ligne
            List<ApiResponse> responses = new ArrayList<>();
            
            // Collecter les erreurs par ligne
            List<RowError> rowErrors = new ArrayList<>();
            
            // Si aucun résultat, retourner une liste vide
            if (results.isEmpty()) {
                log.info("Aucun résultat à traiter pour ce fichier SQL");
                return responses;
            }
            
            // Générer le token d'authentification (une seule fois pour tous les appels)
            String token = tokenService.getToken();
            log.debug("Token d'authentification généré avec succès");
            
            // Traiter chaque ligne de résultat
            int totalRows = results.size();
            log.info("Début du traitement des {} lignes de résultat", totalRows);
            
            for (int i = 0; i < totalRows; i++) {
                Map<String, Object> row = results.get(i);
                log.debug("Traitement de la ligne {}/{}", i + 1, totalRows);
                
                try {
                    // Extraire l'ID ou un identifiant unique pour faciliter le suivi
                    String rowIdentifier = extractRowIdentifier(row);
                    log.debug("Traitement de la ligne avec identifiant: {}", rowIdentifier);
                    
                    // Traiter le template pour cette ligne
                    ApiTemplateResult templateResult;
                    try {
                        templateResult = templateService.processTemplate(
                                sqlFile.getTemplateName(), row);
                        log.debug("Template traité avec succès pour la ligne {}", rowIdentifier);
                    } catch (TemplateProcessingException e) {
                        String errorMsg = "Erreur lors du traitement du template pour la ligne " + rowIdentifier;
                        log.error("{}: {}", errorMsg, e.getMessage());
                        rowErrors.add(new RowError(i, row, errorMsg, e));
                        continue; // Passer à la ligne suivante
                    }
                    
                    // Faire l'appel API pour cette ligne
                    ApiResponse response;
                    try {
                        response = apiClientService.callApi(
                                templateResult.getEndpointInfo().getRoute(),
                                templateResult.getEndpointInfo().getMethod(),
                                templateResult.getJsonPayload(),
                                templateResult.getEndpointInfo().getHeaders(),
                                templateResult.getEndpointInfo().getUrlParams());
                        
                        log.debug("Appel API effectué pour la ligne {}, statut: {}", 
                                rowIdentifier, response.getStatusCode());
                        
                        // Vérifier si l'appel a réussi (code 2xx)
                        if (!response.isSuccess()) {
                            String errorMsg = "L'API a répondu avec un code d'erreur " + response.getStatusCode() + 
                                    " pour la ligne " + rowIdentifier;
                            log.warn("{}: {}", errorMsg, response.getBody());
                            // On ajoute quand même la réponse, mais on enregistre l'erreur
                            rowErrors.add(new RowError(i, row, errorMsg, null));
                        }
                    } catch (ApiCallException e) {
                        String errorMsg = "Erreur lors de l'appel API pour la ligne " + rowIdentifier;
                        log.error("{}: {}", errorMsg, e.getMessage());
                        rowErrors.add(new RowError(i, row, errorMsg, e));
                        // On crée une réponse d'erreur pour garder trace de cette ligne dans les résultats
                        response = ApiResponse.builder()
                                .statusCode(500)
                                .body("Erreur d'appel API: " + e.getMessage())
                                .build();
                    }
                    
                    // Ajouter la réponse à la liste
                    responses.add(response);
                    
                    // Si c'est un paquet de 10 lignes, on fait un log
                    if ((i + 1) % 10 == 0 || i == totalRows - 1) {
                        log.info("Progression: {}/{} lignes traitées ({} erreurs)", 
                                i + 1, totalRows, rowErrors.size());
                    }
                    
                } catch (Exception e) {
                    // Capture des erreurs non spécifiques au niveau de la ligne
                    String errorMsg = "Erreur inattendue lors du traitement de la ligne " + (i + 1);
                    log.error("{}: {}", errorMsg, e.getMessage(), e);
                    rowErrors.add(new RowError(i, row, errorMsg, e));
                    
                    // Continuer avec la prochaine ligne
                }
            }
            
            // Rapport de fin de traitement
            int successCount = totalRows - rowErrors.size();
            log.info("Traitement terminé: {}/{} lignes traitées avec succès ({} erreurs)", 
                    successCount, totalRows, rowErrors.size());
            
            // Si des erreurs ont été détectées, les journaliser de manière détaillée
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