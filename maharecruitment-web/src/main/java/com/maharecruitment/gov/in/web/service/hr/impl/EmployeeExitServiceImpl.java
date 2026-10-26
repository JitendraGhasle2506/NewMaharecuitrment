package com.maharecruitment.gov.in.web.service.hr.impl;

import java.time.LocalDate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import com.maharecruitment.gov.in.web.service.hr.EmployeeExitService;
import com.maharecruitment.gov.in.web.service.hr.EmployeeOffboardingService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmployeeExitServiceImpl implements EmployeeExitService {
    private final EmployeeOffboardingService offboarding;

    @Override
    @PreAuthorize("hasAuthority('ROLE_HR')")
    public void exitEmployee(Long employeeId, LocalDate lastWorkingDate, String reason, String actorLogin) {
        offboarding.exitEmployee(employeeId, lastWorkingDate, reason, actorLogin, EmployeeOffboardingService.Source.HR);
    }
}