# Exemples d'utilisation avec des chemins personnalisés

## 1. Chemin absolu pour les fichiers SQL et templates

```bash
# Utilisation de chemins absolus
java -jar sql2json2api.jar \
  --app.sql.external-directory=/chemin/absolu/vers/sql \
  --app.template.external-directory=/chemin/absolu/vers/templates \
  --api.auth.external-payload-template-path=/chemin/absolu/vers/templates/auth/auth-payload.ftlh
```

## 2. Chemin relatif à la configuration

```bash
# Utilisation de chemins relatifs à spring.config.location
java -jar sql2json2api.jar \
  -Dspring.config.location=file:/Users/hachimhassani/projects/etljobs/sql2json2api/usecasesConfig/am/ \
  --app.sql.external-directory=sql \
  --app.template.external-directory=templates \
  --api.auth.external-payload-template-path=templates/auth/auth-payload.ftlh
```

## 3. Utilisation avec Maven Wrapper

```bash
# Avec chemins absolus
./mvnw spring-boot:run \
  -Dspring-boot.run.arguments="--app.sql.external-directory=/chemin/absolu/vers/sql --app.template.external-directory=/chemin/absolu/vers/templates"

# Avec chemins relatifs à spring.config.location
./mvnw spring-boot:run \
  -Dspring.config.location=file:/Users/hachimhassani/projects/etljobs/sql2json2api/usecasesConfig/am/ \
  -Dspring-boot.run.arguments="--app.sql.external-directory=sql --app.template.external-directory=templates"
```

## 4. Activer un profil spécifique avec des chemins personnalisés

```bash
# Activer le profil "api-call-demo" avec chemins personnalisés
java -jar sql2json2api.jar \
  --spring.profiles.active=api-call-demo \
  --app.sql.external-directory=./sql-files \
  --app.template.external-directory=./template-files
```

## 5. Organisation recommandée des répertoires externes

```
usecasesConfig/
  ├── am/                           # Cas d'utilisation AM
  │   ├── application.yml           # Configuration spécifique
  │   ├── sql/                      # Fichiers SQL
  │   │   ├── GET_users.sql
  │   │   └── POST_order.sql
  │   └── templates/                # Templates Freemarker
  │       ├── json/
  │       │   ├── GET_users.ftlh
  │       │   └── POST_order.ftlh
  │       └── auth/
  │           └── auth-payload.ftlh
  └── intranet/                     # Autre cas d'utilisation
      ├── application.yml
      ├── sql/
      └── templates/
```

## Notes importantes

1. Les chemins relatifs sont résolus par rapport au répertoire spécifié par `spring.config.location`
2. Si `spring.config.location` pointe vers un fichier (et non un répertoire), le répertoire parent sera utilisé comme base
3. L'ordre de priorité pour les templates et fichiers SQL est:
   - D'abord chercher dans les répertoires externes (s'ils sont spécifiés)
   - Si non trouvé ou non spécifiés, utiliser le classpath (ressources embarquées)
4. Pour les templates d'authentification:
   - Si `api.auth.external-payload-template-path` est spécifié, il est utilisé en priorité
   - Sinon, on utilise `api.auth.payload-template-path` depuis le classpath