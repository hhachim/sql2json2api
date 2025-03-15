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
            if (!Files.exists(path)) {
                log.info("Création du répertoire de logs: {}", logFilePath);
                Files.createDirectories(path);
            } else {
                log.debug("Le répertoire de logs existe déjà: {}", logFilePath);
            }
        } catch (Exception e) {
            log.warn("Impossible de créer le répertoire de logs {}: {}", logFilePath, e.getMessage());
        }
    }
}