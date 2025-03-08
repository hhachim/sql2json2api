package com.etljobs.sql2json2api.service.template;

import java.io.IOException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import com.etljobs.sql2json2api.model.SqlFile;

import lombok.extern.slf4j.Slf4j;

/**
 * Service responsable de trouver et de vérifier l'existence des templates
 * Freemarker correspondant aux fichiers SQL selon les conventions de nommage.
 * 
 * Conventions de nommage :
 * - Les fichiers SQL sont nommés avec le préfixe du verbe HTTP (ex: GET_users.sql)
 * - Les templates correspondants suivent la même convention (ex: GET_users.ftlh)
 */
@Service
@Slf4j
public class TemplateFinder {
    
    @Value("${app.template.directory}")
    private String templateDirectory;
    
    // Le rendre protected pour les tests
    protected PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    
    /**
     * Trouve le template correspondant à un fichier SQL.
     * 
     * @param sqlFile Le fichier SQL pour lequel trouver le template
     * @return Optional contenant le nom du template si trouvé, sinon vide
     */
    public Optional<String> findTemplateForSqlFile(SqlFile sqlFile) {
        if (sqlFile == null || sqlFile.getFileName() == null) {
            log.warn("Impossible de trouver un template pour un fichier SQL null");
            return Optional.empty();
        }
        
        // Déterminer le nom du template en fonction des conventions
        String templateName = determineTemplateName(sqlFile);
        
        // Vérifier si le template existe
        if (templateExists(templateName)) {
            log.debug("Template trouvé pour {}: {}", sqlFile.getFileName(), templateName);
            return Optional.of(templateName);
        } else {
            log.warn("Aucun template trouvé pour {}", sqlFile.getFileName());
            return Optional.empty();
        }
    }
    
    /**
     * Détermine le nom du template en fonction des conventions de nommage.
     * 
     * @param sqlFile Le fichier SQL
     * @return Le nom du template attendu
     */
    public String determineTemplateName(SqlFile sqlFile) {
        // Convention principale: remplacer l'extension .sql par .ftlh
        return sqlFile.getFileName().replace(".sql", ".ftlh");
    }
    
    /**
     * Vérifie si un template existe physiquement.
     * 
     * @param templateName Le nom du template à vérifier
     * @return true si le template existe, false sinon
     */
    public boolean templateExists(String templateName) {
        try {
            String templatePath = templateDirectory + "/" + templateName;
            Resource[] resources = resolver.getResources("classpath:" + templatePath);
            return resources.length > 0 && resources[0].exists();
        } catch (IOException e) {
            log.warn("Erreur lors de la vérification de l'existence du template {}: {}", 
                    templateName, e.getMessage());
            return false;
        }
    }
    
    /**
     * Méthode alternative qui trouve un template en utilisant des règles plus flexibles
     * quand la convention standard échoue.
     * 
     * @param sqlFile Le fichier SQL
     * @return Optional contenant le nom du template si trouvé avec des règles alternatives, sinon vide
     */
    public Optional<String> findAlternativeTemplate(SqlFile sqlFile) {
        if (sqlFile == null || sqlFile.getFileName() == null) {
            log.warn("Impossible de trouver un template alternatif pour un fichier SQL null");
            return Optional.empty();
        }
        
        // Essayer chaque stratégie alternative dans l'ordre
        String alternateTemplate = null;
        
        // 1. Essayer avec juste le baseName (sans le verbe HTTP)
        if (sqlFile.getBaseName() != null) {
            alternateTemplate = sqlFile.getBaseName() + ".ftlh";
            if (templateExists(alternateTemplate)) {
                log.debug("Template alternatif basé sur le nom de base trouvé: {}", alternateTemplate);
                return Optional.of(alternateTemplate);
            }
        }
        
        // 2. Essayer avec le même verbe HTTP mais un template générique
        if (sqlFile.getHttpMethod() != null) {
            alternateTemplate = sqlFile.getHttpMethod() + "_generic.ftlh";
            if (templateExists(alternateTemplate)) {
                log.debug("Template générique trouvé pour le verbe HTTP {}: {}", 
                        sqlFile.getHttpMethod(), alternateTemplate);
                return Optional.of(alternateTemplate);
            }
        }
        
        // 3. Essayer avec le template par défaut
        alternateTemplate = "default.ftlh";
        if (templateExists(alternateTemplate)) {
            log.debug("Template par défaut trouvé: {}", alternateTemplate);
            return Optional.of(alternateTemplate);
        }
        
        log.warn("Aucun template alternatif trouvé pour {}", sqlFile.getFileName());
        return Optional.empty();
    }
    
    /**
     * Trouve le template le plus approprié pour un fichier SQL,
     * en essayant d'abord la convention standard puis les règles alternatives.
     * 
     * @param sqlFile Le fichier SQL
     * @return Optional contenant le nom du template si trouvé par une méthode quelconque, sinon vide
     */
    public Optional<String> findBestMatchingTemplate(SqlFile sqlFile) {
        // D'abord essayer la convention standard
        Optional<String> standardTemplate = findTemplateForSqlFile(sqlFile);
        if (standardTemplate.isPresent()) {
            return standardTemplate;
        }
        
        // Si pas trouvé, essayer les règles alternatives
        return findAlternativeTemplate(sqlFile);
    }
}