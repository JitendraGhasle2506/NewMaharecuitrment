package com.maharecruitment.gov.in.attendance.repository;

public interface AttendanceDepartmentSummaryProjection {

    Long getDepartmentId();

    String getDepartmentName();

    Long getTotalEmployees();

    Long getPresentEmployees();
}
