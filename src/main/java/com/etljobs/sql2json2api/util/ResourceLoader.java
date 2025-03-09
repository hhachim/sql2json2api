package com.etljobs.sql2json2api.util;

import java.io.IOException;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * Utilitaire pour charger des ressources depuis le classpath ou le système de fichiers.
 */
public class ResourceLoader {

    /**
     * Charge une ressource depuis le classpath ou le système de fichiers.
     *
     * @param path Le chemin de la ressource
     * @param useExternalPath true pour utiliser un chemin externe, false pour utiliser le classpath
     * @return La ressource
     */
    public static Resource getResource(String path, boolean useExternalPath) {
        if (useExternalPath) {
            return new FileSystemResource(path);
        } else {
            return new ClassPathResource(path);
        }
    }

    /**
     * Liste les ressources correspondant à un pattern.
     *
     * @param locationPattern Le pattern de localisation
     * @param useExternalPath true pour utiliser un chemin externe, false pour utiliser le classpath
     * @return Un tableau de ressources
     * @throws IOException Si une erreur survient
     */
    public static Resource[] listResources(String locationPattern, boolean useExternalPath) throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        
        if (useExternalPath) {
            // Pour un chemin externe, on utilise file:
            return resolver.getResources("file:" + locationPattern);
        } else {
            // Pour un classpath, on utilise classpath:
            return resolver.getResources("classpath:" + locationPattern);
        }
    }
}