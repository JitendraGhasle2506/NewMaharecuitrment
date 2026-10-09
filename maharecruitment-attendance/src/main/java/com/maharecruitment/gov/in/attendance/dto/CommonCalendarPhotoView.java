package com.maharecruitment.gov.in.attendance.dto;

public record CommonCalendarPhotoView(
        Long id,
        Long calendarEntryId,
        String originalName,
        String documentPathToken) {
}
