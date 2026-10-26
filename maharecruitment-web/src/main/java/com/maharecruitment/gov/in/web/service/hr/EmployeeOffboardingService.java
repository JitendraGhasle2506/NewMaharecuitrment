package com.maharecruitment.gov.in.web.service.hr;

import java.time.LocalDate;

/** Shared transactional implementation invoked only after the caller's role and ownership checks. */
public interface EmployeeOffboardingService {
    enum Source { HR, PIP }
    void exitEmployee(Long employeeId, LocalDate lastWorkingDate, String reason, String actorLogin, Source source);
}
