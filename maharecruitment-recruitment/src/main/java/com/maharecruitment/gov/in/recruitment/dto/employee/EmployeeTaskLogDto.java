package com.maharecruitment.gov.in.recruitment.dto.employee;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeTaskLogDto {
    private Long taskId;
    private String projectName;
    private String otherProjectReason;
    private String moduleName;
    private String taskDescription;
    private LocalDate taskDate;
    private Double hours;
    private String payableStatus;
    private String inTime;
    private String startTime;
    private String endTime;
    private String status;
    private String managerRemarks;
    private boolean selected;
}
