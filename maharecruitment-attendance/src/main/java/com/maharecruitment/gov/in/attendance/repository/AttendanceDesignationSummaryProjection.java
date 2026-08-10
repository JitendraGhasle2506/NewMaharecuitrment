package com.maharecruitment.gov.in.attendance.repository;

public interface AttendanceDesignationSummaryProjection {

    Long getDesignationId();

    String getDesignationName();

    Long getTotalEmployees();

    Long getPresentEmployees();
}
