package com.etljobs.sql2json2api.service.threading;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.etljobs.sql2json2api.api.response.ApiResponseAdapter;
import com.etljobs.sql2json2api.exception.ProcessingException;
import com.etljobs.sql2json2api.model.SqlFile;
import com.etljobs.sql2json2api.service.sql.SqlFileService;

import lombok.extern.slf4j.Slf4j;

/**
 * Coordinateur pour le traitement séquentiel des fichiers SQL
 * avec exécution parallèle des appels API au sein de chaque fichier.
 */
@Service
@Slf4j
public class SqlFileSequentialCoordinator {
    
    private final SqlFileService sqlFileService;
    private final SqlBasedParallelApiExecutionService parallelExecutionService;
    private final ApiResponseAdapter responseAdapter;
    
    @Autowired
    public SqlFileSequentialCoordinator(
            SqlFileService sqlFileService,
            SqlBasedParallelApiExecutionService parallelExecutionService,
            ApiResponseAdapter responseAdapter) {
        this.sqlFileService = sqlFileService;
        this.parallelExecutionService = parallelExecutionService;
        this.responseAdapter = responseAdapter;
    }
    
    /**
     * Traite les fichiers SQL dans l'ordre configuré, en assurant que chaque fichier
     * est traité complètement avant de passer au suivant.
     * 
     * @return Résultats des appels API par fichier SQL
     */
    public Map<String, List<com.etljobs.sql2json2api.model.ApiResponse>> processAllSqlFiles() {
        // Obtenir les fichiers SQL dans l'ordre configuré
        List<SqlFile> sqlFiles = sqlFileService.getSqlFilesInConfiguredOrder();
        
        if (sqlFiles.isEmpty()) {
            log.warn("Aucun fichier SQL trouvé à traiter");
            return new HashMap<>();
        }
        
        log.info("Traitement séquentiel de {} fichiers SQL", sqlFiles.size());
        
        // Map pour stocker les résultats par fichier SQL
        Map<String, List<com.etljobs.sql2json2api.model.ApiResponse>> resultsByFile = new HashMap<>();
        
        // Traiter chaque fichier SQL séquentiellement
        for (SqlFile sqlFile : sqlFiles) {
            try {
                log.info("===> Début du traitement du fichier SQL: {}", sqlFile.getFileName());
                
                // Exécuter et attendre que tous les appels API pour ce fichier soient terminés
                ApiCallResults results = parallelExecutionService.executeAndWaitCompletion(sqlFile);
                
                // Convertir les résultats au format legacy
                List<com.etljobs.sql2json2api.model.ApiResponse> legacyResponses = 
                        results.getResponses().stream()
                        .map(responseAdapter::toLegacy)
                        .toList();
                
                // Stocker les résultats dans la map
                resultsByFile.put(sqlFile.getFileName(), legacyResponses);
                
                // Log du résumé
                int successCount = results.getSuccessCount();
                int errorCount = results.getTotalErrorCount();
                
                log.info("===> Fin du traitement de {}: {} succès, {} erreurs", 
                        sqlFile.getFileName(), successCount, errorCount);
                
            } catch (Exception e) {
                log.error("Erreur critique lors du traitement du fichier {}: {}", 
                        sqlFile.getFileName(), e.getMessage(), e);
                
                // Ajouter une entrée vide pour ce fichier
                resultsByFile.put(sqlFile.getFileName(), new ArrayList<>());
                
                // Ne pas interrompre le flux en cas d'erreur sur un fichier
                continue;
            }
        }
        
        log.info("Traitement de tous les fichiers SQL terminé");
        return resultsByFile;
    }
    
    /**
     * Traite un seul fichier SQL spécifié par son nom.
     * 
     * @param sqlFileName Nom du fichier SQL à traiter
     * @return Liste des réponses API
     */
    public List<com.etljobs.sql2json2api.model.ApiResponse> processSingleSqlFile(String sqlFileName) {
        try {
            log.info("Traitement du fichier SQL unique: {}", sqlFileName);
            
            // Lire le fichier SQL
            SqlFile sqlFile = sqlFileService.readSqlFile(sqlFileName);
            
            // Exécuter et attendre les résultats
            ApiCallResults results = parallelExecutionService.executeAndWaitCompletion(sqlFile);
            
            // Convertir et retourner les résultats
            return results.getResponses().stream()
                    .map(responseAdapter::toLegacy)
                    .toList();
            
        } catch (Exception e) {
            throw new ProcessingException("Erreur lors du traitement du fichier SQL: " + sqlFileName, e);
        }
    }
}