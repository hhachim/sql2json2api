# Plan d'implémentation du multithreading pour les appels API

## Contraintes et besoins à respecter

1. **Séquentialité entre fichiers SQL** : Chaque fichier SQL doit être traité complètement (y compris tous ses appels API) avant de passer au fichier SQL suivant.

2. **Parallélisme intra-fichier SQL** : Les appels API générés à partir d'un même fichier SQL peuvent être exécutés en parallèle.

3. **Non-régression fonctionnelle** : L'implémentation du multithreading ne doit pas modifier le comportement fonctionnel de l'application.

4. **Configuration flexible** : Le multithreading doit pouvoir être activé/désactivé et configuré (nombre de threads, taille des lots, etc.).

5. **Gestion des erreurs robuste** : Les erreurs dans un thread ne doivent pas affecter les autres threads ni interrompre le flux global de traitement.

6. **Compatibilité avec le mécanisme de réessai existant** : Le nouveau système doit maintenir la compatibilité avec la stratégie de réessai existante.

7. **Supervision et monitoring** : Le système doit fournir des informations détaillées sur l'exécution des tâches parallèles pour faciliter le débogage.

8. **Stratégie de limitation de débit** : Pour éviter de surcharger les APIs cibles, le système doit inclure un mécanisme de contrôle du débit des appels.

## Phase 1 : Préparation et infrastructure

