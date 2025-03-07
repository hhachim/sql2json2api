package com.etljobs.sql2json2api.exception;

/**
 * Exception thrown when there is an issue with SQL file operations.
 */
public class SqlFileException extends RuntimeException {
    
    public SqlFileException(String message) {
        super(message);
    }
    
    public SqlFileException(String message, Throwable cause) {
        super(message, cause);
    }
}