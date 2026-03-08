package com.maharecruitment.gov.in.master.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.maharecruitment.gov.in.master.dto.ResourceLevelExperienceRequest;
import com.maharecruitment.gov.in.master.dto.ResourceLevelExperienceResponse;

public interface ResourceLevelExperienceService {

    ResourceLevelExperienceResponse create(ResourceLevelExperienceRequest request);

    ResourceLevelExperienceResponse update(Long levelId, ResourceLevelExperienceRequest request);

    ResourceLevelExperienceResponse getById(Long levelId, boolean includeInactive);

    Page<ResourceLevelExperienceResponse> getAll(boolean includeInactive, Pageable pageable);

    void softDelete(Long levelId);

    void restore(Long levelId);
}

