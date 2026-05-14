package com.maharecruitment.gov.in.recruitment.service;

import java.math.BigDecimal;
import java.util.List;

import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyAssessmentCommand;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyAssessmentView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyConsolidatedAssessmentView;

public interface InternalVacancyAssessmentService {

    void submitAssessment(InternalVacancyAssessmentCommand command, String actorEmail);

    InternalVacancyConsolidatedAssessmentView getConsolidatedAssessment(Long interviewDetailId);

    List<InternalVacancyAssessmentView> getIndividualAssessments(Long interviewDetailId);

    /**
     * Returns only the assessment submitted by the given actor (actorEmail) for this interview.
     * Returns null if the current panel has not yet submitted their assessment.
     * This enforces panel-level data isolation on the feedback form.
     */
    InternalVacancyAssessmentView getMyAssessment(Long interviewDetailId, String actorEmail);
}
