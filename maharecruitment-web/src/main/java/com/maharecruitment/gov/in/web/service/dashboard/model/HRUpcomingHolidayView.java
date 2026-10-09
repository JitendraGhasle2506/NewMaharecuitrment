package com.maharecruitment.gov.in.web.service.dashboard.model;

import java.time.LocalDate;

public record HRUpcomingHolidayView(
        Long id,
        String name,
        LocalDate date,
        long daysUntil
) {
}
