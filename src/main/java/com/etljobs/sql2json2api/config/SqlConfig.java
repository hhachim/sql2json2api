package com.etljobs.sql2json2api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

/**
 * Configuration properties for SQL file processing.
 */
@Configuration
@ConfigurationProperties(prefix = "app.sql")
@Getter
@Setter
public class SqlConfig {
    
    /**
     * Directory where SQL files are stored.
     */
    private String directory;
}