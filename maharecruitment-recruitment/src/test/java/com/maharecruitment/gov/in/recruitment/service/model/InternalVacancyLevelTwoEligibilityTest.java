package com.maharecruitment.gov.in.recruitment.service.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.maharecruitment.gov.in.recruitment.entity.RecruitmentAssessmentFeedbackEntity;

class InternalVacancyLevelTwoEligibilityTest {

    @Test
    void qualifiesCandidateAboveSixtyPercentWithoutRecommendation() {
        RecruitmentAssessmentFeedbackEntity assessment = assessmentWithMarks(4, 3, 3, 3);
        assessment.setRecommendationStatus(null);

        assertTrue(InternalVacancyLevelTwoEligibility.isQualified(assessment));
        assertTrue(InternalVacancyLevelTwoEligibility.isQualified(BigDecimal.valueOf(13)));
    }

    @Test
    void doesNotQualifyCandidateAtExactlySixtyPercent() {
        RecruitmentAssessmentFeedbackEntity assessment = assessmentWithMarks(3, 3, 3, 3);
        assessment.setRecommendationStatus("RECOMMENDED");

        assertFalse(InternalVacancyLevelTwoEligibility.isQualified(assessment));
        assertFalse(InternalVacancyLevelTwoEligibility.isQualified(BigDecimal.valueOf(12)));
    }

    private RecruitmentAssessmentFeedbackEntity assessmentWithMarks(
            int communication,
            int technical,
            int leadership,
            int relevantExperience) {
        RecruitmentAssessmentFeedbackEntity assessment = new RecruitmentAssessmentFeedbackEntity();
        assessment.setCommunicationSkillMarks(communication);
        assessment.setTechnicalSkillMarks(technical);
        assessment.setLeadershipQualityMarks(leadership);
        assessment.setRelevantExperienceMarks(relevantExperience);
        return assessment;
    }
}
