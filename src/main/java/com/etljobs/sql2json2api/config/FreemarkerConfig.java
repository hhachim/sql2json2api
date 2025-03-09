package com.etljobs.sql2json2api.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import freemarker.template.TemplateExceptionHandler;

/**
 * Configuration for Freemarker template engine.
 */
@Configuration
public class FreemarkerConfig {
    
    private final PathsConfig pathsConfig;
    
    @Autowired
    public FreemarkerConfig(PathsConfig pathsConfig) {
        this.pathsConfig = pathsConfig;
    }
    
    /**
     * Configures the Freemarker template engine.
     * 
     * @return Configured Freemarker Configuration
     */
    @Bean
    public freemarker.template.Configuration freemarkerConfiguration() {
        freemarker.template.Configuration configuration = new freemarker.template.Configuration(freemarker.template.Configuration.VERSION_2_3_32);
        
        // Load templates according to the configured resolution mode
        if (pathsConfig.getResolutionMode().equalsIgnoreCase("classpath")) {
            // For classpath mode, we use ClassLoaderForTemplateLoading
            configuration.setClassLoaderForTemplateLoading(getClass().getClassLoader(), pathsConfig.getTemplateDirectory());
        } else {
            // For absolute or relative paths, we use FileTemplateLoader
            try {
                // We don't need to use resolvedTemplateDirectory here since we're just setting the base directory
                // for the template loader, and actual template paths will be resolved relative to this directory
                configuration.setDirectoryForTemplateLoading(new java.io.File(pathsConfig.getTemplateDirectory()));
            } catch (java.io.IOException e) {
                throw new RuntimeException("Failed to set template directory: " + pathsConfig.getTemplateDirectory(), e);
            }
        }
        
        // Set template configuration
        configuration.setDefaultEncoding("UTF-8");
        configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        configuration.setLogTemplateExceptions(false);
        configuration.setWrapUncheckedExceptions(true);
        
        return configuration;
    }
}