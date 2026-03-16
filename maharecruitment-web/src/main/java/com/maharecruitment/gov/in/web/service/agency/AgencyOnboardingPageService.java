package com.maharecruitment.gov.in.web.service.agency;

import java.util.List;

import com.maharecruitment.gov.in.web.dto.agency.AgencyPreOnboardingForm;
import com.maharecruitment.gov.in.web.service.agency.model.AgencyOnboardedEmployeeView;
import com.maharecruitment.gov.in.web.service.agency.model.AgencyOnboardingCandidateView;

public interface AgencyOnboardingPageService {

    AgencyPreOnboardingForm loadPreOnboardingForm(String actorEmail, Long recruitmentInterviewDetailId);

    void savePreOnboarding(String actorEmail, Long recruitmentInterviewDetailId, AgencyPreOnboardingForm form);

    List<AgencyOnboardedEmployeeView> getOnboardedEmployees(String actorEmail);

    List<AgencyOnboardedEmployeeView> getEmployeesByStatus(String actorEmail, String status);

    void markEmployeeResigned(String actorEmail, Long employeeId);

    List<AgencyOnboardingCandidateView> getOnboardingReadyCandidates(String actorEmail);
}
