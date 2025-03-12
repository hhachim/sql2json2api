package com.etljobs.sql2json2api.service.orchestration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.etljobs.sql2json2api.exception.ProcessingException;
import com.etljobs.sql2json2api.model.ApiResponse;
import com.etljobs.sql2json2api.model.SqlFile;
import com.etljobs.sql2json2api.service.http.TokenService;
import com.etljobs.sql2json2api.service.sql.SqlExecutionService;
import com.etljobs.sql2json2api.service.sql.SqlFileService;

import lombok.extern.slf4j.Slf4j;

/**
 * Orchestrateur du processus global de traitement des fichiers SQL
 * vers des appels API.
 * 
 * Cette classe implémente le flux complet de traitement:
 * - Lecture et exécution du fichier SQL
 * - Traitement des lignes de résultat
 * - Délégation de la transformation en JSON et des appels API au RowProcessor
 * - Gestion globale des erreurs et des résultats
 */
@Service
@Slf4j
public class ProcessOrchestrator {
    
    private final SqlFileService sqlFileService;
    private final SqlExecutionService sqlExecutionService;
    private final TokenService tokenService;
    private final RetryStrategyFactory retryStrategyFactory;
    private final RowProcessor rowProcessor;
    
    /**
     * Constructeur avec injection de dépendances.
     */
    @Autowired
    public ProcessOrchestrator(
            SqlFileService sqlFileService,
            SqlExecutionService sqlExecutionService,
            TokenService tokenService,
            RetryStrategyFactory retryStrategyFactory,
            RowProcessor rowProcessor) {
        this.sqlFileService = sqlFileService;
        this.sqlExecutionService = sqlExecutionService;
        this.tokenService = tokenService;
        this.retryStrategyFactory = retryStrategyFactory;
        this.rowProcessor = rowProcessor;
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
            
            // 5. Créer une instance de la stratégie de réessai
            RetryStrategy retryStrategy = retryStrategyFactory.create();
            
            // 6. Traiter chaque ligne de résultat
            int totalRows = results.size();
            log.info("Début du traitement des {} lignes de résultat", totalRows);
            
            for (int i = 0; i < totalRows; i++) {
                Map<String, Object> row = results.get(i);
                String rowIdentifier = extractRowIdentifier(row);
                log.debug("Traitement de la ligne {}/{}: {}", i + 1, totalRows, rowIdentifier);
                
                // Déléguer le traitement de cette ligne au RowProcessor
                ApiResponse response = rowProcessor.processRow(
                        sqlFile, row, i, rowIdentifier, retryStrategy, rowErrors);
                
                // Important: ajouter la réponse à la liste même si elle contient une erreur
                if (response != null) {
                    responses.add(response);
                }
            }
            
            log.info("Traitement terminé: {}/{} lignes traitées avec succès ({} erreurs)", 
                    responses.size(), totalRows, rowErrors.size());
            
            // 7. Si des erreurs se sont produites, les journaliser de manière détaillée
            if (!rowErrors.isEmpty()) {
                logRowErrors(rowErrors);
            }
            
        } catch (Exception e) {
            log.error("Erreur globale lors du traitement du fichier SQL: {}", sqlFileName, e);
            throw new ProcessingException("Erreur lors du traitement du fichier SQL: " + sqlFileName, e);
        }
        
        return responses;
    }
    
    /**
     * Journalise en détail les erreurs survenues par ligne.
     * 
     * @param rowErrors Liste des erreurs par ligne
     */
    private void logRowErrors(List<RowError> rowErrors) {
        log.error("Résumé des {} erreurs survenues pendant le traitement:", rowErrors.size());
        for (int i = 0; i < rowErrors.size(); i++) {
            RowError error = rowErrors.get(i);
            log.error("  {}. {}", i + 1, error.getFormattedMessage());
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