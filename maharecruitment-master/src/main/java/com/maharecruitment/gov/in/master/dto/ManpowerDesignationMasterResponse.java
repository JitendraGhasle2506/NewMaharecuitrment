package com.maharecruitment.gov.in.master.dto;

import java.time.LocalDateTime;
import java.util.Set;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ManpowerDesignationMasterResponse {

    private Long designationId;
    private String category;
    private String designationName;
    private String roleName;
    private String educationalQualification;
    private String certification;
    private String activeFlag;
    private Set<ResourceLevelRefResponse> levels;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

