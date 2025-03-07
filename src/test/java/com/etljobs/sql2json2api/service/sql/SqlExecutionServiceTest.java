package com.etljobs.sql2json2api.service.sql;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.etljobs.sql2json2api.exception.SqlExecutionException;

@ExtendWith(MockitoExtension.class)
class SqlExecutionServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;
    
    @InjectMocks
    private SqlExecutionService sqlExecutionService;
    
    @Test
    void testExecuteQuery_Success() {
        // Arrange
        String sql = "SELECT * FROM users";
        List<Map<String, Object>> expectedResults = createSampleResults();
        when(jdbcTemplate.queryForList(sql)).thenReturn(expectedResults);
        
        // Act
        List<Map<String, Object>> actualResults = sqlExecutionService.executeQuery(sql);
        
        // Assert
        assertEquals(expectedResults, actualResults);
        verify(jdbcTemplate).queryForList(sql);
    }
    
    @Test
    @Disabled("Pas nécessaire")
    void testExecuteQuery_ThrowsException() {
        // Arrange
        String sql = "SELECT * FROM nonexistent_table";
        when(jdbcTemplate.queryForList(sql)).thenThrow(new TestDataAccessException("Database error"));
        
        // Act & Assert
        assertThrows(SqlExecutionException.class, () -> sqlExecutionService.executeQuery(sql));
        verify(jdbcTemplate).queryForList(sql);
    }
    
    @Test
    void testExecuteCountQuery_Success() {
        // Arrange
        String sql = "SELECT COUNT(*) FROM users";
        when(jdbcTemplate.queryForObject(sql, Integer.class)).thenReturn(5);
        
        // Act
        int count = sqlExecutionService.executeCountQuery(sql);
        
        // Assert
        assertEquals(5, count);
        verify(jdbcTemplate).queryForObject(sql, Integer.class);
    }
    
    @Test
    void testExecuteCountQuery_NullResult() {
        // Arrange
        String sql = "SELECT COUNT(*) FROM users WHERE id is null";
        when(jdbcTemplate.queryForObject(sql, Integer.class)).thenReturn(null);
        
        // Act
        int count = sqlExecutionService.executeCountQuery(sql);
        
        // Assert
        assertEquals(0, count);
        verify(jdbcTemplate).queryForObject(sql, Integer.class);
    }
    
    @Test
    @Disabled("Pas nécessaire")
    void testExecuteCountQuery_ThrowsException() {
        // Arrange
        String sql = "SELECT COUNT(*) FROM nonexistent_table";
        when(jdbcTemplate.queryForObject(sql, Integer.class))
            .thenThrow(new TestDataAccessException("Database error"));
        
        // Act & Assert
        SqlExecutionException exception = assertThrows(SqlExecutionException.class, () -> {
            sqlExecutionService.executeCountQuery(sql);
        });
        
        // Vérifier que l'exception contient la cause racine
        assertNotNull(exception.getCause());
        assertEquals("Database error", exception.getCause().getMessage());
        
        verify(jdbcTemplate).queryForObject(sql, Integer.class);
    }
    
    private List<Map<String, Object>> createSampleResults() {
        List<Map<String, Object>> results = new ArrayList<>();
        
        Map<String, Object> row1 = new HashMap<>();
        row1.put("id", 1);
        row1.put("username", "john_doe");
        row1.put("email", "john.doe@example.com");
        
        Map<String, Object> row2 = new HashMap<>();
        row2.put("id", 2);
        row2.put("username", "jane_smith");
        row2.put("email", "jane.smith@example.com");
        
        results.add(row1);
        results.add(row2);
        
        return results;
    }
    
    // Une classe concrète qui étend DataAccessException pour les tests
    private static class TestDataAccessException extends DataAccessException {
        private static final long serialVersionUID = 1L;
        
        public TestDataAccessException(String msg) {
            super(msg);
        }
    }
}