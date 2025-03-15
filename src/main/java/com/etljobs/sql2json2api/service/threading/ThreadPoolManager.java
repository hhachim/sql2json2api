package com.etljobs.sql2json2api.service.threading;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.etljobs.sql2json2api.config.ThreadingConfig;
import com.etljobs.sql2json2api.util.correlation.CorrelationContext;
import com.etljobs.sql2json2api.util.correlation.CorrelationPropagator;

import lombok.extern.slf4j.Slf4j;

/**
 * Gestionnaire de pool de threads pour l'exécution de tâches parallèles.
 * Cette classe encapsule la création et la gestion d'un ExecutorService configurable.
 */
@Component
@Slf4j
public class ThreadPoolManager implements DisposableBean {
    
    private final ThreadingConfig config;
    private final ExecutorService executorService;
    
    /**
     * Constructeur avec injection de la configuration.
     * 
     * @param config Configuration du threading
     */
    @Autowired
    public ThreadPoolManager(ThreadingConfig config) {
        this.config = config;
        
        // Log de la configuration
        log.info("Initialisation du pool de threads avec {} threads, capacité de file d'attente: {}, timeout: {}s",
                config.getPoolSize(), config.getQueueCapacity(), config.getTimeoutSeconds());
        
        // Création du ThreadPoolExecutor avec les paramètres de configuration
        this.executorService = new ThreadPoolExecutor(
                config.getPoolSize(),  // Taille de base du pool
                config.getPoolSize(),  // Taille maximale du pool (identique)
                60L, TimeUnit.SECONDS, // Durée de vie des threads inactifs
                new ArrayBlockingQueue<>(config.getQueueCapacity()), // File d'attente bornée
                new ThreadFactoryBuilder("api-call-"), // Fabrique de threads avec préfixe personnalisé
                new CallerRunsPolicy() // Stratégie en cas de rejet: exécuter dans le thread appelant
        );
    }
    
    /**
     * Soumet une tâche pour exécution.
     * 
     * @param <T> Type de retour de la tâche
     * @param task Tâche à exécuter
     * @return Future représentant le résultat de la tâche
     */
    public <T> Future<T> submitTask(Callable<T> task) {
        // Envelopper la tâche pour propager l'ID de corrélation
        Callable<T> wrappedTask = CorrelationPropagator.wrap(task);
        return executorService.submit(wrappedTask);
    }
    
    /**
     * Soumet une collection de tâches pour exécution.
     * 
     * @param <T> Type de retour des tâches
     * @param tasks Collection de tâches à exécuter
     * @return Liste de Futures représentant les résultats des tâches
     */
    public <T> List<Future<T>> submitTasks(Collection<? extends Callable<T>> tasks) {
        List<Future<T>> futures = new ArrayList<>(tasks.size());
        
        for (Callable<T> task : tasks) {
            try {
                // Appliquer un délai entre les soumissions si configuré
                if (config.getSubmissionDelayMs() > 0) {
                    Thread.sleep(config.getSubmissionDelayMs());
                }
                
                // Envelopper la tâche pour propager l'ID de corrélation
                Callable<T> wrappedTask = CorrelationPropagator.wrap(task);
                futures.add(executorService.submit(wrappedTask));
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interruption lors de la soumission des tâches", e);
                break;
            }
        }
        
        return futures;
    }
    
    // Aucun changement pour le reste de la classe
    
    public void shutdown() {
        log.info("Arrêt manuel du pool de threads");
        
        // Ajout du log de l'état actuel du pool
        if (executorService instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor tpe = (ThreadPoolExecutor) executorService;
            log.info("État du pool avant arrêt: tâches actives={}, tâches en file d'attente={}",
                    tpe.getActiveCount(), tpe.getQueue().size());
        }
        
        destroyExecutor();
    }
    
    /**
     * Arrête proprement le service d'exécution.
     * Cette méthode est appelée automatiquement par Spring lors de l'arrêt de l'application.
     */
    @Override
    public void destroy() {
        log.info("Arrêt automatique du pool de threads (appelé par Spring)");
        destroyExecutor();
    }
    
    /**
     * Implémentation commune pour l'arrêt du ExecutorService.
     */
    private void destroyExecutor() {
        if (executorService == null || executorService.isShutdown()) {
            log.info("Le pool de threads est déjà arrêté ou null");
            return;
        }
        
        // Log plus détaillé de l'état
        log.info("Tentative d'arrêt du pool de threads...");
        
        executorService.shutdown();
        try {
            // Augmenter le temps d'attente pour terminer les tâches
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                log.warn("Les tâches ne se terminent pas, forçage de l'arrêt");
                
                // Obtenir la liste des tâches non exécutées
                List<Runnable> pendingTasks = executorService.shutdownNow();
                log.warn("{} tâches n'ont pas été exécutées", pendingTasks.size());
                
                // Attendre une seconde fois avec un délai plus court
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.error("Le pool de threads n'a pas pu être arrêté proprement");
                } else {
                    log.info("Pool de threads arrêté avec succès (après forçage)");
                }
            } else {
                log.info("Pool de threads arrêté avec succès");
            }
        } catch (InterruptedException e) {
            log.warn("Interruption lors de l'arrêt du pool de threads", e);
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Fabrique de threads personnalisée pour nommer les threads.
     */
    private static class ThreadFactoryBuilder implements java.util.concurrent.ThreadFactory {
        private final String namePrefix;
        private final ThreadGroup group;
        private final java.util.concurrent.atomic.AtomicInteger threadNumber = new java.util.concurrent.atomic.AtomicInteger(1);
        
        ThreadFactoryBuilder(String namePrefix) {
            this.namePrefix = namePrefix;
            SecurityManager s = System.getSecurityManager();
            this.group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
        }
        
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
            // Assurez-vous que le thread n'est PAS daemon
            t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }
    
    /**
     * Stratégie pour gérer les rejets de tâches lorsque la file d'attente est pleine.
     * Cette implémentation exécute la tâche dans le thread appelant, ce qui crée
     * une contre-pression naturelle.
     */
    private static class CallerRunsPolicy implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            log.warn("File d'attente pleine, exécution de la tâche dans le thread appelant");
            if (!executor.isShutdown()) {
                r.run();
            }
        }
    }
    
    /**
     * Vérifie si le multithreading est activé dans la configuration.
     * 
     * @return true si le multithreading est activé, false sinon
     */
    public boolean isEnabled() {
        return config.isEnabled();
    }
    
    /**
     * Obtient la durée de timeout configurée en secondes.
     * 
     * @return Durée de timeout en secondes
     */
    public int getTimeoutSeconds() {
        return config.getTimeoutSeconds();
    }
}