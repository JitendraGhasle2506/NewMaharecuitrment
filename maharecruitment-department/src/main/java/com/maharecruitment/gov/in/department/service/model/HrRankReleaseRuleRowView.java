package com.maharecruitment.gov.in.department.service.model;

import java.time.LocalDate;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HrRankReleaseRuleRowView {

    private Integer rankNumber;

    private Integer releaseAfterDays;

    private Integer delayFromPreviousRankDays;

    private Integer effectiveDelayDays;

    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    private boolean active;
}
