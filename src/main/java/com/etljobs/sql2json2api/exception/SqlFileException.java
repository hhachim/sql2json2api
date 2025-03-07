package com.etljobs.sql2json2api.exception;

/**
 * Exception thrown when there's an error reading or processing SQL files.
 */
public class SqlFileException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;

    public SqlFileException(String message) {
        super(message);
    }
    
    public SqlFileException(String message, Throwable cause) {
        super(message, cause);
    }
}