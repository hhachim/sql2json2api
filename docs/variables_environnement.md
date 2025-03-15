# Guide des Variables d'Environnement dans Spring Boot

## Introduction

Spring Boot offre un système flexible de configuration qui utilise différentes sources, notamment les fichiers de propriétés (`.properties`, `.yml`), les arguments de ligne de commande et les variables d'environnement. Ce document explique comment Spring Boot associe automatiquement les variables d'environnement aux propriétés de configuration, parfois de manière surprenante.

## Table des matières

- [Priorité des Sources de Configuration](#priorité-des-sources-de-configuration)
- [Relaxed Binding: Convention de Nommage](#relaxed-binding-convention-de-nommage)
- [Exemples Pratiques](#exemples-pratiques)
- [Cas Particuliers et Pièges](#cas-particuliers-et-pièges)
- [Bonnes Pratiques](#bonnes-pratiques)
- [FAQ](#faq)

## Priorité des Sources de Configuration

Spring Boot utilise une hiérarchie stricte pour résoudre les propriétés de configuration. De la priorité la plus haute à la plus basse:

1. Arguments de ligne de commande (`--property=value`)
2. Variables d'environnement du système d'exploitation
3. Fichiers de configuration externes (en dehors du JAR)
4. Fichiers de configuration dans le classpath (à l'intérieur du JAR)
5. Valeurs par défaut dans le code

Cette hiérarchie signifie qu'une variable d'environnement aura toujours priorité sur une propriété définie dans un fichier `application.yml` ou `application.properties`.

## Relaxed Binding: Convention de Nommage

Spring Boot utilise un mécanisme appelé "relaxed binding" qui permet de référencer une même propriété avec différentes conventions de nommage. Par exemple, une propriété `spring.datasource.url` peut être définie par:

- Une propriété Java: `spring.datasource.url`
- Une variable d'environnement: `SPRING_DATASOURCE_URL`
- Un argument de ligne de commande: `--spring.datasource.url`

### Important: Conversion automatique des noms de propriétés

Lorsque vous définissez une propriété personnalisée comme `api.auth.token` dans votre configuration, Spring Boot créera automatiquement une correspondance avec une variable d'environnement nommée `API_AUTH_TOKEN`.

**C'est cette conversion automatique qui peut créer de la confusion**, car elle se produit même si vous n'avez jamais référencé explicitement cette variable d'environnement avec la notation `${API_AUTH_TOKEN}` dans vos fichiers de configuration.

## Exemples Pratiques

### Exemple 1: Définition d'une propriété dans application.yml

```yaml
api:
  auth:
    token: "mon-token-secret"
```

### Exemple 2: Remplacement par une variable d'environnement

Si vous définissez une variable d'environnement `API_AUTH_TOKEN="token-environnement"`, elle remplacera automatiquement la valeur définie dans application.yml.

Démarrez l'application:
```bash
# Cette commande utilisera "token-environnement" au lieu de "mon-token-secret"
API_AUTH_TOKEN="token-environnement" java -jar monapp.jar
```

### Exemple 3: Référence explicite avec valeur par défaut

```yaml
api:
  auth:
    token: "${API_AUTH_TOKEN:mon-token-secret}"
```

Cette notation indique explicitement: "utiliser la variable d'environnement `API_AUTH_TOKEN` si elle existe, sinon utiliser `mon-token-secret`".

### Exemple 4: Désactiver la résolution des variables d'environnement

Pour forcer l'utilisation de la valeur littérale (empêcher la substitution par les variables d'environnement), vous pouvez utiliser des guillemets simples:

```yaml
api:
  auth:
    token: 'mon-token-secret'  # Les guillemets simples n'empêcheront PAS la substitution
```

Malheureusement, cette approche ne fonctionne pas avec le "relaxed binding" de Spring Boot. La seule solution est de désactiver complètement le mécanisme de relaxed binding ou d'utiliser une technique alternative.

## Cas Particuliers et Pièges

### Sous-environnements et configurations multiples

Si vous utilisez plusieurs profils Spring ou environnements, les variables d'environnement s'appliqueront à tous les profils, ce qui peut être indésirable dans certains cas.

### Docker et conteneurs

Dans les environnements Docker, les variables d'environnement sont souvent utilisées pour configurer les applications. C'est un cas d'usage légitime de cette fonctionnalité, mais cela peut créer des conflits inattendus avec votre configuration locale.

### Pipelines CI/CD

Les pipelines CI/CD définissent souvent des variables d'environnement pour les tests, ce qui peut remplacer de manière inattendue vos configurations.

## Bonnes Pratiques

1. **Utilisation explicite des références**:
   ```yaml
   api:
     auth:
       token: "${MY_CUSTOM_TOKEN_VAR:valeur-par-defaut}"
   ```

2. **Noms de propriétés non conventionnels**:
   Utilisez des noms qui ne suivent pas directement la convention de Spring Boot:
   ```yaml
   api:
     auth:
       tokenValue: "mon-token-secret"  # Moins susceptible d'être remplacé par API_AUTH_TOKENVALUE
   ```

3. **Logging des propriétés au démarrage**:
   ```java
   @PostConstruct
   public void logConfig() {
       log.info("Configuration chargée: token={}", configuredToken);
   }
   ```

4. **Utilisation de @ConfigurationProperties**:
   ```java
   @ConfigurationProperties(prefix = "api.auth")
   public class AuthProperties {
       private String token;
       // getters/setters
   }
   ```

5. **Accès direct via Environment**:
   ```java
   @Autowired
   private Environment env;
   
   public void myMethod() {
       String token = env.getProperty("api.auth.token");
   }
   ```

## Exemples concrets de résolution des problèmes

### Problème: Variable d'environnement remplaçant une configuration locale

**Symptôme**:
Vous avez défini `token: "blabla"` dans application.yml, mais l'application utilise une autre valeur.

**Solution 1**: Vérifier les variables d'environnement actives
```bash
# Sur Linux/Mac
env | grep API_AUTH_TOKEN

# Sur Windows (PowerShell)
Get-ChildItem Env:API_AUTH_TOKEN
```

**Solution 2**: Désactiver temporairement la variable d'environnement
```bash
# Sur Linux/Mac
unset API_AUTH_TOKEN
./run.sh

# Sur Windows (PowerShell)
Remove-Item Env:API_AUTH_TOKEN -ErrorAction SilentlyContinue
./run.ps1
```

**Solution 3**: Contourner le problème avec un nom de propriété différent
```yaml
# Dans application.yml
api:
  auth:
    secretTokenValue: "blabla"  # Nom moins susceptible d'avoir une variable d'environnement correspondante
```

```java
// Dans votre code Java
@Value("${api.auth.secretTokenValue}")
private String configuredToken;
```

## FAQ

### Q: Comment savoir quelles variables d'environnement Spring Boot utilise?

**R**: Vous pouvez activer le mode debug pour voir toutes les propriétés et leur source:
```
java -jar monapp.jar --debug
```

### Q: Comment désactiver complètement l'utilisation des variables d'environnement?

**R**: Ce n'est pas recommandé, mais vous pouvez personnaliser le `PropertySourcesPlaceholderConfigurer`:
```java
@Bean
public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
    PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
    configurer.setIgnoreUnresolvablePlaceholders(true);
    
    // Personnaliser les sources de propriétés pour exclure les variables d'environnement
    // Ce code est simplifié et nécessite plus de configuration
    return configurer;
}
```

### Q: Les variables d'environnement sont-elles sensibles à la casse?

**R**: Oui. Dans la plupart des systèmes d'exploitation, les variables d'environnement sont sensibles à la casse. Spring Boot convertit généralement les propriétés en majuscules avec des underscores comme séparateurs pour les faire correspondre aux variables d'environnement.

---

Ce comportement de Spring Boot peut être déroutant, mais il est conçu pour faciliter la configuration dans différents environnements. En comprenant comment fonctionne le "relaxed binding", vous pouvez éviter les surprises et utiliser efficacement ce mécanisme à votre avantage.