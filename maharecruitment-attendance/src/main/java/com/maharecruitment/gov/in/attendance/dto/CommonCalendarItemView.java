package com.maharecruitment.gov.in.attendance.dto;

import java.time.LocalDate;

import com.maharecruitment.gov.in.attendance.entity.CalendarEntryType;

public record CommonCalendarItemView(
        Long id,
        String source,
        LocalDate date,
        LocalDate endDate,
        String title,
        CalendarEntryType entryType,
        String description) {

    public boolean isHoliday() {
        return entryType == CalendarEntryType.HOLIDAY;
    }

    public boolean isDateRange() {
        return endDate != null && endDate.isAfter(date);
    }

    public LocalDate effectiveEndDate() {
        return endDate == null ? date : endDate;
    }
}
