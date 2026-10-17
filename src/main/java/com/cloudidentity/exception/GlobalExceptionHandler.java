package com.cloudidentity.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequestException(BadRequestException ex, HttpServletRequest request) {
        log.warn("Bad request: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResourceException(DuplicateResourceException ex, HttpServletRequest request) {
        log.warn("Conflict resource: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    @ExceptionHandler(AWSIntegrationException.class)
    public ResponseEntity<ErrorResponse> handleAWSIntegrationException(AWSIntegrationException ex, HttpServletRequest request) {
        log.error("AWS SDK Integration error: ", ex);
        return buildErrorResponse(HttpStatus.BAD_GATEWAY, "AWS Service integration error: " + ex.getMessage(), request, null);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException ex, HttpServletRequest request) {
        log.warn("Invalid login credentials for request: {}", request.getRequestURI());
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid username or password.", request, null);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabledException(DisabledException ex, HttpServletRequest request) {
        log.warn("Account is disabled: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.FORBIDDEN, "Your account has been disabled. Please contact an administrator.", request, null);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUsernameNotFoundException(UsernameNotFoundException ex, HttpServletRequest request) {
        log.warn("User login failed: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "User authentication failed.", request, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied on path {}: {}", request.getRequestURI(), ex.getMessage());
        return buildErrorResponse(HttpStatus.FORBIDDEN, "You do not have permission to perform this action.", request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.warn("Validation error on path {}", request.getRequestURI());
        Map<String, String> validationErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            validationErrors.put(fieldName, errorMessage);
        });
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Validation failed for request parameters.", request, validationErrors);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled internal server error occurred on path {}: ", request.getRequestURI(), ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred. Please try again later.", request, null);
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(
            HttpStatus status, String message, HttpServletRequest request, Map<String, String> validationErrors) {
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .validationErrors(validationErrors)
                .build();
        return new ResponseEntity<>(response, status);
    }
}
