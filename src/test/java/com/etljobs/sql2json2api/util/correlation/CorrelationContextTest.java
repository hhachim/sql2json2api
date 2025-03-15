package com.etljobs.sql2json2api.util.correlation;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class CorrelationContextTest {

    @AfterEach
    void cleanup() {
        // Nettoyage après chaque test
        CorrelationContext.clear();
    }
    
    @Test
    void generateId_ShouldReturnUniqueIds() {
        // Act
        String id1 = CorrelationContext.generateId();
        String id2 = CorrelationContext.generateId();
        
        // Assert
        assertNotNull(id1);
        assertNotNull(id2);
        assertTrue(id1.startsWith("cid-"), "ID should start with the correct prefix");
        assertTrue(id2.startsWith("cid-"), "ID should start with the correct prefix");
        assertEquals(12, id1.length(), "ID should have the expected length");
        assertEquals(12, id2.length(), "ID should have the expected length");
        assertTrue(!id1.equals(id2), "Generated IDs should be unique");
    }
    
    @Test
    void setId_ShouldStoreIdInMDC() {
        // Act
        String id = CorrelationContext.setId();
        String storedId = MDC.get(CorrelationContext.CORRELATION_ID_KEY);
        
        // Assert
        assertNotNull(id);
        assertEquals(id, storedId, "ID should be stored in MDC");
    }
    
    @Test
    void setIdWithValue_ShouldStoreSpecifiedIdInMDC() {
        // Arrange
        String customId = "cid-test12345";
        
        // Act
        CorrelationContext.setId(customId);
        String storedId = MDC.get(CorrelationContext.CORRELATION_ID_KEY);
        
        // Assert
        assertEquals(customId, storedId, "Custom ID should be stored in MDC");
    }
    
    @Test
    void getId_ShouldReturnStoredId() {
        // Arrange
        String id = CorrelationContext.setId();
        
        // Act
        String retrievedId = CorrelationContext.getId();
        
        // Assert
        assertEquals(id, retrievedId, "Retrieved ID should match the stored ID");
    }
    
    @Test
    void clear_ShouldRemoveIdFromMDC() {
        // Arrange
        CorrelationContext.setId();
        
        // Act
        CorrelationContext.clear();
        String id = CorrelationContext.getId();
        
        // Assert
        assertNull(id, "ID should be removed from MDC");
    }
}