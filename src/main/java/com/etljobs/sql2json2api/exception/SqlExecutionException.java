package com.etljobs.sql2json2api.exception;

/**
 * Exception thrown when SQL execution fails.
 */
public class SqlExecutionException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;

    public SqlExecutionException(String message) {
        super(message);
    }
    
    public SqlExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}