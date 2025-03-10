package com.etljobs.sql2json2api.util;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Utilitaire pour résoudre les chemins relatifs par rapport au répertoire de configuration.
 * Permet de supporter des chemins relatifs à l'emplacement spécifié par spring.config.location.
 */
@Component
@Slf4j
public class PathResolver {

    private final Environment environment;
    private final String configLocation;
    
    public PathResolver(Environment environment) {
        this.environment = environment;
        // Récupérer la valeur de spring.config.location
        this.configLocation = environment.getProperty("spring.config.location");
        log.info("Configuration location: {}", configLocation);
    }
    
    /**
     * Résout un chemin qui peut être absolu ou relatif.
     * Si le chemin est relatif et spring.config.location est défini, 
     * le chemin sera résolu par rapport à ce répertoire.
     * 
     * @param path Le chemin à résoudre
     * @return Le chemin absolu résolu
     */
    public String resolvePath(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }
        
        // Si le chemin est déjà absolu, le retourner tel quel
        if (new File(path).isAbsolute()) {
            log.debug("Path is already absolute: {}", path);
            return path;
        }
        
        // Si spring.config.location n'est pas défini, retourner le chemin tel quel
        if (configLocation == null || configLocation.isEmpty()) {
            log.debug("No config location set, keeping path as is: {}", path);
            return path;
        }
        
        // Supprimer le préfixe "file:" s'il existe
        String baseDir = configLocation;
        if (baseDir.startsWith("file:")) {
            baseDir = baseDir.substring(5);
        }
        
        // S'assurer que le chemin de base est un répertoire
        File baseDirFile = new File(baseDir);
        if (baseDirFile.isFile()) {
            // Si c'est un fichier, prendre son répertoire parent
            baseDirFile = baseDirFile.getParentFile();
        }
        
        // Résoudre le chemin relatif par rapport au répertoire de base
        Path resolvedPath = Paths.get(baseDirFile.getAbsolutePath(), path);
        String result = resolvedPath.toString();
        
        log.debug("Resolved relative path '{}' to absolute path '{}' using base '{}'", 
                 path, result, baseDirFile.getAbsolutePath());
        
        return result;
    }
}