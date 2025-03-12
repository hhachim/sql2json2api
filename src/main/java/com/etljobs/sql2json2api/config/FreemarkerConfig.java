package com.etljobs.sql2json2api.config;

import java.io.File;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.etljobs.sql2json2api.util.FileUtils;

import freemarker.template.TemplateExceptionHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * Configuration for Freemarker template engine.
 */
@Configuration
@Slf4j
public class FreemarkerConfig {
    
    @Value("${app.template.directory}")
    private String templateDirectory;
    
    /**
     * Configures the Freemarker template engine.
     * 
     * @return Configured Freemarker Configuration
     */
    @Bean
    public freemarker.template.Configuration freemarkerConfiguration() throws IOException {
        freemarker.template.Configuration configuration = new freemarker.template.Configuration(freemarker.template.Configuration.VERSION_2_3_32);
        
        // Vérifier si le répertoire de templates est un chemin absolu
        if (FileUtils.isAbsolutePath(templateDirectory)) {
            log.info("Configuring Freemarker with absolute directory path: {}", templateDirectory);
            // Utiliser le système de fichiers pour charger les templates
            File templateDir = new File(templateDirectory);
            if (!templateDir.exists() || !templateDir.isDirectory()) {
                log.warn("Template directory does not exist or is not a directory: {}", templateDirectory);
                log.warn("Falling back to classpath loading");
                // Fallback to classpath loading
                configuration.setClassLoaderForTemplateLoading(getClass().getClassLoader(), "templates/json");
            } else {
                // Load templates from file system directory
                configuration.setDirectoryForTemplateLoading(templateDir);
            }
        } else {
            // Original behavior: Load templates from classpath
            log.info("Configuring Freemarker with classpath directory: {}", templateDirectory);
            configuration.setClassLoaderForTemplateLoading(getClass().getClassLoader(), templateDirectory);
        }
        
        // Set template configuration
        configuration.setDefaultEncoding("UTF-8");
        configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        configuration.setLogTemplateExceptions(false);
        configuration.setWrapUncheckedExceptions(true);
        
        return configuration;
    }
}