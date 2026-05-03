package com.example.ucmconnectapi.exception;

/**
 * Exception thrown when user tries to access/modify resource they don't own
 * Results in HTTP 403 Forbidden
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }

    public ForbiddenException(String resourceName, String action) {
        super(String.format("You are not allowed to %s this %s", action, resourceName.toLowerCase()));
    }
}
