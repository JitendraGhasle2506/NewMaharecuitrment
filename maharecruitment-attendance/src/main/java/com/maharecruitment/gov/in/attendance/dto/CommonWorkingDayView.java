package com.maharecruitment.gov.in.attendance.dto;

import java.time.LocalDate;

public record CommonWorkingDayView(
        Long id,
        LocalDate workingDate,
        String officeOrderOriginalName,
        String documentPathToken) {
}
