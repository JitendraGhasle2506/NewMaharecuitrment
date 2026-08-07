package com.maharecruitment.gov.in.attendance.repository;

import java.time.LocalTime;

public interface AttendanceEmployeeDetailProjection {

    Long getEmployeeId();

    String getEmployeeCode();

    String getFullName();

    String getRecruitmentType();

    String getAttendanceStatus();

    LocalTime getCheckInTime();
}
