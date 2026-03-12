package com.maharecruitment.gov.in.recruitment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RecruitmentNotificationAgencyRankRequest {

    @NotNull(message = "Agency id is required.")
    private Long agencyId;

    @NotNull(message = "Rank number is required.")
    @Min(value = 1, message = "Rank number must be at least 1.")
    private Integer rankNumber;
}

