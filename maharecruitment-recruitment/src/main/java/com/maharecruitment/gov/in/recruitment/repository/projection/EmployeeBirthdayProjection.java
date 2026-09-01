package com.maharecruitment.gov.in.recruitment.repository.projection;

import java.time.LocalDate;

public interface EmployeeBirthdayProjection {

    Long getEmployeeId();

    String getFullName();

    LocalDate getDateOfBirth();
}
