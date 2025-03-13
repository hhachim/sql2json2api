package com.etljobs.sql2json2api.runner;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.etljobs.sql2json2api.model.ApiResponse;
import com.etljobs.sql2json2api.service.threading.SqlFileSequentialCoordinator;

import lombok.extern.slf4j.Slf4j;

/**
 * CommandLineRunner pour l'exécution des appels API basés sur les fichiers SQL.
 * Cette version utilise le coordinateur séquentiel pour traiter les fichiers dans l'ordre,
 * avec exécution parallèle des appels API pour chaque fichier si activé.
 */
@Component
@Slf4j
@Profile({"api-call-demo", "dev", "sql2json2api"})
public class ApiCallRunner implements CommandLineRunner, ExitCodeGenerator {

    private final SqlFileSequentialCoordinator coordinator;
    
    @Value("${app.threading.enabled:false}")
    private boolean threadingEnabled;
    
    private int exitCode = 0;

    @Autowired
    public ApiCallRunner(SqlFileSequentialCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            log.info("=== API Call Sql2Json2Api ===");
            log.info("Mode d'exécution: {}", threadingEnabled ? "parallèle" : "séquentiel");
            
            // Utiliser le coordinateur pour traiter tous les fichiers SQL séquentiellement
            Map<String, List<ApiResponse>> resultsByFile = coordinator.processAllSqlFiles();
            
            // Afficher le résumé des résultats
            if (resultsByFile.isEmpty()) {
                log.warn("Aucun résultat obtenu, vérifiez la configuration des fichiers SQL");
            } else {
                log.info("\n=== Résumé des traitements ===");
                
                for (Map.Entry<String, List<ApiResponse>> entry : resultsByFile.entrySet()) {
                    String fileName = entry.getKey();
                    List<ApiResponse> responses = entry.getValue();
                    
                    long successCount = responses.stream().filter(ApiResponse::isSuccess).count();
                    
                    log.info("{}: {} appels API - {} succès, {} échecs", 
                            fileName, responses.size(), successCount, responses.size() - successCount);
                }
            }
            
            log.info("\n=== Traitement terminé ===");
            
        } catch (Exception e) {
            log.error("Erreur lors du traitement: {}", e.getMessage(), e);
            exitCode = 1;
        }
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }
}