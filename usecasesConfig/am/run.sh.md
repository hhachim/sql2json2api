
Soit la commande:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev -Dspring-boot.run.jvmArguments="-Dspring.config.location=file:/Users/hachimhassani/projects/etljobs/sql2json2api/usecasesConfig/am/application-dev.yml"

./mvnw spring-boot:run -Dspring-boot.run.profiles=dev -Dspring-boot.run.jvmArguments="-Dspring.config.location=file:/Users/hachimhassani/projects/etljobs/sql2json2api/usecasesConfig/am/"
```

ou la commande :

```bash
java -jar target/application.jar --spring.config.location=file:/Users/hachimhassani/projects/etljobs/sql2json2api/usecasesConfig/am/application-dev.yml
java -Dspring.config.additional-location=file:/Users/hachimhassani/projects/etljobs/sql2json2api/usecasesConfig/am/ -jar target/sql2json2api-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```