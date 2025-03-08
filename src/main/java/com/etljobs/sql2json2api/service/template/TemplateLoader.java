package com.etljobs.sql2json2api.service.template;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import com.etljobs.sql2json2api.exception.TemplateProcessingException;

import lombok.extern.slf4j.Slf4j;

/**
 * Service responsable du chargement des templates Freemarker.
 * Cette classe est extraite de TemplateProcessingService pour respecter
 * le principe de responsabilité unique.
 */
@Service
@Slf4j
public class TemplateLoader {
    
    @Value("${app.template.directory}")
    private String templateDirectory;
    
    /**
     * Charge le contenu d'un template à partir de son nom.
     * 
     * @param templateName Le nom du template à charger
     * @return Le contenu du template sous forme de chaîne de caractères
     * @throws TemplateProcessingException Si le template ne peut pas être chargé
     */
    public String loadTemplateContent(String templateName) {
        try {
            log.debug("Chargement du template: {}", templateName);
            Resource resource = new ClassPathResource(buildTemplatePath(templateName));
            
            // Vérifier si la ressource existe
            if (!resource.exists()) {
                throw new TemplateProcessingException("Le template n'existe pas: " + templateName);
            }
            
            // Lire le contenu du template
            byte[] bytes = FileCopyUtils.copyToByteArray(resource.getInputStream());
            String content = new String(bytes, StandardCharsets.UTF_8);
            
            log.debug("Template chargé avec succès, taille: {} octets", content.length());
            return content;
            
        } catch (IOException e) {
            throw new TemplateProcessingException("Impossible de charger le template: " + templateName, e);
        }
    }
    
    /**
     * Vérifie si un template existe physiquement dans le classpath.
     * 
     * @param templateName Le nom du template à vérifier
     * @return true si le template existe, false sinon
     */
    public boolean templateExists(String templateName) {
        try {
            Resource resource = new ClassPathResource(buildTemplatePath(templateName));
            return resource.exists();
        } catch (Exception e) {
            log.warn("Erreur lors de la vérification de l'existence du template {}: {}", 
                    templateName, e.getMessage());
            return false;
        }
    }
    
    /**
     * Construit le chemin complet d'un template à partir de son nom.
     * 
     * @param templateName Le nom du template
     * @return Le chemin complet du template
     */
    public String buildTemplatePath(String templateName) {
        return templateDirectory + "/" + templateName;
    }
}