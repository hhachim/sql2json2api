# lancer un test spécifique

```bash
mvn test -Dtest=TokenServiceTest
mvn test -Dtest=ProcessOrchestratorTest#processSqlFile_ShouldReadAndExecuteSQL
```

# Compiler sans les tests

```bash
mvn clean compile
```

# Lancer un runner particulier

```bash
./mvnw exec:java -Dexec.mainClass="com.etljobs.sql2json2api.Sql2json2apiApplication" -Dspring.profiles.active=api-auth-demo

./mvnw exec:java -Dexec.mainClass="com.etljobs.sql2json2api.Sql2json2apiApplication" -Dspring.profiles.active=sql-file-demo

./mvnw exec:java -Dexec.mainClass="com.etljobs.sql2json2api.Sql2json2apiApplication" -Dspring.profiles.active=dev

./mvnw exec:java -Dexec.mainClass="com.etljobs.sql2json2api.Sql2json2apiApplication" -Dspring.profiles.active=dev --spring.config.additional-location=file:/Users/hachimhassani/projects/etljobs/sql2json2api/usecasesConfig/am/
``` 

# Tester l'application telle quelle

- prevoir une base de données à utiliser : attention, des tables seront créées automatiquement dans cette base (une base vide est l'ideale)
- laisser le fichier application-dev.yml vide (ou tout commenter), pour éviter que la configuration application.yml soit surchagée
  
```bash
./mvnw exec:java -Dexec.mainClass="com.etljobs.sql2json2api.Sql2json2apiApplication" -Dspring.profiles.active=dev
```