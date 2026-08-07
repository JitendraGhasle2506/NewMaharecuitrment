package com.maharecruitment.gov.in.attendance.repository;

public interface AttendanceCellSummaryProjection {

    Long getCellId();

    String getCellName();

    String getWingName();

    Long getTotalEmployees();

    Long getPresentEmployees();
}
