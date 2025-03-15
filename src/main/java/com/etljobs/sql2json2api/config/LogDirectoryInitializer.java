package com.etljobs.sql2json2api.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class LogDirectoryInitializer {

    @Value("${logging.file.path:./logs}")
    private String logFilePath;

    @EventListener(ApplicationStartedEvent.class)
    public void initializeLogDirectory() {
        try {
            Path path = Paths.get(logFilePath);
            System.out.println("Tentative de création du répertoire de logs: " + path.toAbsolutePath());
            if (!Files.exists(path)) {
                log.info("Création du répertoire de logs: {}", path.toAbsolutePath());
                Files.createDirectories(path);
            } else {
                log.debug("Le répertoire de logs existe déjà: {}", path.toAbsolutePath());
            }
            System.out.println("Le répertoire de logs est configuré: " + path.toAbsolutePath());
        } catch (Exception e) {
            System.err.println("Erreur lors de la création du répertoire de logs: " + e.getMessage());
            log.warn("Impossible de créer le répertoire de logs {}: {}", logFilePath, e.getMessage());
        }
    }
}
