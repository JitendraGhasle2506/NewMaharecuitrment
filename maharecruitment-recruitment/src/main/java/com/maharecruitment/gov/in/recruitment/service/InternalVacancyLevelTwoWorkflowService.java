package com.maharecruitment.gov.in.recruitment.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.maharecruitment.gov.in.recruitment.service.model.DepartmentCandidateFinalDecision;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyLevelTwoCandidateSummaryView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyLevelTwoPanelUserOptionView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyLevelTwoWorkflowDetailView;

public interface InternalVacancyLevelTwoWorkflowService {

    Page<InternalVacancyLevelTwoCandidateSummaryView> getScheduledCandidatePage(String search, Pageable pageable);

    InternalVacancyLevelTwoWorkflowDetailView getWorkflowDetail(Long recruitmentInterviewDetailId);

    List<InternalVacancyLevelTwoPanelUserOptionView> getEligiblePanelUsers();

    void assignInterviewPanel(
            Long recruitmentInterviewDetailId,
            String actorEmail,
            List<Long> panelUserIds);

    void requestInterviewTimeChange(
            Long recruitmentInterviewDetailId,
            String actorEmail,
            String changeReason);

    void applyFinalDecision(
            Long recruitmentInterviewDetailId,
            String actorEmail,
            DepartmentCandidateFinalDecision finalDecision,
            String decisionRemarks);
}
