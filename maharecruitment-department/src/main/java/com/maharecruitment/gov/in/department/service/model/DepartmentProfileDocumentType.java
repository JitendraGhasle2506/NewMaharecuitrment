package com.maharecruitment.gov.in.department.service.model;

import java.util.Locale;

import com.maharecruitment.gov.in.department.exception.DepartmentApplicationException;

public enum DepartmentProfileDocumentType {
    GST,
    PAN,
    TAN;

    public static DepartmentProfileDocumentType from(String value) {
        if (value == null || value.isBlank()) {
            throw new DepartmentApplicationException("Document type is required.");
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);
        try {
            return DepartmentProfileDocumentType.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new DepartmentApplicationException("Unsupported document type: " + value);
        }
    }
}
