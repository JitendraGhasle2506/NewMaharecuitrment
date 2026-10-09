package com.maharecruitment.gov.in.web.service.dashboard.model;

import java.time.LocalDate;
import java.util.List;

public record HRCurrentCalendarEntryView(
        Long id,
        String title,
        String type,
        LocalDate startDate,
        LocalDate endDate,
        String description,
        List<String> photoTokens,
        long daysUntilStart
) {
    public HRCurrentCalendarEntryView {
        photoTokens = photoTokens == null ? List.of() : List.copyOf(photoTokens);
    }

    public String backgroundPhotoToken() {
        return photoTokens.isEmpty() ? null : photoTokens.getFirst();
    }

    public boolean spansMultipleDays() {
        return endDate != null && endDate.isAfter(startDate);
    }
}
