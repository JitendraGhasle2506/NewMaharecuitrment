package com.maharecruitment.gov.in.master.mapper;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.maharecruitment.gov.in.master.dto.ManpowerDesignationMasterResponse;
import com.maharecruitment.gov.in.master.dto.ResourceLevelRefResponse;
import com.maharecruitment.gov.in.master.entity.ManpowerDesignationMaster;
import com.maharecruitment.gov.in.master.entity.ResourceLevelExperience;

@Component
public class ManpowerDesignationMasterMapper {

    private final ResourceLevelExperienceMapper resourceLevelExperienceMapper;

    public ManpowerDesignationMasterMapper(ResourceLevelExperienceMapper resourceLevelExperienceMapper) {
        this.resourceLevelExperienceMapper = resourceLevelExperienceMapper;
    }

    public ManpowerDesignationMasterResponse toResponse(ManpowerDesignationMaster entity) {
        Set<ResourceLevelRefResponse> levels = entity.getLevels() == null
                ? new LinkedHashSet<>()
                : entity.getLevels().stream()
                        .map(resourceLevelExperienceMapper::toRefResponse)
                        .collect(Collectors.toCollection(LinkedHashSet::new));

        return ManpowerDesignationMasterResponse.builder()
                .designationId(entity.getDesignationId())
                .category(entity.getCategory())
                .designationName(entity.getDesignationName())
                .roleName(entity.getRoleName())
                .educationalQualification(entity.getEducationalQualification())
                .certification(entity.getCertification())
                .activeFlag(entity.getActiveFlag())
                .levels(levels)
                .createdAt(entity.getCreatedDateTime())
                .updatedAt(entity.getUpdatedDateTime())
                .build();
    }

    public Set<ResourceLevelExperience> normalizeLevels(Set<ResourceLevelExperience> levels) {
        if (levels == null) {
            return new LinkedHashSet<>();
        }
        return levels.stream().collect(Collectors.toCollection(LinkedHashSet::new));
    }
}

