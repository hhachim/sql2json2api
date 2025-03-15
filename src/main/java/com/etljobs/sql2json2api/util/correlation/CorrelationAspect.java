package com.etljobs.sql2json2api.util.correlation;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Aspect qui gère automatiquement les identifiants de corrélation
 * aux points clés de l'application.
 */
@Aspect
@Component
@Slf4j
public class CorrelationAspect {
    
    /**
     * Point de coupe pour les méthodes principales de traitement.
     * Ceci cible spécifiquement les méthodes qui démarrent le traitement d'un fichier SQL.
     */
    @Pointcut("execution(* com.etljobs.sql2json2api.service.threading.SqlFileSequentialCoordinator.process*(..))")
    public void sqlFileProcessingMethods() {}
    
    /**
     * Point de coupe pour les méthodes d'exécution d'appels API.
     */
    @Pointcut("execution(* com.etljobs.sql2json2api.api.execution.ApiCallExecutor.execute*(..))")
    public void apiCallMethods() {}
    
    /**
     * Conseil qui entoure les méthodes de traitement pour ajouter l'ID de corrélation.
     */
    @Around("sqlFileProcessingMethods() || apiCallMethods()")
    public Object addCorrelationId(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getSignature().getDeclaringTypeName();
        
        // Vérifier si un ID de corrélation existe déjà
        String existingId = CorrelationContext.getId();
        boolean created = false;
        
        if (existingId == null) {
            // Générer un nouvel ID seulement si aucun n'existe
            String correlationId = CorrelationContext.setId();
            created = true;
            log.debug("Nouveau corrélation ID généré pour {}.{}: {}", 
                      className, methodName, correlationId);
        } else {
            log.debug("Utilisation du corrélation ID existant pour {}.{}: {}", 
                      className, methodName, existingId);
        }
        
        try {
            // Exécuter la méthode avec l'ID de corrélation dans le contexte
            return joinPoint.proceed();
        } finally {
            // Nettoyer seulement si nous avons créé l'ID dans cette méthode
            if (created) {
                log.debug("Nettoyage du corrélation ID après {}.{}", className, methodName);
                CorrelationContext.clear();
            }
        }
    }
}