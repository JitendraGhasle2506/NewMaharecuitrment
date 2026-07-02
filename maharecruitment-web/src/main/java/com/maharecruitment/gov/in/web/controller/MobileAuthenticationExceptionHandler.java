package com.maharecruitment.gov.in.web.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.maharecruitment.gov.in.web.dto.mobile.MobileApiError;
import com.maharecruitment.gov.in.web.service.mobile.MobileTokenConfigurationException;

@RestControllerAdvice(assignableTypes = MobileAuthenticationController.class)
public class MobileAuthenticationExceptionHandler {

    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid username or password.";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<MobileApiError> handleValidation(MethodArgumentNotValidException ex) {
        List<MobileApiError.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new MobileApiError.FieldError(error.getField(), error.getDefaultMessage()))
                .toList();

        return ResponseEntity.badRequest().body(MobileApiError.withFields(
                "VALIDATION_FAILED",
                "Please provide valid login details.",
                fieldErrors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<MobileApiError> handleUnreadableRequest() {
        return ResponseEntity.badRequest().body(MobileApiError.of(
                "INVALID_REQUEST",
                "Request body must be valid JSON."));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<MobileApiError> handleAuthenticationFailure() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(MobileApiError.of(
                "INVALID_CREDENTIALS",
                INVALID_CREDENTIALS_MESSAGE));
    }

    @ExceptionHandler(MobileTokenConfigurationException.class)
    public ResponseEntity<MobileApiError> handleTokenConfiguration() {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(MobileApiError.of(
                "TOKEN_CONFIGURATION_ERROR",
                "Authentication token service is not configured correctly."));
    }
}
