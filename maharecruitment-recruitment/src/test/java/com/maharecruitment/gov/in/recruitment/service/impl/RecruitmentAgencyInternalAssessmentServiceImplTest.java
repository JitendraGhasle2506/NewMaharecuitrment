package com.maharecruitment.gov.in.recruitment.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyAssessmentView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyConsolidatedAssessmentView;

class RecruitmentAgencyInternalAssessmentServiceImplTest {

    @Test
    void agencyAssessmentDetailRedactsPanelIdentityButKeepsFeedback() {
        InternalVacancyAssessmentView panelFeedback = InternalVacancyAssessmentView.builder()
                .assessmentId(81L)
                .assessorName("Confidential Panel Member")
                .assessorType("USER")
                .technicalScore(BigDecimal.valueOf(4))
                .communicationScore(BigDecimal.valueOf(3))
                .leadershipScore(BigDecimal.valueOf(4))
                .relevantExperienceScore(BigDecimal.valueOf(3))
                .totalScore(BigDecimal.valueOf(14))
                .remarks("Recommended")
                .build();
        InternalVacancyConsolidatedAssessmentView source =
                InternalVacancyConsolidatedAssessmentView.builder()
                        .recruitmentInterviewDetailId(66L)
                        .candidateName("Candidate")
                        .requestId("REQ-20260825-I0001")
                        .totalPanels(2)
                        .submittedAssessments(1)
                        .averageScore(BigDecimal.valueOf(14))
                        .individualAssessments(List.of(panelFeedback))
                        .isSelectionCriteriaMet(false)
                        .build();

        InternalVacancyConsolidatedAssessmentView result =
                RecruitmentAgencyInternalAssessmentServiceImpl.redactPanelIdentities(source);
        InternalVacancyAssessmentView redactedFeedback = result.getIndividualAssessments().getFirst();

        assertNull(redactedFeedback.getAssessorName());
        assertNull(redactedFeedback.getAssessorType());
        assertEquals(BigDecimal.valueOf(14), redactedFeedback.getTotalScore());
        assertEquals("Recommended", redactedFeedback.getRemarks());
    }
}
