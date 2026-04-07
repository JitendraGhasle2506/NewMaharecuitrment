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
public class WorkOrderEmployeeView {

    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private String designationName;
    private String levelCode;
    private LocalDate joiningDate;
    private String employmentStatus;

    public Long employeeId() {
        return employeeId;
    }

    public String employeeCode() {
        return employeeCode;
    }

    public String employeeName() {
        return employeeName;
    }

    public String designationName() {
        return designationName;
    }

    public String levelCode() {
        return levelCode;
    }

    public LocalDate joiningDate() {
        return joiningDate;
    }

    public String employmentStatus() {
        return employmentStatus;
    }
}
