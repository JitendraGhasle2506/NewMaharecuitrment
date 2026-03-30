package com.maharecruitment.gov.in.attendance.dto;

import lombok.Data;

@Data
public class ManualAttendanceSummaryDTO {
    private Long userId; // This is the Employee ID in ManualAttendanceRequestEntity
    private String employeeName;
    private String employeeCode;
    private String projectName;
    private int pendingCount;
}
