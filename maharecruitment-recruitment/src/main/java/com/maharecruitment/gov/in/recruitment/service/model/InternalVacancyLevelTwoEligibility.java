package com.maharecruitment.gov.in.recruitment.service.model;

import java.math.BigDecimal;

import com.maharecruitment.gov.in.recruitment.entity.RecruitmentAssessmentFeedbackEntity;

/** Score-only eligibility rules for moving an internal candidate to Round L2. */
public final class InternalVacancyLevelTwoEligibility {

    public static final int MAXIMUM_MARKS = 20;
    public static final int MINIMUM_PERCENTAGE = 60;
    private static final BigDecimal MINIMUM_SCORE = BigDecimal.valueOf(12);

    private InternalVacancyLevelTwoEligibility() {
    }

    public static boolean isQualified(RecruitmentAssessmentFeedbackEntity assessment) {
        return assessment != null
                && isQualified(
                        assessment.getCommunicationSkillMarks(),
                        assessment.getTechnicalSkillMarks(),
                        assessment.getLeadershipQualityMarks(),
                        assessment.getRelevantExperienceMarks());
    }

    public static boolean isQualified(
            Integer communicationMarks,
            Integer technicalMarks,
            Integer leadershipMarks,
            Integer relevantExperienceMarks) {
        if (communicationMarks == null
                || technicalMarks == null
                || leadershipMarks == null
                || relevantExperienceMarks == null) {
            return false;
        }

        int obtainedMarks = communicationMarks + technicalMarks + leadershipMarks + relevantExperienceMarks;
        return obtainedMarks * 100 > MAXIMUM_MARKS * MINIMUM_PERCENTAGE;
    }

    public static boolean isQualified(BigDecimal totalScore) {
        return totalScore != null && totalScore.compareTo(MINIMUM_SCORE) > 0;
    }
}
