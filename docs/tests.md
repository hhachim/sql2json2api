# lancer un test spécifique

```bash
./mvnw exec:java -Dexec.mainClass="com.etljobs.sql2json2api.Sql2json2apiApplication" -Dspring.profiles.active=dev
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
``` 