package com.etljobs.sql2json2api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "api")
@Getter
@Setter
public class ApiConfig {
    
    /**
     * URL de base pour les appels API
     */
    private String baseUrl;
    
    /**
     * Configuration pour l'authentification
     */
    private Auth auth = new Auth();
    
    @Getter
    @Setter
    public static class Auth {
        private String url;
        private String username;
        private String password;
        private long tokenTtl = 3600;
        private String payloadTemplatePath = "auth/auth-payload.ftlh";
    }
}