package com.maharecruitment.gov.in.recruitment.entity;

import java.util.Locale;

public enum EmployeeRecruitmentType {
    INTERNAL,
    EXTERNAL,
    MAHAIT;

    public static String normalizeOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
