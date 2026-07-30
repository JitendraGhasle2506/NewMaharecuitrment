package com.maharecruitment.gov.in.recruitment.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyLevelTwoFeedbackSubmissionInput;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyLevelTwoPanelCandidateView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyLevelTwoPanelWorkflowDetailView;

public interface InternalVacancyLevelTwoPanelReviewService {

    Page<InternalVacancyLevelTwoPanelCandidateView> getAssignedCandidatePage(
            String actorEmail,
            String search,
            Pageable pageable);

    InternalVacancyLevelTwoPanelWorkflowDetailView getWorkflowDetail(
            String actorEmail,
            Long recruitmentInterviewDetailId);

    void submitFeedback(
            String actorEmail,
            Long recruitmentInterviewDetailId,
            InternalVacancyLevelTwoFeedbackSubmissionInput submissionInput);
}
