package com.maharecruitment.gov.in.recruitment.service.model;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InternalVacancyConsolidatedAssessmentView {
    private Long recruitmentInterviewDetailId;
    private String candidateName;
    private String requestId;
    private int totalPanels;
    private int submittedAssessments;
    private BigDecimal averageScore;
    private List<InternalVacancyAssessmentView> individualAssessments;
    private boolean isSelectionCriteriaMet; // e.g. at least 2 assessments
}
