package com.etljobs.sql2json2api.service.template;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * Service responsable du traitement des placeholders dans les routes API.
 * Cette classe extrait et remplace les placeholders comme ${result.id} dans les routes.
 */
@Service
@Slf4j
public class PlaceholderProcessor {

    // Pattern pour trouver les placeholders dans la route (ex: ${result.id})
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{result\\.(\\w+)\\}");
    
    /**
     * Traite les placeholders dans une chaîne en les remplaçant par les valeurs 
     * correspondantes dans les données.
     * 
     * @param input La chaîne contenant des placeholders
     * @param rowData Les données pour remplacer les placeholders
     * @return La chaîne avec les placeholders remplacés
     */
    public String processPlaceholders(String input, Map<String, Object> rowData) {
        if (input == null) {
            return "";
        }
        
        log.debug("Traitement des placeholders dans: {}", input);
        
        StringBuffer result = new StringBuffer();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(input);
        
        while (matcher.find()) {
            String fieldName = matcher.group(1);
            Object value = rowData.get(fieldName);
            String replacement = (value != null) ? value.toString() : "";
            
            log.debug("Remplacement de ${{{}.{}}} par '{}'", "result", fieldName, replacement);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        
        matcher.appendTail(result);
        String processed = result.toString();
        
        log.debug("Résultat après traitement: {}", processed);
        return processed;
    }
}