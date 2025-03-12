package com.etljobs.sql2json2api.service.template;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlaceholderProcessorTest {

    private PlaceholderProcessor processor;
    
    @BeforeEach
    void setUp() {
        processor = new PlaceholderProcessor();
    }
    
    @Test
    void processPlaceholders_ShouldReplaceSinglePlaceholder() {
        // Arrange
        String input = "/api/users/${result.id}";
        Map<String, Object> rowData = new HashMap<>();
        rowData.put("id", 42);
        
        // Act
        String result = processor.processPlaceholders(input, rowData);
        
        // Assert
        assertEquals("/api/users/42", result);
    }
    
    @Test
    void processPlaceholders_ShouldReplaceMultiplePlaceholders() {
        // Arrange
        String input = "/api/users/${result.id}/profile/${result.type}";
        Map<String, Object> rowData = new HashMap<>();
        rowData.put("id", 42);
        rowData.put("type", "admin");
        
        // Act
        String result = processor.processPlaceholders(input, rowData);
        
        // Assert
        assertEquals("/api/users/42/profile/admin", result);
    }
    
    @Test
    void processPlaceholders_ShouldHandleMissingValue() {
        // Arrange
        String input = "/api/users/${result.id}/${result.missing}";
        Map<String, Object> rowData = new HashMap<>();
        rowData.put("id", 42);
        // 'missing' key n'existe pas
        
        // Act
        String result = processor.processPlaceholders(input, rowData);
        
        // Assert
        assertEquals("/api/users/42/", result);
    }
    
    @Test
    void processPlaceholders_ShouldHandleNullInput() {
        // Arrange
        String input = null;
        Map<String, Object> rowData = new HashMap<>();
        
        // Act
        String result = processor.processPlaceholders(input, rowData);
        
        // Assert
        assertEquals("", result);
    }
    
    @Test
    void processPlaceholders_ShouldHandleNonPlaceholderInput() {
        // Arrange
        String input = "/api/users/all";
        Map<String, Object> rowData = new HashMap<>();
        
        // Act
        String result = processor.processPlaceholders(input, rowData);
        
        // Assert
        assertEquals("/api/users/all", result);
    }
}