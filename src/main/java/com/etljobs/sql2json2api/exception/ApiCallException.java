package com.etljobs.sql2json2api.exception;

/**
 * Exception thrown when there's an error making an API call.
 */
public class ApiCallException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;

    public ApiCallException(String message) {
        super(message);
    }
    
    public ApiCallException(String message, Throwable cause) {
        super(message, cause);
    }
}