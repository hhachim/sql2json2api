package com.etljobs.sql2json2api.api.response;

import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.slf4j.Slf4j;

/**
 * Classe utilitaire pour analyser et extraire des données d'une réponse API.
 * Offre des méthodes simplifiées pour traiter le contenu JSON des réponses.
 */
@Slf4j
public class ApiResponseParser {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final ApiResponse response;
    private JsonNode rootNode;
    private boolean parsed;
    private Exception parseError;
    
    /**
     * Crée un parser pour la réponse API spécifiée.
     * 
     * @param response Réponse API à analyser
     */
    public ApiResponseParser(ApiResponse response) {
        this.response = response;
        this.parsed = false;
    }
    
    /**
     * Initialise l'analyse du JSON si ce n'est pas déjà fait.
     * 
     * @return true si l'analyse a réussi, false sinon
     */
    private boolean initParsing() {
        if (!parsed && parseError == null) {
            try {
                if (response.getBody() != null && !response.getBody().isEmpty()) {
                    rootNode = objectMapper.readTree(response.getBody());
                    parsed = true;
                    return true;
                }
            } catch (JsonProcessingException e) {
                parseError = e;
                log.warn("Impossible d'analyser la réponse JSON: {}", e.getMessage());
            }
        }
        return parsed;
    }
    
    /**
     * Vérifie si la réponse contient du JSON valide.
     * 
     * @return true si la réponse contient du JSON valide
     */
    public boolean isValidJson() {
        return initParsing();
    }
    
    /**
     * Récupère l'erreur d'analyse éventuelle.
     * 
     * @return Optional contenant l'erreur, ou vide si aucune erreur
     */
    public Optional<Exception> getParseError() {
        return Optional.ofNullable(parseError);
    }
    
    /**
     * Récupère un nœud JSON depuis un chemin.
     * 
     * @param path Chemin au format pointé (ex: "data.user.id")
     * @return Optional contenant le nœud trouvé, ou vide si non trouvé
     */
    public Optional<JsonNode> getNode(String path) {
        if (!initParsing() || rootNode == null) {
            return Optional.empty();
        }
        
        try {
            String[] parts = path.split("\\.");
            JsonNode currentNode = rootNode;
            
            for (String part : parts) {
                if (currentNode.isObject()) {
                    if (!currentNode.has(part)) {
                        return Optional.empty();
                    }
                    currentNode = currentNode.get(part);
                } else if (currentNode.isArray() && part.matches("\\d+")) {
                    int index = Integer.parseInt(part);
                    if (index >= currentNode.size()) {
                        return Optional.empty();
                    }
                    currentNode = currentNode.get(index);
                } else {
                    return Optional.empty();
                }
            }
            
            return Optional.of(currentNode);
        } catch (Exception e) {
            log.warn("Erreur lors de la navigation dans le JSON: {}", e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Récupère une valeur textuelle depuis un chemin.
     * 
     * @param path Chemin au format pointé
     * @return Optional contenant la valeur textuelle, ou vide si non trouvée
     */
    public Optional<String> getString(String path) {
        return getNode(path).map(JsonNode::asText);
    }
    
    /**
     * Récupère une valeur numérique depuis un chemin.
     * 
     * @param path Chemin au format pointé
     * @return Optional contenant la valeur numérique, ou vide si non trouvée
     */
    public Optional<Integer> getInteger(String path) {
        return getNode(path).map(node -> {
            try {
                return node.asInt();
            } catch (Exception e) {
                return null;
            }
        });
    }
    
    /**
     * Récupère une valeur décimale depuis un chemin.
     * 
     * @param path Chemin au format pointé
     * @return Optional contenant la valeur décimale, ou vide si non trouvée
     */
    public Optional<Double> getDouble(String path) {
        return getNode(path).map(node -> {
            try {
                return node.asDouble();
            } catch (Exception e) {
                return null;
            }
        });
    }
    
    /**
     * Récupère une valeur booléenne depuis un chemin.
     * 
     * @param path Chemin au format pointé
     * @return Optional contenant la valeur booléenne, ou vide si non trouvée
     */
    public Optional<Boolean> getBoolean(String path) {
        return getNode(path).map(node -> {
            try {
                return node.asBoolean();
            } catch (Exception e) {
                return null;
            }
        });
    }
    
    /**
     * Vérifie si un chemin existe dans la réponse.
     * 
     * @param path Chemin au format pointé
     * @return true si le chemin existe, false sinon
     */
    public boolean hasPath(String path) {
        return getNode(path).isPresent();
    }
    
    /**
     * Récupère un tableau JSON depuis un chemin.
     * 
     * @param path Chemin au format pointé
     * @return Optional contenant le tableau, ou vide si non trouvé ou pas un tableau
     */
    public Optional<ArrayNode> getArray(String path) {
        return getNode(path).filter(JsonNode::isArray).map(node -> (ArrayNode) node);
    }
    
    /**
     * Récupère un objet JSON depuis un chemin.
     * 
     * @param path Chemin au format pointé
     * @return Optional contenant l'objet, ou vide si non trouvé ou pas un objet
     */
    public Optional<ObjectNode> getObject(String path) {
        return getNode(path).filter(JsonNode::isObject).map(node -> (ObjectNode) node);
    }
    
    /**
     * Analyse et convertit un chemin en un objet de classe spécifiée.
     * 
     * @param <T> Type d'objet à créer
     * @param path Chemin au format pointé
     * @param clazz Classe de l'objet à créer
     * @return Optional contenant l'objet créé, ou vide si échec
     */
    public <T> Optional<T> getAs(String path, Class<T> clazz) {
        Optional<JsonNode> node = getNode(path);
        if (node.isPresent()) {
            try {
                return Optional.of(objectMapper.treeToValue(node.get(), clazz));
            } catch (JsonProcessingException e) {
                log.warn("Impossible de convertir le nœud en {}: {}", clazz.getSimpleName(), e.getMessage());
            }
        }
        return Optional.empty();
    }
    
    /**
     * Récupère la racine de la réponse JSON.
     * 
     * @return Optional contenant la racine, ou vide si non analysable
     */
    public Optional<JsonNode> getRootNode() {
        if (initParsing()) {
            return Optional.ofNullable(rootNode);
        }
        return Optional.empty();
    }
    
    /**
     * Vérifie si la réponse contient un message d'erreur à un chemin spécifique.
     * 
     * @param errorPath Chemin où chercher le message d'erreur
     * @return Optional contenant le message d'erreur, ou vide si non trouvé
     */
    public Optional<String> getErrorMessage(String errorPath) {
        return getString(errorPath);
    }
    
    /**
     * Récupère un message d'erreur selon des chemins standards courants.
     * 
     * @return Optional contenant le premier message d'erreur trouvé, ou vide si aucun
     */
    public Optional<String> getStandardErrorMessage() {
        // Essayer différents chemins d'erreur standards
        return getString("error.message")
                .or(() -> getString("error"))
                .or(() -> getString("message"))
                .or(() -> getString("errorMessage"))
                .or(() -> getString("error_message"))
                .or(() -> getString("errors.0.message"))
                .or(() -> getString("errors.0"));
    }
}