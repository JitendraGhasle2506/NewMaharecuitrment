package com.maharecruitment.gov.in.department.repository.projection;

public interface CellEmployeeCountProjection {

    String getCellName();

    Long getInternalEmployees();

    Long getExternalEmployees();
}
