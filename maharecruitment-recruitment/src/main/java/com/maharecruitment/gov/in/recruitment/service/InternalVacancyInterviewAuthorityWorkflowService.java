package com.maharecruitment.gov.in.recruitment.service;

import com.maharecruitment.gov.in.recruitment.service.model.DepartmentInterviewAssessmentSubmissionInput;
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentInterviewWorkflowDetailView;

public interface InternalVacancyInterviewAuthorityWorkflowService {

    DepartmentInterviewWorkflowDetailView getInterviewWorkflowDetail(
            String actorEmail,
            String requestId,
            Long recruitmentInterviewDetailId);

    void submitInterviewAssessment(
            String actorEmail,
            String requestId,
            Long recruitmentInterviewDetailId,
            DepartmentInterviewAssessmentSubmissionInput submissionInput);
}
