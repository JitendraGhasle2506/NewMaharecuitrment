package com.maharecruitment.gov.in.web.service.agency;

import java.util.List;

import com.maharecruitment.gov.in.web.dto.agency.AgencyPreOnboardingForm;
import com.maharecruitment.gov.in.web.service.agency.model.AgencyOnboardingCandidateView;

public interface AgencyOnboardingPageService {

    AgencyPreOnboardingForm loadPreOnboardingForm(String actorEmail, Long recruitmentInterviewDetailId);

    void savePreOnboarding(String actorEmail, Long recruitmentInterviewDetailId, AgencyPreOnboardingForm form);

    List<AgencyOnboardingCandidateView> getOnboardingReadyCandidates(String actorEmail);
}
