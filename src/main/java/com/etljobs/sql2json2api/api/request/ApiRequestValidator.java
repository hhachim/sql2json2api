package com.etljobs.sql2json2api.api.request;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

/**
 * Composant responsable de la validation des requêtes API.
 * Vérifie que les requêtes sont complètes et valides avant leur exécution.
 */
@Component
public class ApiRequestValidator {
    
    /**
     * Valide une requête API et renvoie les erreurs éventuelles.
     * 
     * @param request La requête à valider
     * @return Une liste d'erreurs de validation (vide si la requête est valide)
     */
    public List<String> validate(ApiRequest request) {
        List<String> errors = new ArrayList<>();
        
        // Validation de base
        if (request == null) {
            errors.add("La requête ne peut pas être nulle");
            return errors;
        }
        
        // Validation de l'URL
        if (request.getUrl() == null || request.getUrl().trim().isEmpty()) {
            errors.add("L'URL est obligatoire");
        } else {
            // Vérifier que l'URL est bien formée
            try {
                new URI(request.getUrl());
            } catch (URISyntaxException e) {
                errors.add("L'URL n'est pas valide: " + e.getMessage());
            }
        }
        
        // Validation de la méthode HTTP
        if (request.getMethod() == null) {
            errors.add("La méthode HTTP est obligatoire");
        }
        
        // Validation du payload pour les méthodes qui l'exigent
        if ((request.getMethod() == HttpMethod.POST || request.getMethod() == HttpMethod.PUT) 
                && (request.getPayload() == null || request.getPayload().trim().isEmpty())) {
            errors.add("Le payload est obligatoire pour les méthodes " + request.getMethod());
        }
        
        // Validation des en-têtes
        if (request.getHeaders() != null) {
            for (String headerName : request.getHeaders().keySet()) {
                if (headerName == null || headerName.trim().isEmpty()) {
                    errors.add("Les noms d'en-tête ne peuvent pas être vides");
                    break;
                }
            }
        }
        
        return errors;
    }
    
    /**
     * Valide une requête API et lance une exception si elle n'est pas valide.
     * 
     * @param request La requête à valider
     * @throws IllegalArgumentException si la requête n'est pas valide
     */
    public void validateAndThrow(ApiRequest request) {
        List<String> errors = validate(request);
        
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Requête API invalide: " + String.join(", ", errors));
        }
    }
    
    /**
     * Vérifie rapidement si une requête est valide sans générer de messages d'erreur détaillés.
     * 
     * @param request La requête à vérifier
     * @return true si la requête est valide, false sinon
     */
    public boolean isValid(ApiRequest request) {
        if (request == null) {
            return false;
        }
        
        return request.getUrl() != null && !request.getUrl().trim().isEmpty()
                && request.getMethod() != null;
    }
}