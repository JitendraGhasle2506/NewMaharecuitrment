package com.maharecruitment.gov.in.recruitment.service;

import java.util.List;

import com.maharecruitment.gov.in.recruitment.service.model.AgencyCandidateInterviewScheduleInput;
import com.maharecruitment.gov.in.recruitment.service.model.AgencyInternalAssessmentCandidateView;
import com.maharecruitment.gov.in.recruitment.service.model.AgencyInternalAssessmentDetailView;
import com.maharecruitment.gov.in.recruitment.service.model.AgencyInternalAssessmentProjectView;

public interface RecruitmentAgencyInternalAssessmentService {

    List<AgencyInternalAssessmentProjectView> getAssessmentSubmittedProjects(Long agencyId);

    List<AgencyInternalAssessmentCandidateView> getAssessmentSubmittedCandidates(
            Long agencyId,
            Long recruitmentNotificationId);

    AgencyInternalAssessmentDetailView getAssessmentSubmittedCandidateDetail(
            Long agencyId,
            Long recruitmentNotificationId,
            Long recruitmentInterviewDetailId);

    void scheduleLevelTwoInterview(
            Long agencyId,
            Long agencyUserId,
            Long recruitmentNotificationId,
            Long recruitmentInterviewDetailId,
            AgencyCandidateInterviewScheduleInput scheduleInput);
}
