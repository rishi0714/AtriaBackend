package com.campus.platform.exception;

/**
 * Thrown when a JWT-authenticated user attempts to access data
 * belonging to a different college tenant.
 * Mapped to HTTP 403 by GlobalExceptionHandler.
 */
public class TenantAccessDeniedException extends RuntimeException {

    public TenantAccessDeniedException(String message) {
        super(message);
    }

    public TenantAccessDeniedException() {
        super("Access denied: resource belongs to a different college tenant.");
    }
}
