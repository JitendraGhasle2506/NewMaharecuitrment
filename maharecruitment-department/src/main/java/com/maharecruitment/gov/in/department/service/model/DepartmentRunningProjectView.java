package com.maharecruitment.gov.in.department.service.model;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DepartmentRunningProjectView {

    private String projectCode;
    private String projectName;
    private LocalDate startDate;
    private int allocatedEmployees;
    private String status;
    private boolean paymentPending;
    private Long applicationId;
}
