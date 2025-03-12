package com.etljobs.sql2json2api.api.response;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

class ApiResponseParserTest {

    private ApiResponse validJsonResponse;
    private ApiResponse invalidJsonResponse;
    private ApiResponse emptyResponse;
    
    @BeforeEach
    void setUp() {
        // Réponse avec JSON valide
        validJsonResponse = ApiResponse.builder()
                .statusCode(200)
                .body("{\"data\":{\"user\":{\"id\":123,\"name\":\"John Doe\",\"active\":true,\"score\":4.5},\"items\":[1,2,3]},\"status\":\"success\"}")
                .build();
        
        // Réponse avec JSON invalide
        invalidJsonResponse = ApiResponse.builder()
                .statusCode(200)
                .body("This is not a JSON content")
                .build();
        
        // Réponse vide
        emptyResponse = ApiResponse.builder()
                .statusCode(204)
                .body("")
                .build();
    }
    
    @Test
    void isValidJson_ShouldReturnTrue_ForValidJson() {
        // Act
        ApiResponseParser parser = new ApiResponseParser(validJsonResponse);
        
        // Assert
        assertTrue(parser.isValidJson());
    }
    
    @Test
    void isValidJson_ShouldReturnFalse_ForInvalidJson() {
        // Act
        ApiResponseParser parser = new ApiResponseParser(invalidJsonResponse);
        
        // Assert
        assertFalse(parser.isValidJson());
        assertTrue(parser.getParseError().isPresent());
    }
    
    @Test
    void isValidJson_ShouldReturnFalse_ForEmptyResponse() {
        // Act
        ApiResponseParser parser = new ApiResponseParser(emptyResponse);
        
        // Assert
        assertFalse(parser.isValidJson());
    }
    
    @Test
    void getString_ShouldReturnCorrectValue() {
        // Arrange
        ApiResponseParser parser = new ApiResponseParser(validJsonResponse);
        
        // Act & Assert
        assertEquals("John Doe", parser.getString("data.user.name").orElse(null));
        assertEquals("success", parser.getString("status").orElse(null));
    }
    
    @Test
    void getString_ShouldReturnEmpty_ForNonExistentPath() {
        // Arrange
        ApiResponseParser parser = new ApiResponseParser(validJsonResponse);
        
        // Act & Assert
        assertFalse(parser.getString("data.user.email").isPresent());
        assertFalse(parser.getString("nonexistent").isPresent());
    }
    
    @Test
    void getInteger_ShouldReturnCorrectValue() {
        // Arrange
        ApiResponseParser parser = new ApiResponseParser(validJsonResponse);
        
        // Act & Assert
        assertEquals(123, parser.getInteger("data.user.id").orElse(null));
        assertEquals(1, parser.getInteger("data.items.0").orElse(null));
    }
    
    @Test
    void getDouble_ShouldReturnCorrectValue() {
        // Arrange
        ApiResponseParser parser = new ApiResponseParser(validJsonResponse);
        
        // Act & Assert
        assertEquals(4.5, parser.getDouble("data.user.score").orElse(null));
    }
    
    @Test
    void getBoolean_ShouldReturnCorrectValue() {
        // Arrange
        ApiResponseParser parser = new ApiResponseParser(validJsonResponse);
        
        // Act & Assert
        assertEquals(true, parser.getBoolean("data.user.active").orElse(null));
    }
    
    @Test
    void hasPath_ShouldReturnTrue_ForExistingPath() {
        // Arrange
        ApiResponseParser parser = new ApiResponseParser(validJsonResponse);
        
        // Act & Assert
        assertTrue(parser.hasPath("data.user.id"));
        assertTrue(parser.hasPath("status"));
    }
    
    @Test
    void hasPath_ShouldReturnFalse_ForNonExistentPath() {
        // Arrange
        ApiResponseParser parser = new ApiResponseParser(validJsonResponse);
        
        // Act & Assert
        assertFalse(parser.hasPath("data.user.email"));
        assertFalse(parser.hasPath("nonexistent"));
    }
    
    @Test
    void getArray_ShouldReturnCorrectArray() {
        // Arrange
        ApiResponseParser parser = new ApiResponseParser(validJsonResponse);
        
        // Act
        ArrayNode array = parser.getArray("data.items").orElse(null);
        
        // Assert
        assertTrue(array != null);
        assertEquals(3, array.size());
        assertEquals(1, array.get(0).asInt());
        assertEquals(2, array.get(1).asInt());
        assertEquals(3, array.get(2).asInt());
    }
    
    @Test
    void getObject_ShouldReturnCorrectObject() {
        // Arrange
        ApiResponseParser parser = new ApiResponseParser(validJsonResponse);
        
        // Act
        ObjectNode userObject = parser.getObject("data.user").orElse(null);
        
        // Assert
        assertTrue(userObject != null);
        assertEquals(123, userObject.get("id").asInt());
        assertEquals("John Doe", userObject.get("name").asText());
    }
    
    @Test
    void getRootNode_ShouldReturnRootJsonNode() {
        // Arrange
        ApiResponseParser parser = new ApiResponseParser(validJsonResponse);
        
        // Act
        JsonNode rootNode = parser.getRootNode().orElse(null);
        
        // Assert
        assertTrue(rootNode != null);
        assertTrue(rootNode.has("data"));
        assertTrue(rootNode.has("status"));
    }
    
    @Test
    void getStandardErrorMessage_ShouldReturnErrorMessage() {
        // Arrange
        ApiResponse errorResponse = ApiResponse.builder()
                .statusCode(400)
                .body("{\"error\":\"Invalid request\",\"message\":\"Missing required field\"}")
                .build();
        ApiResponseParser parser = new ApiResponseParser(errorResponse);
        
        // Act & Assert
        assertEquals("Invalid request", parser.getStandardErrorMessage().orElse(null));
    }
}