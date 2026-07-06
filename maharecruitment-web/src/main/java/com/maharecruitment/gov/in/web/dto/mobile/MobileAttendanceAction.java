package com.maharecruitment.gov.in.web.dto.mobile;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum MobileAttendanceAction {
    CHECK_IN,
    CHECK_OUT;

    @JsonCreator
    public static MobileAttendanceAction from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim()
                .replace("-", "_")
                .replace(" ", "_")
                .toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "CHECK_IN", "CHECKIN", "IN" -> CHECK_IN;
            case "CHECK_OUT", "CHECKOUT", "OUT" -> CHECK_OUT;
            default -> throw new IllegalArgumentException("Invalid attendance flag.");
        };
    }
}
