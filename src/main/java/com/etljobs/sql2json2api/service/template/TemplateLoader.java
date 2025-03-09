package com.etljobs.sql2json2api.service.template;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import com.etljobs.sql2json2api.config.PathsConfig;
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
    
    private final PathsConfig pathsConfig;
    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    
    @Autowired
    public TemplateLoader(PathsConfig pathsConfig) {
        this.pathsConfig = pathsConfig;
    }
    
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
            String resolvedPath = buildTemplatePath(templateName);
            Resource resource = resolver.getResource(resolvedPath);
            
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
     * Vérifie si un template existe physiquement.
     * 
     * @param templateName Le nom du template à vérifier
     * @return true si le template existe, false sinon
     */
    public boolean templateExists(String templateName) {
        try {
            String resolvedPath = buildTemplatePath(templateName);
            Resource resource = resolver.getResource(resolvedPath);
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
        String templateDir = pathsConfig.resolvedTemplateDirectory();
        // Si le chemin résolu inclut déjà le préfixe classpath:, ne pas l'ajouter à nouveau dans le chemin
        if (templateDir.startsWith("classpath:")) {
            return templateDir + "/" + templateName;
        } else {
            // Dans ce cas, nous utilisons un chemin absolu ou relatif
            return templateDir + "/" + templateName;
        }
    }
}