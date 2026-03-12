package com.maharecruitment.gov.in.department.service;

import java.util.List;

import com.maharecruitment.gov.in.recruitment.service.model.DepartmentCandidateFinalDecision;
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentCandidateReviewDecision;
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentInterviewAssessmentSubmissionInput;
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentInterviewWorkflowDetailView;
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentShortlistingDetailView;
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentShortlistingProjectView;

public interface DepartmentCandidateShortlistingService {

    List<DepartmentShortlistingProjectView> getProjectQueue(String actorEmail);

    DepartmentShortlistingDetailView getShortlistingDetail(Long recruitmentNotificationId, String actorEmail);

    void reviewCandidate(
            Long recruitmentNotificationId,
            Long recruitmentInterviewDetailId,
            DepartmentCandidateReviewDecision reviewDecision,
            String reviewRemarks,
            String actorEmail);

    DepartmentInterviewWorkflowDetailView getInterviewWorkflowDetail(
            Long recruitmentNotificationId,
            Long recruitmentInterviewDetailId,
            String actorEmail);

    void requestInterviewTimeChange(
            Long recruitmentNotificationId,
            Long recruitmentInterviewDetailId,
            String changeReason,
            String actorEmail);

    void submitInterviewAssessment(
            Long recruitmentNotificationId,
            Long recruitmentInterviewDetailId,
            DepartmentInterviewAssessmentSubmissionInput submissionInput,
            String actorEmail);

    void applyFinalSelectionDecision(
            Long recruitmentNotificationId,
            Long recruitmentInterviewDetailId,
            DepartmentCandidateFinalDecision finalDecision,
            String decisionRemarks,
            String actorEmail);
}
