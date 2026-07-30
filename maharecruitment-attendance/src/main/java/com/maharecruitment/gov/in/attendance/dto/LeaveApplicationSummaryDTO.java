package com.maharecruitment.gov.in.attendance.dto;

import lombok.Data;

@Data
public class LeaveApplicationSummaryDTO {
    private Long employeeId;
    private String employeeName;
    private String projectName;
    private Long pendingCount;
}
