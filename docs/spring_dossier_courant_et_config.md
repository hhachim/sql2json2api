## Classpath et accès aux fichiers dans Spring Boot

### Comprendre le Classpath

Le classpath est un paramètre qui indique à la JVM où chercher les classes et les ressources nécessaires à l'exécution d'un programme Java.

- **Contenu du classpath** : Il inclut tous les répertoires et fichiers JAR qui contiennent des classes Java et des ressources
- **Dans un projet Maven/Gradle** : Le classpath inclut automatiquement :
  - Les classes compilées (`target/classes` ou `build/classes`)
  - Les ressources dans `src/main/resources`
  - Toutes les dépendances définies dans le pom.xml ou build.gradle

### Résolution des chemins avec le préfixe `classpath:`

- `classpath:config.properties` cherche `config.properties` à la racine du classpath
- `classpath:config/db.properties` cherche dans le sous-répertoire `config` du classpath

### Comment un JAR utilise les fichiers du répertoire courant

Quand vous exécutez un JAR Spring Boot, le "répertoire courant" est le répertoire depuis lequel la commande `java -jar` est exécutée, et non pas le répertoire où le JAR a été construit.

#### Mécanismes d'accès au répertoire courant

1. **Configuration automatique** : Spring Boot cherche automatiquement les fichiers `application.properties` ou `application.yml` dans :
   - Le répertoire courant (`./`)
   - Le sous-répertoire `/config` du répertoire courant (`./config/`)

2. **Chemins relatifs** : Les chemins relatifs dans la configuration sont résolus par rapport au répertoire courant de l'application lors de l'exécution :
   ```yaml
   app:
     upload-dir: ./uploads  # Sera résolu vers /chemin/execution/uploads
   ```

3. **Chemins absolus avec préfixe `file:`** : Permettent d'accéder à n'importe quel fichier accessible par le système d'exploitation :
   ```yaml
   spring:
     config:
       import:
         - file:/etc/myapp/config.yml
   ```

### Priorité entre classpath et système de fichiers

Spring Boot applique cette priorité lors de la recherche de fichiers de configuration :

1. Répertoire `/config` dans le répertoire courant
2. Répertoire courant (`./`)
3. Répertoire `/config` dans le classpath
4. Racine du classpath

### Exemple pratique : JAR construit ailleurs mais exécuté localement

Supposons un JAR construit sur un serveur CI/CD mais exécuté sur un serveur de production :

```bash
# Sur le serveur CI/CD
./mvnw clean package

# Sur le serveur de production
mkdir -p /opt/myapp/config
cp application-prod.yml /opt/myapp/config/
cd /opt/myapp
java -jar myapp.jar --spring.profiles.active=prod
```

Dans cet exemple :
- Le JAR contient ses propres `application.yml` et `application-prod.yml` dans son classpath
- Mais Spring Boot chargera d'abord `/opt/myapp/config/application-prod.yml` (s'il existe), qui peut surcharger les valeurs du JAR
- Les chemins relatifs dans la configuration seront relatifs à `/opt/myapp/`

### Cas particuliers et astuces

1. **Externalisation complète** : Pour un JAR totalement portable :
   ```bash
   java -jar app.jar --spring.config.location=file:/chemin/vers/config/
   ```

2. **Utilisation avec Docker** :
   ```dockerfile
   WORKDIR /app
   COPY target/myapp.jar .
   VOLUME /app/config
   CMD ["java", "-jar", "myapp.jar"]
   ```
   Les fichiers montés dans `/app/config` seront automatiquement détectés.

3. **Chemins basés sur des variables d'environnement** :
   ```yaml
   logging:
     file:
       path: ${LOG_PATH:./logs}
   ```
   L'application utilisera la variable d'environnement `LOG_PATH` ou par défaut `./logs` dans le répertoire courant.

Cette flexibilité permet de déployer le même binaire JAR dans différents environnements en externalisant la configuration, une pratique clé dans les applications cloud-natives.