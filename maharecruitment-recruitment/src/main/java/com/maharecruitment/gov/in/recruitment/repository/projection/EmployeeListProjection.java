package com.maharecruitment.gov.in.recruitment.repository.projection;

import java.time.LocalDate;

public interface EmployeeListProjection {

    Long getEmployeeId();

    String getEmployeeCode();

    String getFullName();

    String getEmail();

    String getDesignation();

    LocalDate getMahaitJoiningDate();

    String getRecruitmentType();

    String getAgencyName();

    String getStatus();
}
