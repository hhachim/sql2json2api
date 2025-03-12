# SQL2JSON2API

Application Spring Boot qui exécute des requêtes SQL, transforme les résultats en JSON via FreeMarker, et effectue des appels API pour chaque ligne de résultat.

## Fonctionnalités

- Lecture et exécution de requêtes SQL stockées dans des fichiers
- Transformation des résultats en JSON à l'aide de templates FreeMarker
- Support pour les métadonnées d'API dans les templates
- Authentification Bearer Token pour les appels API
- Traitement ligne par ligne pour chaque résultat SQL

## Configuration

Les paramètres de l'application se trouvent dans `application.yml` :

```yaml
app:
  sql:
    directory: sql              # Répertoire des fichiers SQL
  template:
    directory: templates/json   # Répertoire des templates FreeMarker
  batch:
    size: 10                    # Nombre d'éléments par lot
    delay: 500                  # Délai entre les lots (ms)
api:
  auth:
    url: https://api.example.com/auth/token  # URL d'authentification
    username: api_user                       # Nom d'utilisateur
    password: api_password                   # Mot de passe
    token-ttl: 3600                          # Durée de vie du token (secondes)
```

## Convention de nommage

- Fichiers SQL : `VERBE_ressource.sql` (ex: `GET_users.sql`)
- Templates FreeMarker : `VERBE_ressource.ftlh` (ex: `GET_users.ftlh`)

## Exécution de l'application

### Démo d'appel API

Pour tester les appels API, exécutez :

```bash
./mvnw exec:java -Dexec.mainClass="com.etljobs.sql2json2api.Sql2json2apiApplication" -Dspring.profiles.active=api-call-demo
```

Cette commande :
1. Exécute la requête SQL depuis le fichier `TEST_httpbin.sql`
2. Transforme chaque ligne de résultat en JSON à l'aide du template `TEST_httpbin.ftlh`
3. Effectue un appel API POST vers httpbin.org avec le JSON généré
4. Affiche les résultats de l'appel API

### Démo de fichiers SQL

Pour tester la lecture et l'exécution des fichiers SQL :

```bash
./mvnw exec:java -Dexec.mainClass="com.etljobs.sql2json2api.Sql2json2apiApplication" -Dspring.profiles.active=sql-file-demo
```

### Démo de traitement de template

Pour tester le traitement des templates FreeMarker :

```bash
./mvnw exec:java -Dexec.mainClass="com.etljobs.sql2json2api.Sql2json2apiApplication" -Dspring.profiles.active=template-processing-demo
```

## Format des templates FreeMarker

Exemple de template avec métadonnées :

```
<#--
  @api-route: /api/users/${result.id}
  @api-method: GET
  @api-headers: {"Content-Type": "application/json", "Accept": "application/json"}
  @api-params: {"includeDetails": true}
-->
{
  "user": {
    "id": ${result.id},
    "username": "${result.username}",
    "email": "${result.email!""}"
  },
  "timestamp": "${.now?string["yyyy-MM-dd'T'HH:mm:ss"]}"
}
```

Les métadonnées d'API sont définies dans les commentaires FreeMarker et sont utilisées pour configurer l'appel API.