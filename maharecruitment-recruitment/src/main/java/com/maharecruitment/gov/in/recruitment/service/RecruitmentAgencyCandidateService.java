package com.maharecruitment.gov.in.recruitment.service;

import java.util.List;

import com.maharecruitment.gov.in.recruitment.service.model.AgencyCandidateInterviewScheduleInput;
import com.maharecruitment.gov.in.recruitment.service.model.AgencyCandidateSubmissionInput;
import com.maharecruitment.gov.in.recruitment.service.model.AgencySubmittedCandidateView;

public interface RecruitmentAgencyCandidateService {

    List<AgencySubmittedCandidateView> getSubmittedCandidates(Long recruitmentNotificationId, Long agencyId);

    void submitCandidates(
            Long recruitmentNotificationId,
            Long agencyId,
            Long agencyUserId,
            Long designationVacancyId,
            List<AgencyCandidateSubmissionInput> candidateInputs);

    void scheduleInterview(
            Long recruitmentNotificationId,
            Long recruitmentInterviewDetailId,
            Long agencyId,
            Long agencyUserId,
            AgencyCandidateInterviewScheduleInput scheduleInput);
}
