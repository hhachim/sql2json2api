package com.etljobs.sql2json2api.exception;

public class ProcessingException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;

    public ProcessingException(String message) {
        super(message);
    }
    
    public ProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}