package com.maharecruitment.gov.in.recruitment.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.maharecruitment.gov.in.recruitment.service.model.AgencyCandidateInterviewScheduleInput;
import com.maharecruitment.gov.in.recruitment.service.model.AgencyCandidateSubmissionInput;
import com.maharecruitment.gov.in.recruitment.service.model.AgencyShortlistedCandidateProjectView;
import com.maharecruitment.gov.in.recruitment.service.model.AgencySelectedCandidateProjectView;
import com.maharecruitment.gov.in.recruitment.service.model.AgencySelectedCandidateView;
import com.maharecruitment.gov.in.recruitment.service.model.AgencyShortlistedCandidateView;
import com.maharecruitment.gov.in.recruitment.service.model.AgencySubmittedCandidateView;

public interface RecruitmentAgencyCandidateService {

    List<AgencySubmittedCandidateView> getSubmittedCandidates(Long recruitmentNotificationId, Long agencyId);

    List<AgencyShortlistedCandidateProjectView> getShortlistedCandidateProjects(Long agencyId);

    List<AgencyShortlistedCandidateView> getShortlistedCandidates(Long agencyId);

    Page<AgencyShortlistedCandidateView> getShortlistedCandidates(
            Long agencyId,
            Long recruitmentNotificationId,
            Pageable pageable);

    List<AgencySelectedCandidateProjectView> getSelectedCandidateProjects(Long agencyId);

    List<AgencySelectedCandidateView> getSelectedCandidates(Long agencyId);

    List<AgencySelectedCandidateView> getSelectedCandidates(Long agencyId, Long recruitmentNotificationId);

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

    void withdrawCandidate(
            Long recruitmentNotificationId,
            Long recruitmentInterviewDetailId,
            Long agencyId);
}
