package com.etljobs.sql2json2api.service.template;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.etljobs.sql2json2api.exception.TemplateProcessingException;
import com.etljobs.sql2json2api.model.ApiEndpointInfo;
import com.etljobs.sql2json2api.model.ApiTemplateResult;
import com.etljobs.sql2json2api.util.ResourceLoader;

import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;

/**
 * Service principal pour le traitement des templates. Cette classe orchestrate
 * les différentes étapes du traitement des templates, mais délègue les
 * responsabilités spécifiques à des classes spécialisées.
 */
@Service
@Slf4j
public class TemplateProcessingService {

    private final TemplateLoader templateLoader;
    private final TemplateRenderer templateRenderer;
    private final TemplateMetadataService metadataService;
    private final PlaceholderProcessor placeholderProcessor;
    private final Configuration freemarkerConfiguration;

    @Value("${app.template.directory}")
    private String templateDirectory;

    @Value("${app.template.use-external-path:false}")
    private boolean useExternalPath;

    public TemplateProcessingService(
            TemplateLoader templateLoader,
            TemplateRenderer templateRenderer,
            TemplateMetadataService metadataService,
            PlaceholderProcessor placeholderProcessor,
            Configuration freemarkerConfiguration) {
        this.templateLoader = templateLoader;
        this.templateRenderer = templateRenderer;
        this.metadataService = metadataService;
        this.placeholderProcessor = placeholderProcessor;
        this.freemarkerConfiguration = freemarkerConfiguration;
    }

    /**
     * Traite un template avec une ligne de données.
     *
     * @param templateName Le nom du template à traiter
     * @param rowData Les données d'une ligne à utiliser pour le traitement
     * @return ApiTemplateResult contenant le JSON généré et les informations
     * d'API
     * @throws TemplateProcessingException Si une erreur survient pendant le
     * traitement
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

            // 5. Rendre le template
            String jsonPayload;

            // Pour compatibilité avec les chemins externes
            if (useExternalPath) {
                // Charger et traiter le template externe directement
                try {
                    String path = templateDirectory + "/" + templateName;
                    org.springframework.core.io.Resource resource = ResourceLoader.getResource(path, true);

                    if (!resource.exists()) {
                        throw new TemplateProcessingException("Le template n'existe pas: " + templateName);
                    }

                    // Créer un template à partir du contenu
                    String templateString = new String(resource.getInputStream().readAllBytes());
                    Template template = new Template(templateName, templateString, freemarkerConfiguration);

                    // Traiter le template
                    java.io.StringWriter writer = new java.io.StringWriter();
                    template.process(dataModel, writer);
                    jsonPayload = writer.toString();
                } catch (Exception e) {
                    throw new TemplateProcessingException("Erreur lors du rendu du template externe " + templateName, e);
                }
            } else {
                // Utiliser le templateRenderer normal (classpath)
                jsonPayload = templateRenderer.renderTemplate(templateName, dataModel);
            }

            // 6. Créer et retourner le résultat
            return new ApiTemplateResult(jsonPayload, endpointInfo);

        } catch (Exception e) {
            log.error("Erreur lors du traitement du template {}: {}", templateName, e.getMessage(), e);
            throw new TemplateProcessingException("Erreur lors du traitement du template: " + templateName, e);
        }
    }
}
