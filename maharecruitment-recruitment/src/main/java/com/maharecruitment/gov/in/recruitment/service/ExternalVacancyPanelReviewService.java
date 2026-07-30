package com.maharecruitment.gov.in.recruitment.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.maharecruitment.gov.in.recruitment.service.model.ExternalVacancyFeedbackSubmissionInput;
import com.maharecruitment.gov.in.recruitment.service.model.ExternalVacancyPanelCandidateView;
import com.maharecruitment.gov.in.recruitment.service.model.ExternalVacancyPanelWorkflowDetailView;

public interface ExternalVacancyPanelReviewService {

    Page<ExternalVacancyPanelCandidateView> getAssignedCandidatePage(
            String actorEmail,
            String search,
            Pageable pageable);

    ExternalVacancyPanelWorkflowDetailView getWorkflowDetail(
            String actorEmail,
            Long recruitmentInterviewDetailId);

    void submitFeedback(
            String actorEmail,
            Long recruitmentInterviewDetailId,
            ExternalVacancyFeedbackSubmissionInput submissionInput);
}
