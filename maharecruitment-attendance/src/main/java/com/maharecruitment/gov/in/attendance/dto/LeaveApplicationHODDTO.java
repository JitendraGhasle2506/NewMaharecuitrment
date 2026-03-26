package com.maharecruitment.gov.in.attendance.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class LeaveApplicationHODDTO {
    private Long leaveId;
    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private String leaveType;
    private String leaveCategory;
    private LocalDate startDate;
    private LocalDate endDate;
    private String description;
    private LocalDateTime applicationDate;
    private String status;
}
