package com.maharecruitment.gov.in.attendance.service.model;

import java.time.LocalDate;
import java.util.Map;

import lombok.Data;

@Data
public class InternalAttendanceReportRow {

    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private String agencyName;
    private String designation;
    private String departmentName;
    private String subDepartmentName;
    private Long projectId;
    private String projectName;
    private String employeeStatus;
    private String requestId;
    private String levelCode;
    private LocalDate joiningDate;
    private Map<Integer, String> dailyStatus;
    private long presentCount;
    private long absentCount;
    private long leaveCount;
    private long compOffCount;
    private long holidayCount;
    private long weekOffCount;
    private long tourCount;
    private long payableDays;

    public long getAbsentTotalCount() {
        return absentCount + leaveCount;
    }
}
