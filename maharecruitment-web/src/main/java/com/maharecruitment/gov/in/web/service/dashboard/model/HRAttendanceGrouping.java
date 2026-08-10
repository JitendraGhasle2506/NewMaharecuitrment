package com.maharecruitment.gov.in.web.service.dashboard.model;

import java.util.Locale;
import java.util.Optional;

public enum HRAttendanceGrouping {
    CELL,
    DESIGNATION,
    DEPARTMENT;

    public static Optional<HRAttendanceGrouping> from(String value) {
        if (value == null || value.isBlank()) {
            return Optional.of(CELL);
        }
        try {
            return Optional.of(valueOf(value.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
