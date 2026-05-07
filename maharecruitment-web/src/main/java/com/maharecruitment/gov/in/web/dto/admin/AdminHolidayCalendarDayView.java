package com.maharecruitment.gov.in.web.dto.admin;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminHolidayCalendarDayView {

    private final LocalDate date;
    private final boolean currentMonth;
    private final boolean today;
    private final boolean holiday;
    private final boolean weekend;
    private final boolean workingDayHoliday;
    private final String holidayName;
    private final boolean weekOffWorkingDay;
    private final Long weekOffWorkingDayId;
}
