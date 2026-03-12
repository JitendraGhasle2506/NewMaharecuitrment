package com.maharecruitment.gov.in.recruitment.service;

import com.maharecruitment.gov.in.recruitment.service.model.DepartmentCandidateFinalDecision;
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentInterviewAssessmentSubmissionInput;
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentInterviewWorkflowDetailView;

public interface RecruitmentDepartmentInterviewWorkflowService {

    DepartmentInterviewWorkflowDetailView getInterviewWorkflowDetail(
            Long departmentRegistrationId,
            Long recruitmentNotificationId,
            Long recruitmentInterviewDetailId);

    void requestInterviewTimeChange(
            Long departmentRegistrationId,
            Long departmentUserId,
            Long recruitmentNotificationId,
            Long recruitmentInterviewDetailId,
            String changeReason);

    void submitInterviewAssessment(
            Long departmentRegistrationId,
            Long departmentUserId,
            Long recruitmentNotificationId,
            Long recruitmentInterviewDetailId,
            DepartmentInterviewAssessmentSubmissionInput submissionInput);

    void applyFinalSelectionDecision(
            Long departmentRegistrationId,
            Long departmentUserId,
            Long recruitmentNotificationId,
            Long recruitmentInterviewDetailId,
            DepartmentCandidateFinalDecision finalDecision,
            String decisionRemarks);
}
