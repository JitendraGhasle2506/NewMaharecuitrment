package com.maharecruitment.gov.in.department.repository.projection;

import java.time.LocalDate;

import lombok.Getter;

@Getter
public class DepartmentOnboardedEmployeeView {

    private final Long employeeId;
    private final String employeeCode;
    private final String requestId;
    private final String candidateName;
    private final String candidateEmail;
    private final String candidateMobile;
    private final String levelCode;
    private final LocalDate joiningDate;
    private final LocalDate onboardingDate;
    private final LocalDate resignationDate;
    private final String status;
    private final String designationName;

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
}
