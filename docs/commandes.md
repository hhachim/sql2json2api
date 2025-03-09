# Commandes équivalentes: mvnw et java -jar

**Note importante**: Pour utiliser les commandes `java -jar`, vous devez d'abord construire votre application avec `./mvnw clean package`, ce qui générera le fichier JAR dans le dossier target/.

## 1. Lancement basique de l'application
| Maven Wrapper (`mvnw`) | Java JAR |
|------------------------|----------|
| `./mvnw spring-boot:run` | `java -jar target/application.jar` |

Lance l'application avec la configuration par défaut.

## 2. Activer un profil spécifique
| Maven Wrapper (`mvnw`) | Java JAR |
|------------------------|----------|
| `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` | `java -jar target/application.jar --spring.profiles.active=dev` |

Active le profil "dev", chargeant les configurations spécifiques à l'environnement de développement.

## 3. Utiliser un emplacement de configuration personnalisé
| Maven Wrapper (`mvnw`) | Java JAR |
|------------------------|----------|
| `./mvnw spring-boot:run -Dspring-boot.run.arguments=--spring.config.location=file:/chemin/vers/config/` | `java -jar target/application.jar --spring.config.location=file:/chemin/vers/config/` |

Remplace l'emplacement par défaut des fichiers de configuration par un chemin personnalisé.

## 4. Ajouter un emplacement de configuration supplémentaire
| Maven Wrapper (`mvnw`) | Java JAR |
|------------------------|----------|
| `./mvnw spring-boot:run -Dspring-boot.run.arguments=--spring.config.additional-location=file:/chemin/vers/config/` | `java -jar target/application.jar --spring.config.additional-location=file:/chemin/vers/config/` |

Ajoute un emplacement supplémentaire pour les fichiers de configuration, tout en conservant les emplacements par défaut.

## 5. Spécifier un fichier de configuration précis
| Maven Wrapper (`mvnw`) | Java JAR |
|------------------------|----------|
| `./mvnw spring-boot:run -Dspring-boot.run.arguments=--spring.config.location=file:/chemin/vers/application-custom.yml` | `java -jar target/application.jar --spring.config.location=file:/chemin/vers/application-custom.yml` |

Utilise un fichier de configuration spécifique au lieu du fichier application.yml par défaut.

## 6. Combiner profil et emplacement de configuration
| Maven Wrapper (`mvnw`) | Java JAR |
|------------------------|----------|
| `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev -Dspring-boot.run.arguments=--spring.config.location=file:/chemin/vers/config/` | `java -jar target/application.jar --spring.profiles.active=dev --spring.config.location=file:/chemin/vers/config/` |

Combine l'activation du profil "dev" avec un dossier de configuration personnalisé.

## 7. Utiliser un profil spécifique API avec configuration externe
| Maven Wrapper (`mvnw`) | Java JAR |
|------------------------|----------|
| `./mvnw spring-boot:run -Dspring-boot.run.profiles=api-call-demo -Dspring-boot.run.arguments=--spring.config.location=file:/chemin/vers/config-externe/` | `java -jar target/application.jar --spring.profiles.active=api-call-demo --spring.config.location=file:/chemin/vers/config-externe/` |

Active le profil "api-call-demo" tout en utilisant des configurations externes spécifiques aux tests d'API.

## 8. Modifier le port d'écoute
| Maven Wrapper (`mvnw`) | Java JAR |
|------------------------|----------|
| `./mvnw spring-boot:run -Dserver.port=9090` | `java -Dserver.port=9090 -jar target/application.jar` |

Change le port sur lequel l'application s'exécute (9090 au lieu du port 8080 par défaut).

## 9. Configurer la mémoire JVM
| Maven Wrapper (`mvnw`) | Java JAR |
|------------------------|----------|
| `./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Xmx512m -Xms256m"` | `java -Xmx512m -Xms256m -jar target/application.jar` |

Configure la mémoire Java (512MB maximum, 256MB initial) pour optimiser les performances.

## 10. Activer le débogage à distance
| Maven Wrapper (`mvnw`) | Java JAR |
|------------------------|----------|
| `./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8000"` | `java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8000 -jar target/application.jar` |

Active le débogage à distance sur le port 8000, permettant de se connecter avec un IDE pendant l'exécution.