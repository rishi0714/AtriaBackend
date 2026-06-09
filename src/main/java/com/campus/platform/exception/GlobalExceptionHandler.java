package com.campus.platform.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Centralised exception handler producing RFC 7807 ProblemDetail responses.
 *
 * Maps:
 *  ResourceNotFoundException         → 404
 *  DuplicateResourceException        → 409
 *  TenantAccessDeniedException       → 403
 *  AccessDeniedException (Spring)    → 403
 *  MethodArgumentNotValidException   → 422 with per-field errors
 *  DataIntegrityViolationException   → 409 (DB constraint violation)
 *  ResponseStatusException           → mirrors its own status
 *  Exception (catch-all)             → 500
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String ERROR_BASE_URI = "https://campus-platform.example.com/errors/";

    // ── Domain exceptions ────────────────────────────────────────────────────────

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "not-found", ex.getMessage());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ProblemDetail handleDuplicate(DuplicateResourceException ex) {
        return problem(HttpStatus.CONFLICT, "duplicate-resource", ex.getMessage());
    }

    @ExceptionHandler(TenantAccessDeniedException.class)
    public ProblemDetail handleTenantDenied(TenantAccessDeniedException ex) {
        return problem(HttpStatus.FORBIDDEN, "tenant-access-denied", ex.getMessage());
    }

    // ── Spring Security ──────────────────────────────────────────────────────────

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        return problem(HttpStatus.FORBIDDEN, "forbidden",
                "You do not have permission to perform this action.");
    }

    // ── Validation ───────────────────────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value",
                        (a, b) -> a   // keep first error per field
                ));

        ProblemDetail pd = problem(HttpStatus.UNPROCESSABLE_ENTITY,
                "validation-error", "One or more fields failed validation.");
        pd.setProperty("fieldErrors", fieldErrors);
        return pd;
    }

    // ── DB constraint violations ─────────────────────────────────────────────────

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("DB constraint violation: {}", ex.getMostSpecificCause().getMessage());
        return problem(HttpStatus.CONFLICT, "constraint-violation",
                "A database constraint was violated. The record may already exist.");
    }

    // ── ResponseStatusException (used in services for quick throws) ──────────────

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail handleResponseStatus(ResponseStatusException ex) {
        return problem(HttpStatus.valueOf(ex.getStatusCode().value()),
                "request-error", ex.getReason() != null ? ex.getReason() : ex.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleNoResource(NoResourceFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    // ── Catch-all ────────────────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        log.error("Unhandled exception: ", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error",
                "An unexpected error occurred. Please try again later.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Invalid value for parameter '" + ex.getName() + "': " + ex.getValue()
        );
        return ResponseEntity.badRequest().body(detail);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ProblemDetail> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "File size exceeds the maximum allowed limit of 1MB"
        );
        problem.setType(URI.create("https://campus-platform.example.com/errors/payload-too-large"));
        problem.setTitle("Payload Too Large");
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(problem);
    }

    // ── Builder helper ───────────────────────────────────────────────────────────

    private ProblemDetail problem(HttpStatus status, String errorCode, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(URI.create(ERROR_BASE_URI + errorCode));
        pd.setProperty("timestamp", Instant.now().toString());
        return pd;
    }
}