### Étape 1.1 : Ajouter la configuration pour le multithreading
1. Créer une classe `ThreadingConfig` dans le package `config`
2. Ajouter des propriétés de configuration dans `application.yml` 
   - `app.threading.enabled` (booléen, défaut: false)
   - `app.threading.pool-size` (nombre de threads, défaut: nombre de processeurs disponibles)
   - `app.threading.queue-capacity` (capacité de la file d'attente, défaut: 100)
   - `app.threading.timeout-seconds` (timeout des tâches, défaut: 60)

### Étape 1.2 : Créer un gestionnaire de threads configurable
1. Créer une classe `ThreadPoolManager` dans un nouveau package `service.threading`
2. Implémenter la création d'un `ThreadPoolExecutor` configurable
3. Ajouter des méthodes pour soumettre des tâches et attendre leur complétion
4. Ajouter une méthode de shutdown propre

## Phase 2 : Modèle de tâches et résultats

### Étape 2.1 : Créer des classes de modèle pour les tâches d'appel API
1. Créer une classe `ApiCallTask` qui encapsule une tâche d'appel API
2. Implémenter `Callable<ApiResponse>` pour cette classe
3. Ajouter des champs pour stocker toutes les informations nécessaires à l'appel API

### Étape 2.2 : Créer une classe pour gérer les résultats des appels API
1. Créer une classe `ApiCallResults` pour collecter les résultats
2. Implémenter des méthodes pour ajouter des résultats et obtenir des statistiques
3. Ajouter des méthodes pour gérer les timeouts et les erreurs

## Phase 3 : Service d'exécution parallèle

### Étape 3.1 : Créer un service d'exécution d'appels API en parallèle par fichier SQL
1. Créer une classe `SqlBasedParallelApiExecutionService` qui gère spécifiquement le parallélisme dans le contexte d'un fichier SQL
2. Implémenter un mécanisme de barrière qui attend que tous les appels API d'un fichier SQL soient terminés
3. Ajouter une méthode `executeAndWaitCompletion` qui bloque jusqu'à ce que tous les appels soient terminés

### Étape 3.2 : Ajouter le mode "compatibilité" pour les appels séquentiels
1. Ajouter un drapeau pour basculer entre exécution parallèle et séquentielle
2. Implémenter un mode séquentiel qui utilise le service API existant
3. S'assurer que l'interface reste cohérente quel que soit le mode

### Étape 3.3 : Ajouter un coordinateur de traitement SQL séquentiel
1. Créer une classe `SqlFileSequentialCoordinator` qui traite les fichiers SQL l'un après l'autre
2. Implémenter un mécanisme qui attend la complétion de tous les appels API d'un fichier avant de passer au suivant
3. Utiliser des `CountDownLatch` ou `CompletableFuture.allOf()` pour gérer l'attente

## Phase 4 : Intégration avec ProcessOrchestrator

### Étape 4.1 : Adapter ProcessOrchestrator pour le traitement séquentiel par fichier
1. Modifier `ProcessOrchestrator` pour traiter les fichiers SQL de manière séquentielle
2. Pour chaque fichier, utiliser le traitement parallèle des appels API
3. Implémenter un point de synchronisation entre chaque fichier SQL

### Étape 4.2 : Ajouter la stratégie de batching
1. Implémenter une logique pour diviser les résultats SQL en lots
2. Configurer la taille des lots via des propriétés (app.batch.api-calls)
3. Ajouter une option pour limiter le nombre d'appels API simultanés

### Étape 4.3 : Gestion des dépendances entre les fichiers SQL
1. Créer un mécanisme pour définir explicitement les dépendances entre fichiers SQL
2. Implémenter une méthode pour vérifier si tous les prérequis d'un fichier SQL sont satisfaits
3. Ajouter des logs détaillés sur l'état d'avancement du traitement séquentiel

## Phase 5 : Gestion des erreurs et reprise

### Étape 5.1 : Améliorer la gestion des erreurs spécifiques au multithreading
1. Créer des exceptions dédiées au traitement parallèle
2. Implémenter une stratégie de reprise pour les appels échoués
3. Ajouter un mécanisme pour réessayer uniquement les appels échoués

### Étape 5.2 : Ajouter une gestion de "backpressure"
1. Implémenter un mécanisme pour limiter la vitesse des appels API
2. Ajouter des délais dynamiques entre les soumissions de tâches
3. Configurer des seuils de ralentissement basés sur le taux d'erreur

## Phase 6 : Services de monitoring et logging

### Étape 6.1 : Ajouter un service de monitoring pour les appels parallèles
1. Créer une classe `ApiCallMonitoringService`
2. Implémenter des métriques pour suivre les performances des appels
3. Ajouter des logs détaillés pour le débogage

### Étape 6.2 : Créer un service d'analyse des résultats
1. Implémenter des méthodes pour analyser les temps de réponse
2. Ajouter des statistiques sur les taux de réussite/échec
3. Créer des rapports de performance pour les appels parallèles

### Étape 6.3 : Suivi de progression par fichier SQL
1. Implémenter un service de suivi qui montre la progression du traitement par fichier SQL
2. Ajouter des métriques spécifiques: nombre d'appels terminés/restants par fichier
3. Créer des logs structurés qui indiquent clairement la transition entre les fichiers SQL

## Phase 7 : Tests et intégration finale

### Étape 7.1 : Tests unitaires pour les nouveaux composants
1. Ajouter des tests pour `ThreadPoolManager`
2. Tester `SqlBasedParallelApiExecutionService` avec des mocks
3. Tester les différentes stratégies de batching et gestion d'erreurs

### Étape 7.2 : Tests d'intégration avec contrainte séquentielle
1. Créer des tests qui vérifient que le traitement séquentiel des fichiers SQL est respecté
2. Tester des scénarios où un fichier SQL dépend des résultats d'un fichier précédent
3. Vérifier que les appels API d'un fichier sont tous complétés avant le passage au fichier suivant

### Étape 7.3 : Finaliser l'intégration
1. Mettre à jour ApiCallRunner pour utiliser le nouveau système en parallèle
2. Configurer les profiles pour activer/désactiver le multithreading
3. Mettre à jour la documentation

### Étape 7.4 : Tests de stress pour la séquentialité
1. Créer des tests avec de nombreux fichiers SQL et de nombreux appels API
2. Simuler des délais variables pour les appels API pour tester la robustesse
3. Vérifier que l'ordre de traitement des fichiers SQL est toujours respecté même sous charge

## Conseils d'implémentation

1. **Isolation des modifications** : Gardez les modifications isolées des composants existants aussi longtemps que possible.
2. **Tests fréquents** : Testez à chaque étape pour vous assurer que le comportement existant n'est pas cassé.
3. **Feature flag** : Utilisez un drapeau de fonctionnalité pour activer/désactiver le multithreading facilement.
4. **Logging détaillé** : Ajoutez des logs détaillés, particulièrement importants pour déboguer les problèmes de multithreading.
5. **Gestion des ressources** : Assurez-vous que les pools de threads sont correctement fermés pour éviter les fuites de ressources.
6. **Identifiants uniques** : Ajoutez des identifiants uniques à chaque tâche pour faciliter le suivi dans les logs.
7. **Mode dégradé** : Prévoyez un mécanisme pour revenir au mode séquentiel si des problèmes surviennent avec le parallélisme.
8. **Séparation des préoccupations** : Gardez la logique de parallélisation séparée de la logique métier.