package com.maharecruitment.gov.in.master.mapper;

import org.springframework.stereotype.Component;

import com.maharecruitment.gov.in.master.dto.ResourceLevelExperienceResponse;
import com.maharecruitment.gov.in.master.dto.ResourceLevelRefResponse;
import com.maharecruitment.gov.in.master.entity.ResourceLevelExperience;

@Component
public class ResourceLevelExperienceMapper {

    public ResourceLevelExperienceResponse toResponse(ResourceLevelExperience entity) {
        return ResourceLevelExperienceResponse.builder()
                .levelId(entity.getLevelId())
                .levelCode(entity.getLevelCode())
                .levelName(entity.getLevelName())
                .minExperience(entity.getMinExperience())
                .maxExperience(entity.getMaxExperience())
                .activeFlag(entity.getActiveFlag())
                .createdAt(entity.getCreatedDateTime())
                .updatedAt(entity.getUpdatedDateTime())
                .build();
    }

    public ResourceLevelRefResponse toRefResponse(ResourceLevelExperience entity) {
        return ResourceLevelRefResponse.builder()
                .levelId(entity.getLevelId())
                .levelCode(entity.getLevelCode())
                .levelName(entity.getLevelName())
                .build();
    }
}

