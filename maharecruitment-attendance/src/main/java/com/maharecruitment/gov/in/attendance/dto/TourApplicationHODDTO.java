package com.maharecruitment.gov.in.attendance.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class TourApplicationHODDTO {
    private Long tourId;
    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private String designation;
    private String hodRemarks;
    private String tourCategory;
    private String timePeriod;
    private LocalDate startDate;
    private LocalDate endDate;
    private String description;
    private LocalDateTime applicationDate;
    private String status;
}
