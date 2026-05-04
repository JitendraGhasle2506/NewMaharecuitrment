package com.maharecruitment.gov.in.attendance.service;

import java.time.LocalDate;

import com.maharecruitment.gov.in.attendance.service.model.InternalAttendanceSyncResult;

public interface InternalAttendanceSyncService {

    InternalAttendanceSyncResult syncScheduledAttendance();

    InternalAttendanceSyncResult syncAttendance(LocalDate startDate, LocalDate endDate);

    long countEligibleInternalEmployees();
}
