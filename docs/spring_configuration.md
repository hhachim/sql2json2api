# Documentation sur la configuration dans Spring Boot

## Fichiers de configuration et leur priorité

Spring Boot offre plusieurs méthodes pour configurer une application. L'ordre de priorité (du plus faible au plus élevé) est le suivant :

1. Propriétés par défaut de l'application (`@PropertySource` dans le code)
2. Fichiers de configuration dans le classpath
   - `application.properties` ou `application.yml`
   - `application-{profile}.properties` ou `application-{profile}.yml`
3. Fichiers de configuration externes (en dehors du jar)
   - `application.properties` ou `application.yml` dans le répertoire courant
   - `application-{profile}.properties` ou `application-{profile}.yml` dans le répertoire courant
4. Propriétés de ligne de commande (`--property=value`)
5. Propriétés système Java (`-Dproperty=value`)
6. Variables d'environnement du système d'exploitation

En cas de conflit, la source de plus haute priorité l'emporte.

## Formats de configuration

Spring Boot prend en charge deux formats de fichiers de configuration principaux :

1. **Properties (.properties)** : Format clé-valeur simple
   ```properties
   server.port=8080
   spring.datasource.url=jdbc:mysql://localhost/db
   ```

2. **YAML (.yml ou .yaml)** : Format hiérarchique plus lisible
   ```yaml
   server:
     port: 8080
   spring:
     datasource:
       url: jdbc:mysql://localhost/db
   ```

## Import de fichiers de configuration

Depuis Spring Boot 2.4, vous pouvez importer d'autres fichiers de configuration grâce à la propriété `spring.config.import`.

### Syntaxe dans application.yml
```yaml
spring:
  config:
    import:
      - classpath:autre-config.yml
      - file:/chemin/absolu/config.properties
      - optional:classpath:config-optionnelle.yml
```

### Préfixes disponibles

| Préfixe | Description | Exemple |
|---------|-------------|---------|
| `classpath:` | Fichier dans le classpath de l'application | `classpath:configs/db.properties` |
| `file:` | Fichier avec chemin absolu dans le système de fichiers | `file:/etc/myapp/config.yml` |
| `optional:` | Indique que le fichier est facultatif (ne génère pas d'erreur s'il n'existe pas) | `optional:file:/config.yml` |

Les préfixes peuvent être combinés : `optional:file:/chemin/config.yml`

### Ordre d'importation
Les fichiers importés sont chargés immédiatement après le fichier qui contient l'instruction d'import, avant de passer au fichier de configuration suivant dans l'ordre de priorité.

## Profils de configuration

Les profils permettent d'avoir différentes configurations selon l'environnement.

### Activer un profil
```bash
java -jar app.jar --spring.profiles.active=dev,mysql
```

### Configuration spécifique à un profil
```yaml
# application.yml
spring:
  config:
    activate:
      on-profile: dev
  datasource:
    url: jdbc:h2:mem:devdb
---
spring:
  config:
    activate:
      on-profile: prod
  datasource:
    url: jdbc:mysql://prod-server/proddb
```

## Remplacement de variables

Spring Boot prend en charge le remplacement de variables dans les fichiers de configuration.

### Syntaxe pour le remplacement de variables
```yaml
app:
  name: MonApplication
  description: ${app.name} est une application Spring Boot
  
server:
  port: ${PORT:8080}  # Utilise la valeur de PORT ou 8080 par défaut
```

### Remplacement en cascade
```yaml
# variables.properties
BASE_DIR=/opt/app
LOG_DIR=${BASE_DIR}/logs
CACHE_DIR=${BASE_DIR}/cache

# application.yml qui importe variables.properties
spring:
  config:
    import:
      - file:${VARIABLES_FILE_PATH:./variables.properties}
      
logging:
  path: ${LOG_DIR}
```

## Exemples d'utilisation avancée

### Importer des fichiers conditionnellement selon le profil
```yaml
spring:
  config:
    activate:
      on-profile: dev
    import:
      - optional:file:./config/dev.properties
---
spring:
  config:
    activate:
      on-profile: prod
    import:
      - optional:file:./config/prod.properties
```

### Utiliser des variables d'environnement pour localiser les fichiers
```yaml
spring:
  config:
    import:
      - optional:file:${CONFIG_DIR:./config}/application.properties
```

### Importations imbriquées
Le fichier importé peut lui-même importer d'autres fichiers, créant ainsi une hiérarchie d'importation.

## Bonnes pratiques

1. **Configuration modulaire** : Divisez votre configuration en modules logiques
2. **Valeurs par défaut** : Toujours fournir des valeurs par défaut pour les propriétés critiques
3. **Documentation** : Commenter les propriétés importantes dans des fichiers exemple
4. **Sécurité** : Ne stockez jamais les informations sensibles directement dans les fichiers de configuration versionés
5. **Validation** : Utilisez `@ConfigurationProperties` avec validation pour les blocs de configuration
6. **Centralisation** : Pour les microservices, envisagez d'utiliser Spring Cloud Config Server

Cette structure de configuration flexible permet d'adapter votre application à différents environnements tout en maintenant une base de code unique.