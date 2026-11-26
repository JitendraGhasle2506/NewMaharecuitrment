package com.maharecruitment.gov.in.web.service.dashboard.model;

import java.util.Locale;
import java.util.Optional;

/**
 * Recruitment type an HR user can filter the attendance register by.
 * The enum name matches the value stored in {@code employee_master.recruitment_type}.
 */
public enum HRAttendanceEmployeeType {
    INTERNAL("Internal"),
    EXTERNAL("External"),
    MAHAIT("MAHAIT");

    private final String label;

    HRAttendanceEmployeeType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static Optional<HRAttendanceEmployeeType> from(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(value.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
