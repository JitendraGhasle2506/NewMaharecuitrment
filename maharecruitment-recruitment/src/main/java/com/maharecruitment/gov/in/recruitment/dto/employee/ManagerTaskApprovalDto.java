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
public class ManagerTaskApprovalDto {
    private Long taskId;
    private String employeeName;
    private String projectName;
    private String moduleName;
    private String taskDescription;
    private LocalDate taskDate;
    private Double hours;
    private String status;
}
