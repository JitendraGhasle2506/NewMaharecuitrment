package com.maharecruitment.gov.in.recruitment.service.model;

import java.time.LocalDateTime;

import com.maharecruitment.gov.in.recruitment.entity.InternalVacancyOpeningStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InternalVacancyOpeningSummaryView {

    private Long internalVacancyOpeningId;
    private String requestId;
    private Long projectId;
    private String projectName;
    private int designationCount;
    private long totalVacancies;
    private InternalVacancyOpeningStatus status;
    private LocalDateTime createdDateTime;
}
