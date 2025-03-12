# Gestion des dépendances externes avec un JAR standard (non-fat)

## Dépendances externes pour un JAR construit ailleurs

Lorsque vous utilisez un JAR standard (non-fat) qui a été construit sur un système différent de celui où il est exécuté, la gestion des dépendances devient un point important à comprendre.

### Mécanismes de chargement des dépendances externes

#### 1. Le Classpath explicite

La méthode la plus directe pour spécifier des dépendances externes est l'utilisation du paramètre `-classpath` ou `-cp` de la commande `java` :

```bash
java -cp "myapp.jar:./lib/*" com.example.MainClass
```

Cette commande ajoute tous les JARs du répertoire `lib` au classpath.

#### 2. Répertoires de recherche standards

Java recherche par défaut les dépendances dans certains répertoires spécifiques :

- **lib/** : Répertoire conventionnel pour les bibliothèques
- **libs/** : Variante couramment utilisée
- **ext/** : Pour les extensions
- **CLASSPATH** : Variable d'environnement système

#### 3. Le Manifest du JAR

Le fichier `MANIFEST.MF` dans un JAR peut spécifier des dépendances externes via l'attribut `Class-Path` :

```
Manifest-Version: 1.0
Main-Class: com.example.MainClass
Class-Path: lib/dependency1.jar lib/dependency2.jar
```

Les chemins spécifiés sont relatifs à l'emplacement du JAR principal.

### Configuration dans Spring Boot pour les JARs non-fat

Spring Boot est généralement distribué sous forme de FAT JAR, mais peut être configuré pour un déploiement "exploded" avec dépendances externes :

#### Utilisation de Maven pour générer un JAR standard avec dépendances externes

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <configuration>
                <layout>ZIP</layout>
                <includeSystemScope>true</includeSystemScope>
                <executable>true</executable>
            </configuration>
        </plugin>
    </plugins>
</build>
```

Dans ce cas, les dépendances seront placées dans un répertoire `lib/` à côté du JAR principal.

### Scénarios de déploiement courants

#### Scénario 1: Structure de déploiement manuelle

```
/opt/myapp/
├── myapp.jar                # JAR principal
├── application.properties   # Configuration externe
└── lib/                     # Répertoire de dépendances
    ├── dependency1.jar
    ├── dependency2.jar
    └── ...
```

Pour exécuter :
```bash
cd /opt/myapp
java -jar myapp.jar
```

Le JAR chargera automatiquement les dépendances dans le répertoire `lib/` si le Manifest est correctement configuré.

#### Scénario 2: Déploiement avec script de lancement

```bash
#!/bin/bash
APP_HOME=/opt/myapp
JAVA_OPTS="-Xms256m -Xmx512m"
CLASSPATH="$APP_HOME/myapp.jar:$APP_HOME/lib/*:$APP_HOME/config"

java $JAVA_OPTS -cp $CLASSPATH com.example.MainClass
```

Ce script offre un contrôle précis sur le classpath et les options JVM.

#### Scénario 3: Serveur d'applications Java EE

Pour les déploiements dans des serveurs comme Tomcat, JBoss ou WebSphere :

```
[SERVEUR]/webapps/myapp/
├── WEB-INF/
│   ├── classes/     # Classes compilées
│   ├── lib/         # Dépendances spécifiques à l'application
│   └── web.xml
└── ...
```

Le serveur d'applications gère le classpath pour toutes les applications déployées.

### Bonnes pratiques pour les dépendances externes

1. **Structure cohérente** : Maintenir une structure de répertoires cohérente entre les environnements
2. **Scripts d'installation** : Fournir des scripts qui mettent en place correctement la structure de dépendances
3. **Documentation** : Documenter clairement les dépendances requises et leur emplacement attendu
4. **Vérification au démarrage** : Implémenter une vérification des dépendances critiques au démarrage de l'application
5. **Isolation des dépendances** : Éviter de partager les mêmes fichiers JAR entre plusieurs applications

### Gestion de versions et conflits

Lorsque les dépendances sont externalisées, la gestion des versions devient cruciale :

1. **Nommage explicite avec version** : Utilisez des noms de fichiers qui incluent le numéro de version
2. **Isolation des dépendances** : Chaque application devrait avoir son propre ensemble de dépendances
3. **Tests de compatibilité** : Vérifiez que toutes les dépendances sont compatibles entre elles

### Différences clés avec un FAT JAR

| Aspect | JAR standard avec dépendances externes | FAT JAR |
|--------|---------------------------------------|---------|
| Taille du JAR principal | Petite (quelques MB) | Grande (dizaines ou centaines de MB) |
| Déploiement | Plus complexe, nécessite plusieurs fichiers | Simple, fichier unique |
| Mise à jour des dépendances | Peut remplacer une dépendance sans recompiler | Nécessite une recompilation complète |
| Partage de dépendances | Possible entre applications | Impossible (chaque JAR contient ses propres copies) |
| Isolation | Moins d'isolation (dépendances partagées) | Meilleure isolation |
| Performance au démarrage | Peut être plus lente (chargement de plusieurs JARs) | Généralement plus rapide |

Cette approche de JAR standard avec dépendances externes est particulièrement pertinente dans les environnements d'entreprise traditionnels où le contrôle précis des dépendances et leur partage entre applications sont importants.