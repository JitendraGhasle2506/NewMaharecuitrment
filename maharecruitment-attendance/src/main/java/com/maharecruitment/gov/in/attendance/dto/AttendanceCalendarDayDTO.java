package com.maharecruitment.gov.in.attendance.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AttendanceCalendarDayDTO {

    private final LocalDate date;
    private final boolean currentMonth;
    private final boolean today;
    private final boolean holiday;
    private final boolean weekOff;
    private final boolean workingDay;
    private final String holidayRemark;
}
