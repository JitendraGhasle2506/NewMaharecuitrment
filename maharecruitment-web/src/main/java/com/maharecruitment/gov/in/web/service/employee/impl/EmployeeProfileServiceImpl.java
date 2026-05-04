package com.maharecruitment.gov.in.web.service.employee.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.web.dto.agency.AgencyPreOnboardingForm;
import com.maharecruitment.gov.in.web.service.employee.EmployeeProfileService;
import com.maharecruitment.gov.in.web.service.hr.HROnboardingPageService;
import com.maharecruitment.gov.in.web.service.hr.model.EmployeeOnboardingDetailView;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EmployeeProfileServiceImpl implements EmployeeProfileService {

    private final EmployeeRepository employeeRepository;
    private final HROnboardingPageService hrOnboardingPageService;

    public EmployeeProfileServiceImpl(
            EmployeeRepository employeeRepository,
            HROnboardingPageService hrOnboardingPageService) {
        this.employeeRepository = employeeRepository;
        this.hrOnboardingPageService = hrOnboardingPageService;
    }

    @Override
    public EmployeeOnboardingDetailView loadCurrentEmployeeProfile(String loginEmail) {
        if (!StringUtils.hasText(loginEmail)) {
            throw new RecruitmentNotificationException("Employee login email is required.");
        }

        EmployeeEntity employee = resolveEmployeeProfile(loginEmail.trim());
        if (!hasOnboardingDetails(employee)) {
            throw new RecruitmentNotificationException("Employee onboarding details are not available.");
        }

        return buildDetailView(employee);
    }

    private EmployeeEntity resolveEmployeeProfile(String loginEmail) {
        List<EmployeeEntity> employeeProfiles = employeeRepository.findDetailedProfilesByEmail(loginEmail);
        if (employeeProfiles.isEmpty()) {
            throw new RecruitmentNotificationException("Employee profile not found for the logged-in user.");
        }
        if (employeeProfiles.size() > 1) {
            log.warn("Multiple employee profiles found for loginEmail={}. Selecting the highest-priority record.",
                    loginEmail);
        }

        return employeeProfiles.stream()
                .filter(this::hasOnboardingDetails)
                .findFirst()
                .orElse(employeeProfiles.getFirst());
    }

    private EmployeeOnboardingDetailView buildDetailView(EmployeeEntity employee) {
        AgencyPreOnboardingForm onboardingForm = hrOnboardingPageService
                .loadOnboardingForm(employee.getPreOnboarding().getPreOnboardingId());
        return new EmployeeOnboardingDetailView(
                employee.getEmployeeId(),
                employee.getEmployeeCode(),
                employee.getStatus(),
                employee.getRecruitmentType(),
                employee.getResignationDate(),
                onboardingForm);
    }

    private boolean hasOnboardingDetails(EmployeeEntity employee) {
        return employee != null
                && employee.getPreOnboarding() != null
                && employee.getPreOnboarding().getPreOnboardingId() != null;
    }
}
