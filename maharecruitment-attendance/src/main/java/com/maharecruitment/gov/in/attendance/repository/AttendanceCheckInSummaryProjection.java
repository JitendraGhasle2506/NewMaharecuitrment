package com.maharecruitment.gov.in.attendance.repository;

public interface AttendanceCheckInSummaryProjection {

    Long getPresentCount();

    Long getCheckedInCount();

    Long getEarlyCount();

    Long getStandardCount();

    Long getLateCount();
}
