package com.etljobs.sql2json2api.service.orchestration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.etljobs.sql2json2api.exception.ProcessingException;
import com.etljobs.sql2json2api.model.ApiResponse;
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
            
            // Pour cette première étape, nous retournons simplement une liste vide
            // Les étapes suivantes enrichiront cette méthode
            return new ArrayList<>();
            
        } catch (Exception e) {
            log.error("Erreur lors du traitement du fichier SQL: {}", sqlFileName, e);
            throw new ProcessingException("Erreur lors du traitement du fichier SQL: " + sqlFileName, e);
        }
    }
}