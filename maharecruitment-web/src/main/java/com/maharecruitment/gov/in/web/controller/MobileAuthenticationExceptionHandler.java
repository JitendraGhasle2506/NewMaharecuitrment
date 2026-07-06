package com.maharecruitment.gov.in.web.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import com.maharecruitment.gov.in.web.dto.mobile.MobileApiError;
import com.maharecruitment.gov.in.web.exception.FileStorageException;
import com.maharecruitment.gov.in.web.service.mobile.MobileAttendanceException;
import com.maharecruitment.gov.in.web.service.mobile.MobileTokenConfigurationException;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice(assignableTypes = {
        MobileAuthenticationController.class,
        MobileAttendanceController.class
})
public class MobileAuthenticationExceptionHandler {

    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid username or password.";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<MobileApiError> handleValidation(MethodArgumentNotValidException ex) {
        List<MobileApiError.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new MobileApiError.FieldError(error.getField(), error.getDefaultMessage()))
                .toList();

        return ResponseEntity.badRequest().body(MobileApiError.withFields(
                "VALIDATION_FAILED",
                "Please provide valid request details.",
                fieldErrors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<MobileApiError> handleUnreadableRequest() {
        return ResponseEntity.badRequest().body(MobileApiError.of(
                "INVALID_REQUEST",
                "Request body must be valid JSON."));
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<MobileApiError> handleDisabledEmployee() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(MobileApiError.of(
                "EMPLOYEE_INACTIVE",
                "Active employee profile was not found for mobile login."));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<MobileApiError> handleAuthenticationFailure() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(MobileApiError.of(
                "INVALID_CREDENTIALS",
                INVALID_CREDENTIALS_MESSAGE));
    }

    @ExceptionHandler(MobileAttendanceException.class)
    public ResponseEntity<MobileApiError> handleMobileAttendance(MobileAttendanceException ex) {
        return ResponseEntity.status(ex.getStatus()).body(MobileApiError.of(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MissingServletRequestPartException.class,
            MethodArgumentTypeMismatchException.class,
            ConstraintViolationException.class
    })
    public ResponseEntity<MobileApiError> handleInvalidMultipartRequest() {
        return ResponseEntity.badRequest().body(MobileApiError.of(
                "INVALID_ATTENDANCE_REQUEST",
                "Please provide valid attendance details."));
    }

    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<MobileApiError> handleFileStorage(FileStorageException ex) {
        return ResponseEntity.badRequest().body(MobileApiError.of(
                "INVALID_IMAGE",
                ex.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<MobileApiError> handleMaxUploadSize() {
        return ResponseEntity.badRequest().body(MobileApiError.of(
                "IMAGE_TOO_LARGE",
                "Attendance image is too large."));
    }

    @ExceptionHandler(MobileTokenConfigurationException.class)
    public ResponseEntity<MobileApiError> handleTokenConfiguration() {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(MobileApiError.of(
                "TOKEN_CONFIGURATION_ERROR",
                "Authentication token service is not configured correctly."));
    }
}
