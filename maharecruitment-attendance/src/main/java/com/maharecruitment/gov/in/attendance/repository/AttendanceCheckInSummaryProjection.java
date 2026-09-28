package com.maharecruitment.gov.in.attendance.repository;

public interface AttendanceCheckInSummaryProjection {

    Long getPresentCount();

    Long getInternalPresentCount();

    Long getMahaitPresentCount();

    Long getCheckedInCount();

    Long getEarlyCount();

    Long getStandardCount();

    Long getLateCount();

    Long getAfterElevenCount();
}
