package com.maharecruitment.gov.in.web.service.agency;

import java.util.List;

import com.maharecruitment.gov.in.recruitment.service.model.AgencyInternalAssessmentCandidateView;
import com.maharecruitment.gov.in.recruitment.service.model.AgencyInternalAssessmentDetailView;
import com.maharecruitment.gov.in.recruitment.service.model.AgencyInternalAssessmentProjectView;
import com.maharecruitment.gov.in.web.dto.agency.AgencyInterviewScheduleForm;

public interface AgencyInternalAssessmentPageService {

    List<AgencyInternalAssessmentProjectView> getAssessmentSubmittedProjects(String actorEmail);

    List<AgencyInternalAssessmentCandidateView> getAssessmentSubmittedCandidates(
            String actorEmail,
            Long recruitmentNotificationId);

    AgencyInternalAssessmentDetailView getAssessmentSubmittedCandidateDetail(
            String actorEmail,
            Long recruitmentNotificationId,
            Long recruitmentInterviewDetailId);

    void scheduleLevelTwoInterview(
            String actorEmail,
            Long recruitmentNotificationId,
            Long recruitmentInterviewDetailId,
            AgencyInterviewScheduleForm scheduleForm);
}
