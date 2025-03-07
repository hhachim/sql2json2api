package com.etljobs.sql2json2api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import freemarker.template.TemplateExceptionHandler;

/**
 * Configuration for Freemarker template engine.
 */
@Configuration
public class FreemarkerConfig {
    
    @Value("${app.template.directory}")
    private String templateDirectory;
    
    /**
     * Configures the Freemarker template engine.
     * 
     * @return Configured Freemarker Configuration
     */
    @Bean
    public freemarker.template.Configuration freemarkerConfiguration() {
        freemarker.template.Configuration configuration = new freemarker.template.Configuration(freemarker.template.Configuration.VERSION_2_3_32);
        
        // Load templates from classpath
        configuration.setClassLoaderForTemplateLoading(getClass().getClassLoader(), templateDirectory);
        
        // Set template configuration
        configuration.setDefaultEncoding("UTF-8");
        configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        configuration.setLogTemplateExceptions(false);
        configuration.setWrapUncheckedExceptions(true);
        
        return configuration;
    }
}