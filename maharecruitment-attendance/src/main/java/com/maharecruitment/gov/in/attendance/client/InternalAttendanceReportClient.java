package com.maharecruitment.gov.in.attendance.client;

import java.time.LocalDate;
import java.util.List;

import com.maharecruitment.gov.in.attendance.client.model.InternalAttendanceDayRecord;

public interface InternalAttendanceReportClient {

    List<InternalAttendanceDayRecord> fetchAttendanceReport(
            String uniqueCode,
            LocalDate startDate,
            LocalDate endDate);
}
