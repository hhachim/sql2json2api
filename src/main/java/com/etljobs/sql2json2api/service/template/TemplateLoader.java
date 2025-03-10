package com.etljobs.sql2json2api.service.template;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import com.etljobs.sql2json2api.exception.TemplateProcessingException;
import com.etljobs.sql2json2api.util.PathResolver;

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
    
    @Value("${app.template.external-directory:}")
    private String externalTemplateDirectory;
    
    @Autowired
    private PathResolver pathResolver;
    
    /**
     * Charge le contenu d'un template à partir de son nom.
     * 
     * @param templateName Le nom du template à charger
     * @return Le contenu du template sous forme de chaîne de caractères
     * @throws TemplateProcessingException Si le template ne peut pas être chargé
     */
    public String loadTemplateContent(String templateName) {
        // Résoudre le chemin externe s'il est spécifié
        String resolvedExternalDir = pathResolver.resolvePath(externalTemplateDirectory);
        
        // First try to load from external directory if configured
        if (resolvedExternalDir != null && !resolvedExternalDir.isEmpty()) {
            File file = new File(resolvedExternalDir, templateName);
            if (file.exists() && file.isFile()) {
                try {
                    log.debug("Loading template from external directory: {}", templateName);
                    return Files.readString(file.toPath(), StandardCharsets.UTF_8);
                } catch (IOException e) {
                    log.warn("Failed to read external template: {}", templateName, e);
                    // Fall through to try classpath
                }
            }
        }
        
        // Fall back to classpath
        try {
            log.debug("Loading template from classpath: {}", templateName);
            Resource resource = new ClassPathResource(buildTemplatePath(templateName));
            
            // Vérifier si la ressource existe
            if (!resource.exists()) {
                throw new TemplateProcessingException("Le template n'existe pas: " + templateName);
            }
            
            // Lire le contenu du template
            byte[] bytes = FileCopyUtils.copyToByteArray(resource.getInputStream());
            String content = new String(bytes, StandardCharsets.UTF_8);
            
            log.debug("Template loaded successfully, size: {} bytes", content.length());
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
        // Résoudre le chemin externe s'il est spécifié
        String resolvedExternalDir = pathResolver.resolvePath(externalTemplateDirectory);
        
        // First check external directory if configured
        if (resolvedExternalDir != null && !resolvedExternalDir.isEmpty()) {
            File file = new File(resolvedExternalDir, templateName);
            if (file.exists() && file.isFile()) {
                return true;
            }
        }
        
        // Then check classpath
        try {
            Resource resource = new ClassPathResource(buildTemplatePath(templateName));
            return resource.exists();
        } catch (Exception e) {
            log.warn("Error while checking template existence: {}", templateName, e);
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