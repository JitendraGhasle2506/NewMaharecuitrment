package com.maharecruitment.gov.in.master.dto;

import com.maharecruitment.gov.in.master.entity.AgencyStatus;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AgencyStatusUpdateRequest {

    @NotNull(message = "Agency status is required")
    private AgencyStatus status;
}
