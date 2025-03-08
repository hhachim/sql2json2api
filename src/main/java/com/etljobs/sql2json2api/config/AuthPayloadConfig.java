package com.etljobs.sql2json2api.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "api.auth")
@Getter
@Setter
public class AuthPayloadConfig {
    
    /**
     * Format du payload pour l'authentification
     * Valeurs possibles: default, custom, custom2, etc.
     */
    private String payloadFormat = "default";
    
    /**
     * Champs additionnels à inclure dans le payload (pour format custom)
     */
    private Map<String, String> additionalFields = new HashMap<>();
    
    /**
     * Nom du champ pour le username (peut varier selon les APIs)
     */
    private String usernameField = "username";
    
    /**
     * Nom du champ pour le mot de passe (peut varier selon les APIs)
     */
    private String passwordField = "password";
}