package com.maharecruitment.gov.in.web.service.agency;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.maharecruitment.gov.in.web.dto.agency.AgencyPreOnboardingForm;
import com.maharecruitment.gov.in.web.service.agency.model.AgencyOnboardedEmployeeView;
import com.maharecruitment.gov.in.web.service.agency.model.AgencyOnboardingCandidateView;

public interface AgencyOnboardingPageService {

    AgencyPreOnboardingForm loadPreOnboardingForm(String actorEmail, Long recruitmentInterviewDetailId);

    void savePreOnboarding(String actorEmail, Long recruitmentInterviewDetailId, AgencyPreOnboardingForm form);

    Page<AgencyOnboardedEmployeeView> getOnboardedEmployees(String actorEmail, String search, Pageable pageable);

    Page<AgencyOnboardedEmployeeView> getEmployeesByStatus(String actorEmail, String status, String search, Pageable pageable);
    Page<AgencyOnboardedEmployeeView> getOnboardedEmployees(String actorEmail, Pageable pageable);

    Page<AgencyOnboardedEmployeeView> getEmployeesByStatus(String actorEmail, String status, Pageable pageable);

    void markEmployeeResigned(String actorEmail, Long employeeId, java.time.LocalDate resignationDate);

    List<AgencyOnboardingCandidateView> getOnboardingReadyCandidates(String actorEmail);
}
