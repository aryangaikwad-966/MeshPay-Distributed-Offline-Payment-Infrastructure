package com.demo.upimesh.controller;

import com.demo.upimesh.model.ErrorResponse;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global Exception Handler
 * Provides unified error responses across all REST endpoints
 * Prevents information leakage through exception messages
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle authentication exceptions (invalid JWT, missing token, etc)
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex, WebRequest request) {
        
        log.warn("Authentication failed: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.UNAUTHORIZED.value())
            .error("UNAUTHORIZED")
            .message("Authentication failed. Please provide a valid token.")
            .path(request.getDescription(false).replace("uri=", ""))
            .build();
        
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handle authorization exceptions (insufficient roles, etc)
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {
        
        log.warn("Access denied: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.FORBIDDEN.value())
            .error("FORBIDDEN")
            .message("You do not have permission to access this resource.")
            .path(request.getDescription(false).replace("uri=", ""))
            .build();
        
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    /**
     * Handle rate limiting exceptions
     */
    @ExceptionHandler(RequestNotPermitted.class)
    public ResponseEntity<ErrorResponse> handleRateLimitException(
            RequestNotPermitted ex, WebRequest request) {
        
        log.warn("Rate limit exceeded");
        
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.TOO_MANY_REQUESTS.value())
            .error("RATE_LIMIT_EXCEEDED")
            .message("Too many requests. Please try again later.")
            .path(request.getDescription(false).replace("uri=", ""))
            .build();
        
        return new ResponseEntity<>(error, HttpStatus.TOO_MANY_REQUESTS);
    }

    /**
     * Handle validation errors (invalid request body)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        log.warn("Validation error: {}", ex.getMessage());
        
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });
        
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("VALIDATION_ERROR")
            .message("Request validation failed")
            .details(fieldErrors)
            .path(request.getDescription(false).replace("uri=", ""))
            .build();
        
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle insufficient funds exceptions
     */
    @ExceptionHandler(com.demo.upimesh.model.InsufficientFundsException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientFundsException(
            com.demo.upimesh.model.InsufficientFundsException ex, WebRequest request) {
        
        log.warn("Insufficient funds: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("INSUFFICIENT_FUNDS")
            .message("Insufficient funds in sender account")
            .path(request.getDescription(false).replace("uri=", ""))
            .build();
        
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle crypto/decryption errors
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleCryptoException(
            RuntimeException ex, WebRequest request) {
        
        String errorMessage = ex.getMessage();
        
        if (errorMessage.contains("decryption") || errorMessage.contains("Decrypt")) {
            log.error("Decryption error (generic response)");
            
            ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("INVALID_PACKET")
                .message("Packet validation failed")
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
            
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
        
        log.error("Unexpected error: {}", errorMessage, ex);
        
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error("INTERNAL_ERROR")
            .message("An internal server error occurred. Please try again later.")
            .path(request.getDescription(false).replace("uri=", ""))
            .build();
        
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Handle generic exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllExceptions(
            Exception ex, WebRequest request) throws Exception {
        
        // Let Spring handle resource not found (404) properly
        if (ex.getClass().getName().contains("NoResourceFoundException")) {
            throw ex; 
        }
        
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error("INTERNAL_ERROR")
            .message("An internal server error occurred.")
            .path(request.getDescription(false).replace("uri=", ""))
            .build();
        
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
