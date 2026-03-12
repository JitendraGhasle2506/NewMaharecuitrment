package com.maharecruitment.gov.in.recruitment.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignRecruitmentNotificationRanksRequest {

    @Valid
    @NotEmpty(message = "At least one agency rank mapping is required.")
    private List<RecruitmentNotificationAgencyRankRequest> agencyRanks;
}

