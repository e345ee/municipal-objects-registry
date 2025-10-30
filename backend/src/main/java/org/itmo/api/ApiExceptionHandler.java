package org.itmo.api;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.Nullable;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;

import java.net.URI;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.*;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            org.springframework.web.context.request.WebRequest request) {

        String msg = "Request body is invalid JSON or has wrong types";
        if (ex.getCause() instanceof InvalidFormatException ife) {
            msg = "Invalid value '" + ife.getValue() + "' for property '" +
                    (ife.getPath().isEmpty() ? "" : ife.getPath().get(0).getFieldName()) + "'";
        }
        return respond(HttpStatus.BAD_REQUEST, "bad_request", msg, request, java.util.Map.of("cause", ex.getClass().getSimpleName()));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            org.springframework.web.context.request.WebRequest request) {

        java.util.Map<String, String> fields = ex.getBindingResult().getFieldErrors().stream()
                .collect(java.util.stream.Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage, (a,b)->a));
        return respond(HttpStatus.BAD_REQUEST, "validation_failed", "Validation failed", request, java.util.Map.of("fields", fields));
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            org.springframework.web.context.request.WebRequest request) {

        return respond(HttpStatus.BAD_REQUEST, "bad_request", "Missing parameter: " + ex.getParameterName(), request, null);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest req) {
        java.util.Map<String, String> fields = ex.getConstraintViolations().stream()
                .collect(java.util.stream.Collectors.toMap(
                        v -> {
                            String p = v.getPropertyPath().toString();
                            int i = p.lastIndexOf('.');
                            return i >= 0 ? p.substring(i + 1) : p;
                        },
                        ConstraintViolation::getMessage,
                        (a,b)->a));
        return respond(HttpStatus.BAD_REQUEST, "validation_failed", "Validation failed", req.getRequestURI(), java.util.Map.of("fields", fields));
    }

    @ExceptionHandler({NoSuchElementException.class, jakarta.persistence.EntityNotFoundException.class})
    public ResponseEntity<Object> handleNotFound(RuntimeException ex, jakarta.servlet.http.HttpServletRequest req) {
        String msg = java.util.Optional.ofNullable(ex.getMessage())
                .filter(s -> !s.isBlank())
                .orElse("Resource not found");
        return respond(HttpStatus.NOT_FOUND, "not_found", msg, req.getRequestURI(), null);
    }

    @ExceptionHandler({DataIntegrityViolationException.class, SQLIntegrityConstraintViolationException.class})
    public ResponseEntity<Object> handleIntegrity(Exception ex, HttpServletRequest req) {
        return respond(HttpStatus.CONFLICT, "data_integrity_violation", "Data integrity violation", req.getRequestURI(), null);
    }

    @ExceptionHandler(PSQLException.class)
    public ResponseEntity<Object> handlePSQL(PSQLException ex, HttpServletRequest req) {
        log.warn("Database error: {}", ex.getMessage());
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "db_error", "Database error", req.getRequestURI(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleOther(Exception ex, HttpServletRequest req) {
        log.error("Unhandled error", ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error", "Unexpected server error", req.getRequestURI(), null);
    }

    @ExceptionHandler(DeletionBlockedException.class)
    public ResponseEntity<?> handleDeletionBlocked(
            DeletionBlockedException ex,
            HttpServletRequest request
    ) {
        var body = Map.of(
                "error", "deletion_blocked",
                "message", ex.getMessage(),
                "entity", ex.getEntity(),
                "id", ex.getId(),
                "usageCount", ex.getUsageCount(),
                "blockingCityIds", ex.getBlockingCityIds(),
                "status", 409,
                "path", request.getRequestURI(),
                "timestamp", java.time.OffsetDateTime.now().toString()
        );

        return ResponseEntity.status(409).body(body);
    }

    @ExceptionHandler(RelatedEntityNotFound.class)
    public ResponseEntity<Map<String,Object>> handleRelatedNotFound(RelatedEntityNotFound ex,
                                                                    HttpServletRequest req) {
        var body = new java.util.LinkedHashMap<String,Object>();
        body.put("error", "related_entity_not_found");
        body.put("message", ex.getMessage());
        body.put("entity", ex.getEntity());
        body.put("id", ex.getId());
        body.put("status", 404);
        body.put("path", req.getRequestURI());
        body.put("timestamp", java.time.OffsetDateTime.now().toString());
        return ResponseEntity.status(404).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String,Object>> handleIllegalArgument(IllegalArgumentException ex,
                                                                    HttpServletRequest req) {
        var body = new java.util.LinkedHashMap<String,Object>();
        body.put("error", "bad_request");
        body.put("message", ex.getMessage());
        body.put("status", 400);
        body.put("path", req.getRequestURI());
        body.put("timestamp", java.time.OffsetDateTime.now().toString());
        return ResponseEntity.badRequest().body(body);
    }




    private ResponseEntity<Object> respond(
            HttpStatus status,
            String code,
            String msg,
            org.springframework.web.context.request.WebRequest request,
            @Nullable java.util.Map<String, Object> details) {

        String desc = java.util.Optional.ofNullable(request.getDescription(false)).orElse("");
        String path = desc.startsWith("uri=") ? desc.substring(4) : desc;
        return respond(status, code, msg, path, details);
    }

    private ResponseEntity<Object> respond(
            HttpStatus status,
            String code,
            String msg,
            String path,
            @Nullable java.util.Map<String, Object> details) {

        ErrorResponse body = new ErrorResponse(code, msg, status.value(), path, details);
        return new ResponseEntity<>(body, status);
    }
}