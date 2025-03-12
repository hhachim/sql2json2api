package com.etljobs.sql2json2api.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "app.sql")
@Getter
@Setter
public class SqlConfig {
    
    /**
     * Directory where SQL files are stored.
     */
    private String directory;
    
    /**
     * Liste des fichiers SQL à exécuter dans l'ordre spécifié.
     * Si la liste est vide, tous les fichiers sont exécutés.
     */
    private List<String> executionOrder = new ArrayList<>();
}