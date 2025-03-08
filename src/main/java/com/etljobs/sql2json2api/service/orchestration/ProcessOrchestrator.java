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
     * Traite un fichier SQL en exécutant la requête et en préparant la structure
     * pour les futures étapes de traitement.
     * 
     * @param sqlFileName Nom du fichier SQL à traiter
     * @return Liste vide (sera enrichie dans les étapes suivantes)
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
            
            // 4. Traiter uniquement la première ligne pour cette étape
            if (!results.isEmpty()) {
                // 4.1 Obtenir le token d'authentification (une seule fois)
                String token = tokenService.getToken();
                
                // 4.2 Traiter la première ligne
                Map<String, Object> firstRow = results.get(0);
                ApiTemplateResult templateResult = templateService.processTemplate(
                        sqlFile.getTemplateName(), firstRow);
                
                log.debug("Template traité pour la première ligne");
                
                // 4.3 Effectuer l'appel API avec les informations extraites
                ApiResponse response = apiClientService.callApi(
                        templateResult.getEndpointInfo().getRoute(),
                        templateResult.getEndpointInfo().getMethod(),
                        templateResult.getJsonPayload(),
                        templateResult.getEndpointInfo().getHeaders(),
                        templateResult.getEndpointInfo().getUrlParams());
                
                log.info("Appel API effectué pour la première ligne, statut: {}", response.getStatusCode());
                
                // 4.4 Ajouter la réponse à la liste
                responses.add(response);
            }
            
            return responses;
            
        } catch (Exception e) {
            log.error("Erreur lors du traitement du fichier SQL: {}", sqlFileName, e);
            throw new ProcessingException("Erreur lors du traitement du fichier SQL: " + sqlFileName, e);
        }
    }
}