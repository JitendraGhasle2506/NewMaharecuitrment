package com.maharecruitment.gov.in.recruitment.repository.projection;

/** Cell authority names loaded in bulk after the employee directory is paginated. */
public interface EmployeeCellAuthorityProjection {
    Long getEmployeeId();
    Long getAuthorityUserId();
    String getAuthorityName();
}
