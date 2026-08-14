package com.maharecruitment.gov.in.attendance.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class EmployeeAttendanceRequestDTO {

    private String requestType;
    private String requestTypeLabel;
    private LocalDateTime submittedAt;
    private LocalDate startDate;
    private LocalDate endDate;
    private String category;
    private String details;
    private String inTime;
    private String outTime;
    private String status;
    private String reviewerRemarks;
}
