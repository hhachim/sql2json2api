package com.etljobs.sql2json2api.service.template;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.etljobs.sql2json2api.exception.TemplateProcessingException;
import com.etljobs.sql2json2api.model.ApiEndpointInfo;
import com.etljobs.sql2json2api.model.ApiTemplateResult;

import lombok.extern.slf4j.Slf4j;

/**
 * Service principal pour le traitement des templates.
 * Cette classe orchestrate les différentes étapes du traitement des templates,
 * mais délègue les responsabilités spécifiques à des classes spécialisées.
 */
@Service
@Slf4j
public class TemplateProcessingService {
    
    private final TemplateLoader templateLoader;
    private final TemplateRenderer templateRenderer;
    private final TemplateMetadataService metadataService;
    private final PlaceholderProcessor placeholderProcessor;
    
    public TemplateProcessingService(
            TemplateLoader templateLoader,
            TemplateRenderer templateRenderer,
            TemplateMetadataService metadataService,
            PlaceholderProcessor placeholderProcessor) {
        this.templateLoader = templateLoader;
        this.templateRenderer = templateRenderer;
        this.metadataService = metadataService;
        this.placeholderProcessor = placeholderProcessor;
    }
    
    /**
     * Traite un template avec une ligne de données.
     * 
     * @param templateName Le nom du template à traiter
     * @param rowData Les données d'une ligne à utiliser pour le traitement
     * @return ApiTemplateResult contenant le JSON généré et les informations d'API
     * @throws TemplateProcessingException Si une erreur survient pendant le traitement
     */
    public ApiTemplateResult processTemplate(String templateName, Map<String, Object> rowData) {
        try {
            log.info("Traitement du template {} pour une ligne de données", templateName);
            
            // 1. Charger le contenu du template
            String templateContent = templateLoader.loadTemplateContent(templateName);
            
            // 2. Extraire les métadonnées d'API
            ApiEndpointInfo endpointInfo = metadataService.extractMetadataFromTemplate(templateContent);
            
            // 3. Traiter les placeholders dans la route
            String processedRoute = placeholderProcessor.processPlaceholders(
                    endpointInfo.getRoute(), rowData);
            endpointInfo.setRoute(processedRoute);
            
            // 4. Préparer le modèle de données pour le rendu
            Map<String, Object> dataModel = new HashMap<>();
            dataModel.put("result", rowData);
            
            // 5. Rendre le template pour obtenir le JSON
            String jsonPayload = templateRenderer.renderTemplate(templateName, dataModel);
            
            // 6. Créer et retourner le résultat
            return new ApiTemplateResult(jsonPayload, endpointInfo);
            
        } catch (Exception e) {
            log.error("Erreur lors du traitement du template {}: {}", templateName, e.getMessage());
            throw new TemplateProcessingException("Erreur lors du traitement du template: " + templateName, e);
        }
    }
}