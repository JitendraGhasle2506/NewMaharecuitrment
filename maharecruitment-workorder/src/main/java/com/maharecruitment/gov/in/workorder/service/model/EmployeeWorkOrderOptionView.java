package com.maharecruitment.gov.in.workorder.service.model;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeWorkOrderOptionView {

    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private String requestId;
    private String projectName;
    private String agencyName;
    private String departmentName;
    private String designationName;
    private String levelCode;
    private String status;
    private LocalDate joiningDate;
}
