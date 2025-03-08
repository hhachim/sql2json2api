package com.etljobs.sql2json2api.service.orchestration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.etljobs.sql2json2api.exception.ProcessingException;
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
            
            // Si aucun résultat, retourner une liste vide
            if (results.isEmpty()) {
                log.info("Aucun résultat à traiter pour ce fichier SQL");
                return responses;
            }
            
            // 4. Générer le token d'authentification (une seule fois pour tous les appels)
            String token = tokenService.getToken();
            log.debug("Token d'authentification généré");
            
            // 5. Traiter chaque ligne de résultat
            int totalRows = results.size();
            log.info("Début du traitement des {} lignes de résultat", totalRows);
            
            for (int i = 0; i < totalRows; i++) {
                Map<String, Object> row = results.get(i);
                log.debug("Traitement de la ligne {}/{}: {}", i + 1, totalRows, row);
                
                try {
                    // Traiter le template pour cette ligne
                    ApiTemplateResult templateResult = templateService.processTemplate(
                            sqlFile.getTemplateName(), row);
                    
                    // Faire l'appel API pour cette ligne
                    ApiResponse response = apiClientService.callApi(
                            templateResult.getEndpointInfo().getRoute(),
                            templateResult.getEndpointInfo().getMethod(),
                            templateResult.getJsonPayload(),
                            templateResult.getEndpointInfo().getHeaders(),
                            templateResult.getEndpointInfo().getUrlParams());
                    
                    log.debug("Appel API effectué pour la ligne {}, statut: {}", i + 1, response.getStatusCode());
                    
                    // Ajouter la réponse à la liste
                    responses.add(response);
                    
                    // Si c'est un paquet de 10 lignes, on fait un log
                    if ((i + 1) % 10 == 0) {
                        log.info("Progression: {}/{} lignes traitées", i + 1, totalRows);
                    }
                    
                } catch (Exception e) {
                    // Gestion des erreurs au niveau de la ligne
                    log.error("Erreur lors du traitement de la ligne {}: {}", i + 1, e.getMessage());
                    // On continue avec la prochaine ligne plutôt que d'arrêter tout le processus
                }
            }
            
            log.info("Traitement terminé: {}/{} lignes traitées avec succès", responses.size(), totalRows);
            return responses;
            
        } catch (Exception e) {
            log.error("Erreur lors du traitement du fichier SQL: {}", sqlFileName, e);
            throw new ProcessingException("Erreur lors du traitement du fichier SQL: " + sqlFileName, e);
        }
    }
}