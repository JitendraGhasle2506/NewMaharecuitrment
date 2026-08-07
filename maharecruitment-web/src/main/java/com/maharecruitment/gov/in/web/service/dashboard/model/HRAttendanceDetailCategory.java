package com.maharecruitment.gov.in.web.service.dashboard.model;

import java.util.Locale;
import java.util.Optional;

public enum HRAttendanceDetailCategory {
    TOTAL("All Employees", "Active employees included in the attendance register."),
    PRESENT("Present Employees", "Employees marked present for the selected working day."),
    ABSENT("Absent / Not Marked", "Employees without a present attendance mark for the working day."),
    CHECKED_IN("Recorded Check-ins", "Employees with a recorded check-in time."),
    EARLY("Early Check-ins", "Employees who checked in before 9:45 AM."),
    STANDARD("Regular Check-ins", "Employees who checked in from 9:45 AM to before 10:15 AM."),
    LATE("Late Check-ins", "Employees who checked in from 10:15 AM through 11:00 AM."),
    AFTER_ELEVEN("After 11:00 AM", "Employees who checked in after 11:00 AM.");

    private final String title;
    private final String description;

    HRAttendanceDetailCategory(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public static Optional<HRAttendanceDetailCategory> from(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(value.trim().replace('-', '_').toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
