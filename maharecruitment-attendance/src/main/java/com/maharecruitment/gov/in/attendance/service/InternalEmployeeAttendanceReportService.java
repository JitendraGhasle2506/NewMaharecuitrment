package com.maharecruitment.gov.in.attendance.service;

import com.maharecruitment.gov.in.attendance.service.model.InternalAttendanceReportFilter;
import com.maharecruitment.gov.in.attendance.service.model.InternalAttendanceReportView;

public interface InternalEmployeeAttendanceReportService {

    InternalAttendanceReportView buildReport(InternalAttendanceReportFilter filter);
}
