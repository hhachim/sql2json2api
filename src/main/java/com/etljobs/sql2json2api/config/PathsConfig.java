package com.etljobs.sql2json2api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "app.paths")
@Getter
@Setter
public class PathsConfig {
    
    /**
     * Mode de résolution des chemins.
     * Valeurs possibles: classpath, absolute, relative
     */
    private String resolutionMode = "classpath";
    
    /**
     * Répertoire contenant les fichiers SQL
     */
    private String sqlDirectory = "sql";
    
    /**
     * Répertoire contenant les templates Freemarker
     */
    private String templateDirectory = "templates/json";
    
    /**
     * Chemin vers le template d'authentification
     */
    private String authTemplatePath = "auth/auth-payload.ftlh";
    
    /**
     * Résout un chemin selon le mode de résolution configuré
     * @param path Chemin relatif à résoudre
     * @return Chemin résolu complet
     */
    public String resolvePath(String path) {
        switch (resolutionMode.toLowerCase()) {
            case "absolute":
                return path; // Chemin absolu, utilisé tel quel
            case "relative":
                // Si spring.config.location est défini, le préfixer
                String configLocation = System.getProperty("spring.config.location", "");
                if (!configLocation.isEmpty() && !configLocation.endsWith("/")) {
                    configLocation += "/";
                }
                return configLocation + path;
            case "classpath":
            default:
                return "classpath:" + path;
        }
    }
    
    /**
     * Résout le chemin du répertoire SQL
     */
    public String resolvedSqlDirectory() {
        return resolvePath(sqlDirectory);
    }
    
    /**
     * Résout le chemin du répertoire des templates
     */
    public String resolvedTemplateDirectory() {
        return resolvePath(templateDirectory);
    }
    
    /**
     * Résout le chemin du template d'authentification
     */
    public String resolvedAuthTemplatePath() {
        return resolvePath(authTemplatePath);
    }
}