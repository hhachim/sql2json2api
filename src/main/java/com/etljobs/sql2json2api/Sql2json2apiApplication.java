package com.etljobs.sql2json2api;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@Slf4j
public class Sql2json2apiApplication {

    public static void main(String[] args) {
        // Charger le fichier .env
        loadDotEnv();
        
        // Démarrer l'application Spring Boot normalement
        SpringApplication.run(Sql2json2apiApplication.class, args);
    }
    
    /**
     * Charge les variables d'environnement depuis un fichier .env spécifique
     */
    private static void loadDotEnv() {
        // Obtenir le chemin complet du fichier .env depuis la variable d'environnement
        String envFilePath = System.getenv("ENV_FILE_PATH");
        
        if (envFilePath == null || envFilePath.isEmpty()) {
            log.info("ENV_FILE_PATH non définie, utilisation du fichier .env par défaut");
            // Utiliser .env par défaut dans le répertoire courant
            envFilePath = ".env";
        } else {
            log.info("Utilisation du fichier env: {}", envFilePath);
        }
        
        // Vérifier que le fichier existe
        Path envPath = Paths.get(envFilePath);
        if (!Files.exists(envPath)) {
            log.warn("Fichier env non trouvé: {}", envFilePath);
            return;
        }
        
        try {
            // Récupérer le répertoire et le nom du fichier
            String directory = envPath.getParent() != null ? 
                               envPath.getParent().toString() : ".";
            String filename = envPath.getFileName().toString();
            
            // Configurer et charger dotenv avec le fichier spécifique
            Dotenv dotenv = Dotenv.configure()
                    .directory(directory)
                    .filename(filename)
                    .ignoreIfMissing()
                    .load();
            
            // Si des variables ont été chargées
            if (!dotenv.entries().isEmpty()) {
                log.info("{} variables chargées depuis {}", dotenv.entries().size(), envFilePath);
                
                // Copier les variables dans les propriétés système
                dotenv.entries().forEach(entry -> {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    
                    // Ne pas afficher les mots de passe dans les logs
                    if (key.toLowerCase().contains("password") || key.toLowerCase().contains("secret")) {
                        log.debug("Variable d'environnement chargée: {} = ******", key);
                    } else {
                        log.debug("Variable d'environnement chargée: {} = {}", key, value);
                    }
                    
                    // Définir comme propriété système si elle n'existe pas déjà
                    if (System.getProperty(key) == null) {
                        System.setProperty(key, value);
                    }
                });
            } else {
                log.info("Aucune variable chargée depuis {}", envFilePath);
            }
        } catch (Exception e) {
            log.warn("Erreur lors du chargement du fichier env: {}", e.getMessage());
        }
    }
}