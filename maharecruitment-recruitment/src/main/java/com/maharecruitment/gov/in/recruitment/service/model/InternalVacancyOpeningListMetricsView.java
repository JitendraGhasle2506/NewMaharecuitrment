package com.maharecruitment.gov.in.recruitment.service.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InternalVacancyOpeningListMetricsView {

    private long totalOpenings;
    private long draftOpenings;
    private long activeOpenings;
    private long closedOpenings;
}
