package com.etljobs.sql2json2api.service.template;

import java.io.StringWriter;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.etljobs.sql2json2api.exception.TemplateProcessingException;

import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;

/**
 * Service responsable du rendu des templates Freemarker.
 * Cette classe est extraite de TemplateProcessingService pour séparer
 * la responsabilité du rendu des templates.
 */
@Service
@Slf4j
public class TemplateRenderer {

    private final Configuration freemarkerConfig;
    
    public TemplateRenderer(Configuration freemarkerConfig) {
        this.freemarkerConfig = freemarkerConfig;
    }
    
    /**
     * Effectue le rendu d'un template avec les données fournies.
     * 
     * @param templateName Le nom du template à rendre
     * @param dataModel Le modèle de données à utiliser pour le rendu
     * @return Le contenu rendu
     * @throws TemplateProcessingException Si une erreur survient pendant le rendu
     */
    public String renderTemplate(String templateName, Map<String, Object> dataModel) {
        try {
            log.debug("Rendu du template {} avec {} variables de données", 
                    templateName, dataModel.size());
            
            // Récupérer le template depuis la configuration Freemarker
            Template template = freemarkerConfig.getTemplate(templateName);
            
            // Effectuer le rendu dans un StringWriter
            StringWriter writer = new StringWriter();
            template.process(dataModel, writer);
            
            // Récupérer le résultat
            String result = writer.toString();
            log.debug("Rendu terminé, taille du résultat: {} caractères", result.length());
            
            return result;
            
        } catch (Exception e) {
            throw new TemplateProcessingException("Erreur lors du rendu du template " + templateName, e);
        }
    }
}