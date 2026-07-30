package com.maharecruitment.gov.in.web.dto.mobile;

import java.time.Instant;
import java.util.List;

public record MobileApiError(
        String code,
        String message,
        Instant timestamp,
        List<FieldError> fieldErrors) {

    public MobileApiError {
        fieldErrors = fieldErrors == null ? List.of() : List.copyOf(fieldErrors);
    }

    public static MobileApiError of(String code, String message) {
        return new MobileApiError(code, message, Instant.now(), List.of());
    }

    public static MobileApiError withFields(String code, String message, List<FieldError> fieldErrors) {
        return new MobileApiError(code, message, Instant.now(), fieldErrors);
    }

    public record FieldError(String field, String message) {
    }
}
