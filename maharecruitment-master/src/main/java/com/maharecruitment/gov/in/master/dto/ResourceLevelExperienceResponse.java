package com.maharecruitment.gov.in.master.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ResourceLevelExperienceResponse {

    private Long levelId;
    private String levelCode;
    private String levelName;
    private BigDecimal minExperience;
    private BigDecimal maxExperience;
    private String activeFlag;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

