package com.maharecruitment.gov.in.attendance.service;

import com.maharecruitment.gov.in.attendance.service.model.InternalAttendanceSyncResult;

public interface InternalAttendanceSyncService {

    InternalAttendanceSyncResult syncCurrentMonthAttendance();
}
