package com.maharecruitment.gov.in.web.service.hr;

import java.time.LocalDate;

public interface EmployeeExitService {
    void exitEmployee(Long employeeId, LocalDate lastWorkingDate, String reason, String actorLogin);
}
