package com.etljobs.sql2json2api.config;

import java.io.File;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.etljobs.sql2json2api.util.PathResolver;

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
    
    @Value("${app.template.external-directory:}")
    private String externalTemplateDirectory;
    
    private final PathResolver pathResolver;
    
    @Autowired
    public FreemarkerConfig(PathResolver pathResolver) {
        this.pathResolver = pathResolver;
    }
    
    /**
     * Configures the Freemarker template engine.
     * 
     * @return Configured Freemarker Configuration
     */
    @Bean
    public freemarker.template.Configuration freemarkerConfiguration() {
        freemarker.template.Configuration configuration = new freemarker.template.Configuration(freemarker.template.Configuration.VERSION_2_3_32);
        
        // Set template configuration
        configuration.setDefaultEncoding("UTF-8");
        configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        configuration.setLogTemplateExceptions(false);
        configuration.setWrapUncheckedExceptions(true);
        
        // Résoudre le chemin externe s'il est spécifié
        String resolvedExternalDir = pathResolver.resolvePath(externalTemplateDirectory);
        
        // Configure template loading strategy
        if (resolvedExternalDir != null && !resolvedExternalDir.isEmpty()) {
            try {
                log.info("Using external template directory: {}", resolvedExternalDir);
                File directory = new File(resolvedExternalDir);
                if (!directory.exists() || !directory.isDirectory()) {
                    log.warn("External template directory does not exist or is not a directory: {}", resolvedExternalDir);
                    log.warn("Falling back to classpath templates");
                    configuration.setClassLoaderForTemplateLoading(getClass().getClassLoader(), templateDirectory);
                } else {
                    configuration.setDirectoryForTemplateLoading(directory);
                }
            } catch (IOException e) {
                log.error("Failed to set external template directory: {}", resolvedExternalDir, e);
                log.warn("Falling back to classpath templates");
                configuration.setClassLoaderForTemplateLoading(getClass().getClassLoader(), templateDirectory);
            }
        } else {
            // Load templates from classpath (default behavior)
            log.info("Using classpath template directory: {}", templateDirectory);
            configuration.setClassLoaderForTemplateLoading(getClass().getClassLoader(), templateDirectory);
        }
        
        return configuration;
    }
}