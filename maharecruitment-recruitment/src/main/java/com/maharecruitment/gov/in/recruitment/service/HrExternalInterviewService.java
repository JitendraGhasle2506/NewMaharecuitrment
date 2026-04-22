package com.maharecruitment.gov.in.recruitment.service;

import java.util.List;

import com.maharecruitment.gov.in.recruitment.service.model.HrExternalInterviewCandidateView;
import com.maharecruitment.gov.in.recruitment.service.model.HrExternalInterviewWorkflowDetailView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyLevelTwoPanelUserOptionView;

public interface HrExternalInterviewService {

    List<HrExternalInterviewCandidateView> getExternalInterviewsForHr();

    List<InternalVacancyLevelTwoPanelUserOptionView> getEligiblePanelUsers();

    void assignInterviewPanel(Long recruitmentInterviewDetailId, List<Long> panelUserIds, String actorEmail);

    HrExternalInterviewWorkflowDetailView getInterviewWorkflowDetail(Long recruitmentInterviewDetailId);

    void submitHrFinalDecision(Long recruitmentInterviewDetailId, String decisionRemarks, String actorEmail);

}
