package com.campus.platform.exception;

/**
 * Thrown when a create/register operation would violate a uniqueness constraint
 * (e.g. duplicate registration, duplicate college domain, duplicate club name per tenant).
 * Mapped to HTTP 409 by GlobalExceptionHandler.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
