package com.maharecruitment.gov.in.attendance.service.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class InternalAttendanceReportView {

    private InternalAttendanceReportFilter filter;
    private String monthName;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime generatedAt;
    private int daysInMonth;
    private List<LocalDate> calendarDays;
    private InternalAttendanceReportSummary summary;
    private List<InternalAttendanceReportRow> rows;
}
