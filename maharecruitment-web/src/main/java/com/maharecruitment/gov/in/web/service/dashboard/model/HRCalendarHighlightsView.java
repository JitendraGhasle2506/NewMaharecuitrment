package com.maharecruitment.gov.in.web.service.dashboard.model;

import java.time.LocalDate;
import java.util.List;

public record HRCalendarHighlightsView(
        LocalDate today,
        LocalDate holidayWindowEnd,
        List<HRCurrentCalendarEntryView> currentEvents,
        List<HRUpcomingHolidayView> upcomingHolidays
) {
}
