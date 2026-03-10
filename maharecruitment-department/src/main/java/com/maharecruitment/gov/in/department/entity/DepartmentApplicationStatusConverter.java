package com.maharecruitment.gov.in.department.entity;

import java.util.Locale;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class DepartmentApplicationStatusConverter implements AttributeConverter<DepartmentApplicationStatus, String> {

    @Override
    public String convertToDatabaseColumn(DepartmentApplicationStatus attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public DepartmentApplicationStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return DepartmentApplicationStatus.DRAFT;
        }

        String normalized = dbData.trim().toUpperCase(Locale.ROOT);

        if (normalized.matches("\\d+")) {
            return DepartmentApplicationStatus.fromStatusCode(Integer.parseInt(normalized));
        }

        switch (normalized) {
            case "SUBMITTED":
                return DepartmentApplicationStatus.SUBMITTED_TO_HR;
            case "UNDER_REVIEW":
                return DepartmentApplicationStatus.AUDITOR_REVIEW;
            case "APPROVED":
                return DepartmentApplicationStatus.COMPLETED;
            case "REJECTED":
                return DepartmentApplicationStatus.HR_REJECTED;
            default:
                return DepartmentApplicationStatus.valueOf(normalized);
        }
    }
}
