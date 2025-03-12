# Guide de configuration de l'application Spring Boot

Ce document explique comment gérer les configurations externes et lancer l'application avec différentes options de configuration.

## Lancement de l'application

### Lancement standard (avec application.yml à la racine)

```bash
java -Dspring.profiles.active=sql2json2api -jar sql2json2api-0.0.1-SNAPSHOT.jar
```

### Lancement lorsque le JAR n'est pas dans le même dossier que la configuration

#### Option 1 : Spécifier l'emplacement de la configuration
```bash
java -Dspring.profiles.active=sql2json2api --spring.config.location=chemin/vers/application.yml -jar chemin/vers/sql2json2api-0.0.1-SNAPSHOT.jar
```

#### Option 2 : Spécifier le nom et l'emplacement de la configuration
```bash
java -Dspring.profiles.active=sql2json2api --spring.config.name=application --spring.config.location=chemin/vers/ -jar chemin/vers/sql2json2api-0.0.1-SNAPSHOT.jar
```

#### Option 3 : Se placer dans le répertoire de configuration et indiquer le chemin vers le JAR
```bash
cd chemin/vers/dossier/avec/application.yml
java -Dspring.profiles.active=sql2json2api -jar chemin/complet/vers/sql2json2api-0.0.1-SNAPSHOT.jar
```

## Gestion des configurations multiples avec spring.config.import

La propriété `spring.config.import` permet d'importer des fichiers de configuration supplémentaires.

### Syntaxe de base dans application.yml

```yaml
spring:
  config:
    import:
      - classpath:/autre-config.yml          # Fichier dans le classpath
      - file:./config-locale.yml             # Fichier dans le répertoire courant
      - optional:file:/chemin/config.yml     # Fichier externe optionnel
      - optional:configserver:http://...     # Configuration depuis un serveur
```

### Préfixes disponibles

- `classpath:` - Fichiers dans le classpath de l'application
- `file:` - Fichiers avec chemin absolu ou relatif
- `optional:` - Indique que l'importation est facultative (pas d'erreur si absent)
- `configserver:` - Import depuis un Spring Cloud Config Server

### Formats supportés

`spring.config.import` supporte à la fois les formats YAML et Properties :

```yaml
spring:
  config:
    import:
      - file:./config.properties
      - file:./config.yml
```

## Faire référence aux fichiers du répertoire courant

Dans vos fichiers de configuration, vous pouvez référencer des fichiers locaux de plusieurs façons :

### Chemins relatifs standards
```yaml
fichier:
  chemin: "./mon-fichier.txt"
  logs: "./logs/application.log"
```

### Notation explicite avec file:./
```yaml
fichier:
  chemin: "file:./mon-fichier.txt"
  config: "file:./config/"
```

### Utilisation de variables système
```yaml
fichier:
  chemin: "${user.dir}/mon-fichier.txt"
  dossier: "${user.dir}/donnees/"
```

### Configuration des ressources statiques
```yaml
spring:
  web:
    resources:
      static-locations: file:./static/,file:./public/
```

## Cas d'utilisation courants

### Séparation des configurations par environnement
```yaml
spring:
  config:
    import:
      - common-config.yml
      - ${spring.profiles.active}-config.yml
```

### Organisation par domaine fonctionnel
```yaml
spring:
  config:
    import:
      - database-config.yml
      - security-config.yml
      - cache-config.yml
```

### Externalisation des secrets
```yaml
spring:
  config:
    import:
      - optional:file:./secrets.properties
```

## Utilisation de user.dir

La propriété système `user.dir` est particulièrement utile pour référencer de manière fiable le répertoire de travail courant dans vos configurations Spring Boot.

### Qu'est-ce que user.dir ?

`user.dir` est une propriété système Java qui contient le chemin absolu du répertoire à partir duquel l'application Java a été lancée (répertoire de travail courant).

### Exemples d'utilisation

#### Dans les fichiers de configuration
```yaml
# Dans application.yml
application:
  upload:
    directory: "${user.dir}/uploads"
  data:
    files: "${user.dir}/data"
  temp:
    directory: "${user.dir}/temp"
```

#### Dans le code Java
```java
// Dans une classe Java
String workingDir = System.getProperty("user.dir");
File configFile = new File(workingDir + "/config/settings.json");
```

### Avantages de user.dir

1. **Portabilité** : Fonctionne sur tous les systèmes d'exploitation
2. **Fiabilité** : Toujours résolu correctement, indépendamment de la façon dont l'application est lancée
3. **Flexibilité** : Permet de créer des chemins relatifs au dossier de lancement, facilitant le déploiement

### Combinaison avec spring.config.import

Vous pouvez combiner `user.dir` avec `spring.config.import` pour des configurations avancées :

```yaml
# Dans application.yml principal
spring:
  config:
    import:
      - optional:file:${user.dir}/conf/external-config.yml
```

## Note importante

Les chemins relatifs sont toujours résolus par rapport au répertoire de travail courant (d'où est lancée la commande `java -jar`), et non par rapport à l'emplacement du fichier JAR.