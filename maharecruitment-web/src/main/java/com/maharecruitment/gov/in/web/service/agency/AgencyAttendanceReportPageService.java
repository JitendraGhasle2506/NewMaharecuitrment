package com.maharecruitment.gov.in.web.service.agency;

import com.maharecruitment.gov.in.web.service.agency.model.AgencyAttendanceReportPageView;

public interface AgencyAttendanceReportPageService {

    AgencyAttendanceReportPageView getAttendanceReport(
            String actorEmail,
            Integer month,
            Integer year,
            String employeeType,
            String search);
}
