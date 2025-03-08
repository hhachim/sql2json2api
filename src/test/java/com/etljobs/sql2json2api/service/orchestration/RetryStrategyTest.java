package com.etljobs.sql2json2api.service.orchestration;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RetryStrategyTest {

    private RetryStrategy retryStrategy;
    
    @BeforeEach
    void setUp() {
        // 3 tentatives max, 100ms de délai initial, multiplicateur de 1.5
        retryStrategy = new RetryStrategy(3, 100, 1.5);
    }
    
    @ParameterizedTest
    @ValueSource(ints = {408, 429, 500, 502, 503, 504})
    void isRetryableStatusCode_ShouldReturnTrue_ForRetryableStatusCodes(int statusCode) {
        assertTrue(retryStrategy.isRetryableStatusCode(statusCode));
    }
    
    @ParameterizedTest
    @ValueSource(ints = {200, 201, 204, 400, 401, 403, 404})
    void isRetryableStatusCode_ShouldReturnFalse_ForNonRetryableStatusCodes(int statusCode) {
        assertFalse(retryStrategy.isRetryableStatusCode(statusCode));
    }
    
    @Test
    void isRetryableException_ShouldReturnTrue_ForTimeoutExceptions() {
        Exception timeoutException = new SocketTimeoutException("Connection timed out");
        assertTrue(retryStrategy.isRetryableException(timeoutException));
    }
    
    @Test
    void isRetryableException_ShouldReturnTrue_ForConnectionExceptions() {
        Exception connectionException = new ConnectException("Connection refused");
        assertTrue(retryStrategy.isRetryableException(connectionException));
    }
    
    @Test
    void isRetryableException_ShouldReturnTrue_ForTemporaryExceptions() {
        Exception temporaryException = new RuntimeException("Temporary server overload");
        assertTrue(retryStrategy.isRetryableException(temporaryException));
    }
    
    @Test
    void isRetryableException_ShouldReturnFalse_ForNullException() {
        assertFalse(retryStrategy.isRetryableException(null));
    }
    
    @Test
    void isRetryableException_ShouldReturnFalse_ForNonRetryableExceptions() {
        Exception validationException = new IllegalArgumentException("Invalid input");
        assertFalse(retryStrategy.isRetryableException(validationException));
    }
    
    @Test
    void calculateDelay_ShouldReturnInitialDelay_ForFirstAttempt() {
        assertEquals(100, retryStrategy.calculateDelay(1));
    }
    
    @Test
    void calculateDelay_ShouldIncreaseDelay_ForSubsequentAttempts() {
        assertEquals(100, retryStrategy.calculateDelay(2)); // 100 * (1.5^0)
        assertEquals(150, retryStrategy.calculateDelay(3)); // 100 * (1.5^1)
    }
    
    @Test
    void shouldRetry_ShouldReturnTrue_WhenUnderMaxAttempts() {
        assertTrue(retryStrategy.shouldRetry(1));
        assertTrue(retryStrategy.shouldRetry(2));
    }
    
    @Test
    void shouldRetry_ShouldReturnFalse_WhenAtOrAboveMaxAttempts() {
        assertFalse(retryStrategy.shouldRetry(3));
        assertFalse(retryStrategy.shouldRetry(4));
    }
    
    @Test
    void shouldRetry_ShouldConsiderBothAttemptsAndStatusCode() {
        // Sous le max d'essais et statut retryable → true
        assertTrue(retryStrategy.shouldRetry(500, 1));
        
        // Sous le max d'essais mais statut non-retryable → false
        assertFalse(retryStrategy.shouldRetry(404, 1));
        
        // Au-dessus du max d'essais même avec statut retryable → false
        assertFalse(retryStrategy.shouldRetry(500, 3));
    }
    
    @Test
    void shouldRetry_ShouldConsiderBothAttemptsAndException() {
        Exception retryableException = new SocketTimeoutException("Timeout");
        Exception nonRetryableException = new IllegalArgumentException("Invalid");
        
        // Sous le max d'essais et exception retryable → true
        assertTrue(retryStrategy.shouldRetry(retryableException, 1));
        
        // Sous le max d'essais mais exception non-retryable → false
        assertFalse(retryStrategy.shouldRetry(nonRetryableException, 1));
        
        // Au-dessus du max d'essais même avec exception retryable → false
        assertFalse(retryStrategy.shouldRetry(retryableException, 3));
    }
}