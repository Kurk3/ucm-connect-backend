package com.example.ucmconnectapi.exception;

/**
 * Exception thrown when a resource conflict occurs (e.g., duplicate entry)
 * Results in HTTP 409 Conflict
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s already exists with %s: '%s'", resourceName, fieldName, fieldValue));
    }
}
