package com.etljobs.sql2json2api.exception;

/**
 * Exception thrown when there's an error processing a template.
 */
public class TemplateProcessingException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;

    public TemplateProcessingException(String message) {
        super(message);
    }
    
    public TemplateProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}