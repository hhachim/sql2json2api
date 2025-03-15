package com.etljobs.sql2json2api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Configuration pour activer AOP dans l'application.
 */
@Configuration
@EnableAspectJAutoProxy
public class AopConfig {
    // Pas besoin de contenu, l'annotation @EnableAspectJAutoProxy suffit
}