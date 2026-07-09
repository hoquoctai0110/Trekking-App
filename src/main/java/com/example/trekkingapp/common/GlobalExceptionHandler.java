package com.example.trekkingapp.common;

import com.example.trekkingapp.auth.EmailDeliveryException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException exception, HttpServletRequest request) {
        log.error("REQUEST_EXCEPTION requestId={} method={} path={} type={} message={}",
                RequestTracing.getRequestId(),
                request.getMethod(),
                request.getRequestURI(),
                exception.getClass().getName(),
                exception.getMessage(),
                exception);
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Validation failed");

        return ResponseEntity.badRequest().body(new ApiResponse<>(false, message, null));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException exception, HttpServletRequest request) {
        log.error("REQUEST_EXCEPTION requestId={} method={} path={} type={} message={}",
                RequestTracing.getRequestId(),
                request.getMethod(),
                request.getRequestURI(),
                exception.getClass().getName(),
                exception.getMessage(),
                exception);
        return ResponseEntity.badRequest().body(new ApiResponse<>(false, resolveMessage(exception), null));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalStateException(IllegalStateException exception, HttpServletRequest request) {
        log.error("REQUEST_EXCEPTION requestId={} method={} path={} type={} message={}",
                RequestTracing.getRequestId(),
                request.getMethod(),
                request.getRequestURI(),
                exception.getClass().getName(),
                exception.getMessage(),
                exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, resolveMessage(exception), null));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolationException(DataIntegrityViolationException exception, HttpServletRequest request) {
        log.error("REQUEST_EXCEPTION requestId={} method={} path={} type={} message={}",
                RequestTracing.getRequestId(),
                request.getMethod(),
                request.getRequestURI(),
                exception.getClass().getName(),
                exception.getMessage(),
                exception);
        String message = "Request violates a unique or required data constraint";
        String exceptionMessage = exception.getMostSpecificCause() == null
                ? exception.getMessage()
                : exception.getMostSpecificCause().getMessage();
        if (exceptionMessage != null) {
            String normalized = exceptionMessage.toLowerCase();
            if (normalized.contains("email")) {
                message = "Email already exists";
            } else if (normalized.contains("phone")) {
                message = "Phone already exists";
            }
        }

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiResponse<>(false, message, null));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException exception, HttpServletRequest request) {
        log.error("REQUEST_EXCEPTION requestId={} method={} path={} type={} message={}",
                RequestTracing.getRequestId(),
                request.getMethod(),
                request.getRequestURI(),
                exception.getClass().getName(),
                exception.getMessage(),
                exception);
        return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Invalid JSON request body", null));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        log.error("REQUEST_EXCEPTION requestId={} method={} path={} type={} name={} value={} requiredType={} message={}",
                RequestTracing.getRequestId(),
                request.getMethod(),
                request.getRequestURI(),
                exception.getClass().getName(),
                exception.getName(),
                exception.getValue(),
                exception.getRequiredType() == null ? null : exception.getRequiredType().getSimpleName(),
                exception.getMessage(),
                exception);

        String message = "Invalid value for parameter '" + exception.getName() + "'";
        return ResponseEntity.badRequest().body(new ApiResponse<>(false, message, null));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatusException(ResponseStatusException exception, HttpServletRequest request) {
        log.error("REQUEST_EXCEPTION requestId={} method={} path={} type={} message={}",
                RequestTracing.getRequestId(),
                request.getMethod(),
                request.getRequestURI(),
                exception.getClass().getName(),
                exception.getMessage(),
                exception);
        String message = exception.getReason();
        if (message == null || message.isBlank()) {
            message = resolveMessage(exception);
        }

        return ResponseEntity.status(exception.getStatusCode())
                .body(new ApiResponse<>(false, message, null));
    }

    @ExceptionHandler(EmailDeliveryException.class)
    public ResponseEntity<ApiResponse<Void>> handleEmailDeliveryException(EmailDeliveryException exception, HttpServletRequest request) {
        log.error("REQUEST_EXCEPTION requestId={} method={} path={} type={} message={}",
                RequestTracing.getRequestId(),
                request.getMethod(),
                request.getRequestURI(),
                exception.getClass().getName(),
                exception.getMessage(),
                exception);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiResponse<>(false, resolveMessage(exception), null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception exception, HttpServletRequest request) {
        log.error("REQUEST_EXCEPTION requestId={} method={} path={} type={} message={}",
                RequestTracing.getRequestId(),
                request.getMethod(),
                request.getRequestURI(),
                exception.getClass().getSimpleName(),
                exception.getMessage(),
                exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, resolveMessage(exception), null));
    }

    private String resolveMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }

        return message;
    }
}
