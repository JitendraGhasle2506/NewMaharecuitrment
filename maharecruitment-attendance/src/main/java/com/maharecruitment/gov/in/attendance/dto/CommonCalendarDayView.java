package com.maharecruitment.gov.in.attendance.dto;

import java.time.LocalDate;
import java.util.List;

public record CommonCalendarDayView(
        LocalDate date,
        boolean currentMonth,
        boolean today,
        boolean weekend,
        boolean holiday,
        boolean workingDay,
        Long workingDayId,
        List<CommonCalendarItemView> items) {
}
