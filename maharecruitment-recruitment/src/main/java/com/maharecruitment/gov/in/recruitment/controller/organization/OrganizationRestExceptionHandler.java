package com.maharecruitment.gov.in.recruitment.controller.organization;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.maharecruitment.gov.in.master.dto.ApiResponse;
import com.maharecruitment.gov.in.master.exception.BusinessValidationException;
import com.maharecruitment.gov.in.master.exception.DuplicateResourceException;
import com.maharecruitment.gov.in.master.exception.ResourceNotFoundException;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;

@RestControllerAdvice(basePackages = "com.maharecruitment.gov.in.recruitment.controller.organization")
public class OrganizationRestExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleDuplicate(DuplicateResourceException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), null);
    }

    @ExceptionHandler({
            BusinessValidationException.class,
            RecruitmentNotificationException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleBadRequest(RuntimeException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        return build(HttpStatus.BAD_REQUEST, "Validation failed", errors);
    }

    private ResponseEntity<ApiResponse<Map<String, Object>>> build(
            HttpStatus status,
            String message,
            Map<String, Object> data) {
        return ResponseEntity.status(status).body(ApiResponse.of(message, data));
    }
}
