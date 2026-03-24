package com.maharecruitment.gov.in.attendance.dto;


import java.util.List;

import lombok.Data;


@Data
public class AttendanceRegisterDTO {

    private Long empId;
    private String orgId;

    // Employee Details
    private String name;
    private String designation;
    private String email;
    private String mobile;
    private String photoPath;
    private String requestId;
    // Organization Details
    private String organization;
    private String division;
    private String officeLocation;
    private String level;
    private String projectName;

    // Attendance Filters
    private String dateRange;   // Month-Year (02-2026)

    // Today's Activity
    private String inTime;
    private String outTime;
    private String avgResponseTime;
  
    private String todayInTime;
    private String todayOutTime;
    private String todayStatus;
    private List<AttendanceDayDTO> attendanceDays;
    private List<AttendanceDayDTO> presentDays;
    private Double maxOutHour;
    private Long userId;
}