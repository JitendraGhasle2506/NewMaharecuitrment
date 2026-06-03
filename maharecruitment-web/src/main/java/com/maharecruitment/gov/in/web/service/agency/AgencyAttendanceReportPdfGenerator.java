package com.maharecruitment.gov.in.web.service.agency;

import com.maharecruitment.gov.in.attendance.service.model.GeneratedAttendanceReportDocument;
import com.maharecruitment.gov.in.web.service.agency.model.AgencyAttendanceReportPageView;

public interface AgencyAttendanceReportPdfGenerator {

    GeneratedAttendanceReportDocument generate(AgencyAttendanceReportPageView report);
}
