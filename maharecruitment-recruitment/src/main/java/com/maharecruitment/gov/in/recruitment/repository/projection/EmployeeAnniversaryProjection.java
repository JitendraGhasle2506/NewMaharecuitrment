package com.maharecruitment.gov.in.recruitment.repository.projection;

import java.time.LocalDate;

public interface EmployeeAnniversaryProjection {

    Long getEmployeeId();

    String getFullName();

    LocalDate getMarriageDate();
}
