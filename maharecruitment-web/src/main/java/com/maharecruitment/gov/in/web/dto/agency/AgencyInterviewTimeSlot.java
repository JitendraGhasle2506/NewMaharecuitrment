package com.maharecruitment.gov.in.web.dto.agency;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.Optional;

public enum AgencyInterviewTimeSlot {

    MORNING("10:00 AM - 11:30 AM", LocalTime.of(10, 0)),
    AFTERNOON("2:00 PM - 5:00 PM", LocalTime.of(14, 0));

    private final String displayValue;
    private final LocalTime startTime;

    AgencyInterviewTimeSlot(String displayValue, LocalTime startTime) {
        this.displayValue = displayValue;
        this.startTime = startTime;
    }

    public String getDisplayValue() {
        return displayValue;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public static Optional<AgencyInterviewTimeSlot> fromDisplayValue(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String normalized = value.trim();
        return Arrays.stream(values())
                .filter(slot -> slot.displayValue.equalsIgnoreCase(normalized))
                .findFirst();
    }
}
