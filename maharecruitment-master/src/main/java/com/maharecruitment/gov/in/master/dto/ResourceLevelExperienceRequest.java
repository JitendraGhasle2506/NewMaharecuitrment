package com.maharecruitment.gov.in.master.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResourceLevelExperienceRequest {

    @NotBlank(message = "Level code is required")
    private String levelCode;

    @NotBlank(message = "Level name is required")
    private String levelName;

    @NotNull(message = "Minimum experience is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Minimum experience must be zero or more")
    private BigDecimal minExperience;

    @NotNull(message = "Maximum experience is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Maximum experience must be zero or more")
    private BigDecimal maxExperience;

    private String activeFlag;
}

