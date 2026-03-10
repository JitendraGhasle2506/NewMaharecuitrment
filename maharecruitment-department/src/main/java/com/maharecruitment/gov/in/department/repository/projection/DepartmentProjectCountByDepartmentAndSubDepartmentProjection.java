package com.maharecruitment.gov.in.department.repository.projection;

public interface DepartmentProjectCountByDepartmentAndSubDepartmentProjection {

    Long getDepartmentId();

    Long getSubDepartmentId();

    Long getProjectCount();
}
