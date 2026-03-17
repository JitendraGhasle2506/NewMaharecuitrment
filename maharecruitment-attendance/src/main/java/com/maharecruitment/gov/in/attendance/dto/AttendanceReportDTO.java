package com.maharecruitment.gov.in.attendance.dto;

import java.util.Map;

import lombok.Data;

@Data
public class AttendanceReportDTO {
    private Long userId;
    private String requestId;
    private String employeeName;
    private String designation;
    private String department;
    private String subDepartment;
    private String projectName;
    private String level;
    private String agencyName;
    private Map<Integer, String> dailyStatus; // Day (1-31) -> Status (P/A/H/W)
}
