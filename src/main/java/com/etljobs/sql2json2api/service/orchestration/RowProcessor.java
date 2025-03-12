package com.etljobs.sql2json2api.service.orchestration;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.etljobs.sql2json2api.exception.TemplateProcessingException;
import com.etljobs.sql2json2api.model.ApiResponse;
import com.etljobs.sql2json2api.model.ApiTemplateResult;
import com.etljobs.sql2json2api.model.SqlFile;
import com.etljobs.sql2json2api.service.http.ApiClientService;
import com.etljobs.sql2json2api.service.orchestration.RetryStrategy.RetryContext;
import com.etljobs.sql2json2api.service.template.TemplateProcessingService;

import lombok.extern.slf4j.Slf4j;

/**
 * Classe responsable du traitement d'une seule ligne de résultat SQL.
 * Cette classe encapsule la logique de transformation d'une ligne en JSON
 * et d'appel à l'API correspondante, avec gestion des réessais.
 */
@Component
@Slf4j
public class RowProcessor {
    
    private final TemplateProcessingService templateService;
    private final ApiClientService apiClientService;
    
    /**
     * Constructeur avec injection de dépendances.
     */
    public RowProcessor(
            TemplateProcessingService templateService,
            ApiClientService apiClientService) {
        this.templateService = templateService;
        this.apiClientService = apiClientService;
    }
    
    /**
     * Traite une seule ligne avec un mécanisme de réessai.
     * 
     * @param sqlFile Le fichier SQL traité
     * @param row La ligne de données à traiter
     * @param rowIndex L'index de la ligne (pour les logs)
     * @param rowIdentifier L'identifiant lisible de la ligne (pour les logs)
     * @param retryStrategy La stratégie de réessai à utiliser
     * @param rowErrors Liste pour collecter les erreurs par ligne
     * @return La réponse API générée, ou null si une erreur non récupérable est survenue
     */
    public ApiResponse processRow(
            SqlFile sqlFile, 
            Map<String, Object> row, 
            int rowIndex, 
            String rowIdentifier, 
            RetryStrategy retryStrategy,
            List<RowError> rowErrors) {
        
        try {
            // Créer un contexte de réessai
            RetryContext retryContext = retryStrategy.createContext();
            
            // Traiter le template (une seule fois car le contenu ne change pas)
            ApiTemplateResult templateResult = templateService.processTemplate(sqlFile.getTemplateName(), row);
            
            // Variables pour stocker le résultat et l'erreur
            ApiResponse response = null;
            Exception lastError = null;
            
            // Tenter l'appel API avec réessai
            while (retryContext.getCurrentAttempt() <= retryStrategy.getMaxAttempts()) {
                int attempt = retryContext.getCurrentAttempt();
                
                if (attempt > 1) {
                    log.info("Tentative {} pour la ligne {}", attempt, rowIdentifier);
                }
                
                try {
                    // Appeler l'API
                    response = apiClientService.callApi(
                            templateResult.getEndpointInfo().getRoute(),
                            templateResult.getEndpointInfo().getMethod(),
                            templateResult.getJsonPayload(),
                            templateResult.getEndpointInfo().getHeaders(),
                            templateResult.getEndpointInfo().getUrlParams());
                    
                    // Sauvegarder le code de statut dans le contexte
                    retryContext.setLastStatusCode(response.getStatusCode());
                    
                    // Vérifier si l'appel a réussi ou si c'est une erreur non récupérable
                    if (response.isSuccess() || 
                            !retryStrategy.shouldRetry(response.getStatusCode(), attempt)) {
                        return response; // On retourne la réponse même en cas d'erreur
                    }
                    
                    // C'est une erreur récupérable, et on n'a pas atteint le max de tentatives
                    log.warn("API a répondu avec le code {} pour la ligne {}. Réessai...", 
                            response.getStatusCode(), rowIdentifier);
                    
                    // Préparer la prochaine tentative
                    retryContext.incrementAttempt();
                    retryStrategy.sleep(attempt + 1);
                    
                } catch (Exception e) {
                    lastError = e;
                    retryContext.setLastException(e);
                    
                    if (!retryStrategy.shouldRetry(e, attempt)) {
                        // Erreur non récupérable
                        log.error("Erreur non récupérable pour la ligne {}: {}", 
                                rowIdentifier, e.getMessage());
                        
                        // Ajouter aux erreurs
                        rowErrors.add(new RowError(rowIndex, row, e.getMessage(), e, attempt));
                        
                        // Retourner une réponse d'erreur
                        return ApiResponse.builder()
                                .statusCode(500)
                                .body("Erreur non récupérable: " + e.getMessage())
                                .build();
                    }
                    
                    if (attempt == retryStrategy.getMaxAttempts()) {
                        // On a atteint le max de tentatives
                        log.error("Échec définitif pour la ligne {} après {} tentatives: {}", 
                                rowIdentifier, attempt, e.getMessage());
                        
                        // Ajouter aux erreurs
                        rowErrors.add(new RowError(rowIndex, row, e.getMessage(), e, attempt));
                        
                        // Retourner une réponse d'erreur
                        return ApiResponse.builder()
                                .statusCode(500)
                                .body("Erreur après " + attempt + " tentatives: " + e.getMessage())
                                .build();
                    }
                    
                    // Attendre avant de réessayer
                    log.warn("Erreur récupérable lors de l'appel API pour la ligne {}: {}. Réessai...", 
                            rowIdentifier, e.getMessage());
                    
                    // Préparer la prochaine tentative
                    retryContext.incrementAttempt();
                    retryStrategy.sleep(attempt + 1);
                }
            }
            
            // Si on arrive ici, c'est qu'il y a eu une erreur mais on n'a pas atteint maxRetryAttempts
            // Ce cas est théoriquement impossible avec la logique ci-dessus
            if (lastError != null) {
                return ApiResponse.builder()
                        .statusCode(500)
                        .body("Erreur non gérée: " + lastError.getMessage())
                        .build();
            } else {
                return ApiResponse.builder()
                        .statusCode(500)
                        .body("Erreur inconnue: aucune réponse ni erreur générée")
                        .build();
            }
            
        } catch (TemplateProcessingException e) {
            // Erreur lors du traitement du template, non récupérable
            log.error("Erreur de template pour la ligne {}: {}", rowIdentifier, e.getMessage());
            rowErrors.add(new RowError(rowIndex, row, e.getMessage(), e, 1));
            
            return ApiResponse.builder()
                    .statusCode(500)
                    .body("Erreur de template: " + e.getMessage())
                    .build();
        } catch (Exception e) {
            // Toute autre erreur non prévue
            log.error("Erreur inattendue pour la ligne {}: {}", rowIdentifier, e.getMessage());
            rowErrors.add(new RowError(rowIndex, row, e.getMessage(), e, 1));
            
            return ApiResponse.builder()
                    .statusCode(500)
                    .body("Erreur inattendue: " + e.getMessage())
                    .build();
        }
    }
}