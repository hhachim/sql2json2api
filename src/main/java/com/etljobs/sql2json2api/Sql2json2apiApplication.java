package com.etljobs.sql2json2api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@Slf4j
public class Sql2json2apiApplication {

    public static void main(String[] args) {
		// Démarrer l'application Spring Boot normalement
		ConfigurableApplicationContext context = SpringApplication.run(Sql2json2apiApplication.class, args);
		
		// Forcer l'application à se terminer après l'exécution des beans CommandLineRunner
		// Cette ligne est importante pour que l'application se termine après l'exécution des runners
		int exitCode = SpringApplication.exit(context, () -> 0);
		
		// Quitter avec le code de sortie approprié
		System.exit(exitCode);
	}
}