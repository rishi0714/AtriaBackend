package com.campus.platform.exception;

/**
 * Thrown when a requested resource is not found in the database.
 * Mapped to HTTP 404 by GlobalExceptionHandler.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceName, Object identifier) {
        super(resourceName + " not found with identifier: " + identifier);
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
