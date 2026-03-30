package com.maharecruitment.gov.in.department.repository.projection;

import java.time.LocalDate;

import lombok.Data;

@Data
public class DepartmentOnboardedEmployeeView {

    private Long employeeId;
    private String employeeCode;
    private String requestId;
    private String candidateName;
    private String candidateEmail;
    private String candidateMobile;
    private String levelCode;
    private LocalDate joiningDate;
    private LocalDate onboardingDate;
    private LocalDate resignationDate;
    private String status;
    private String designationName;

    public DepartmentOnboardedEmployeeView(
            Long employeeId,
            String employeeCode,
            String requestId,
            String candidateName,
            String candidateEmail,
            String candidateMobile,
            String levelCode,
            LocalDate joiningDate,
            LocalDate onboardingDate,
            LocalDate resignationDate,
            String status,String designationName) {

        this.employeeId = employeeId;
        this.employeeCode = employeeCode;
        this.requestId = requestId;
        this.candidateName = candidateName;
        this.candidateEmail = candidateEmail;
        this.candidateMobile = candidateMobile;
        this.levelCode = levelCode;
        this.joiningDate = joiningDate;
        this.onboardingDate = onboardingDate;
        this.resignationDate = resignationDate;
        this.status = status;
        this.designationName = designationName;
    }

    // getters
}
