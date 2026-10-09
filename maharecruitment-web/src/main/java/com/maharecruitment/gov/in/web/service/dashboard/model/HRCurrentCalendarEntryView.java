package com.maharecruitment.gov.in.web.service.dashboard.model;

import java.time.LocalDate;

public record HRCurrentCalendarEntryView(
        Long id,
        String title,
        String type,
        LocalDate startDate,
        LocalDate endDate,
        String description,
        String backgroundPhotoToken,
        long daysUntilStart
) {
    public boolean spansMultipleDays() {
        return endDate != null && endDate.isAfter(startDate);
    }
}
