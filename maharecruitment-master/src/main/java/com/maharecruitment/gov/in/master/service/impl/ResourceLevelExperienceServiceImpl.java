package com.maharecruitment.gov.in.master.service.impl;

import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.master.dto.ResourceLevelExperienceRequest;
import com.maharecruitment.gov.in.master.dto.ResourceLevelExperienceResponse;
import com.maharecruitment.gov.in.master.entity.ResourceLevelExperience;
import com.maharecruitment.gov.in.master.exception.BusinessValidationException;
import com.maharecruitment.gov.in.master.exception.DuplicateResourceException;
import com.maharecruitment.gov.in.master.exception.ResourceNotFoundException;
import com.maharecruitment.gov.in.master.mapper.ResourceLevelExperienceMapper;
import com.maharecruitment.gov.in.master.repository.ResourceLevelExperienceRepository;
import com.maharecruitment.gov.in.master.service.ResourceLevelExperienceService;

@Service
@Transactional(readOnly = true)
public class ResourceLevelExperienceServiceImpl implements ResourceLevelExperienceService {

    private static final String ACTIVE = "Y";
    private static final String INACTIVE = "N";

    private final ResourceLevelExperienceRepository repository;
    private final ResourceLevelExperienceMapper mapper;

    public ResourceLevelExperienceServiceImpl(
            ResourceLevelExperienceRepository repository,
            ResourceLevelExperienceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public ResourceLevelExperienceResponse create(ResourceLevelExperienceRequest request) {
        validateExperienceRange(request.getMinExperience(), request.getMaxExperience());
        ensureUniqueLevelCode(request.getLevelCode(), null);

        ResourceLevelExperience entity = ResourceLevelExperience.builder()
                .levelCode(request.getLevelCode())
                .levelName(request.getLevelName())
                .minExperience(request.getMinExperience())
                .maxExperience(request.getMaxExperience())
                .activeFlag(normalizeActiveFlag(request.getActiveFlag()))
                .build();

        return mapper.toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public ResourceLevelExperienceResponse update(Long levelId, ResourceLevelExperienceRequest request) {
        ResourceLevelExperience entity = repository.findById(levelId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource level not found for id: " + levelId));

        validateExperienceRange(request.getMinExperience(), request.getMaxExperience());
        ensureUniqueLevelCode(request.getLevelCode(), levelId);

        entity.setLevelCode(request.getLevelCode());
        entity.setLevelName(request.getLevelName());
        entity.setMinExperience(request.getMinExperience());
        entity.setMaxExperience(request.getMaxExperience());
        entity.setActiveFlag(normalizeActiveFlag(request.getActiveFlag()));

        return mapper.toResponse(repository.save(entity));
    }

    @Override
    public ResourceLevelExperienceResponse getById(Long levelId, boolean includeInactive) {
        ResourceLevelExperience entity = includeInactive
                ? repository.findById(levelId)
                        .orElseThrow(() -> new ResourceNotFoundException("Resource level not found for id: " + levelId))
                : repository.findByLevelIdAndActiveFlagIgnoreCase(levelId, ACTIVE)
                        .orElseThrow(() -> new ResourceNotFoundException("Active resource level not found for id: " + levelId));

        return mapper.toResponse(entity);
    }

    @Override
    public Page<ResourceLevelExperienceResponse> getAll(boolean includeInactive, Pageable pageable) {
        Page<ResourceLevelExperience> page = includeInactive
                ? repository.findAll(pageable)
                : repository.findByActiveFlagIgnoreCase(ACTIVE, pageable);
        return page.map(mapper::toResponse);
    }

    @Override
    @Transactional
    public void softDelete(Long levelId) {
        ResourceLevelExperience entity = repository.findById(levelId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource level not found for id: " + levelId));
        entity.setActiveFlag(INACTIVE);
        repository.save(entity);
    }

    @Override
    @Transactional
    public void restore(Long levelId) {
        ResourceLevelExperience entity = repository.findById(levelId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource level not found for id: " + levelId));
        entity.setActiveFlag(ACTIVE);
        repository.save(entity);
    }

    private void ensureUniqueLevelCode(String levelCode, Long excludeId) {
        if (repository.existsByLevelCodeExcludingId(levelCode, excludeId)) {
            throw new DuplicateResourceException("Level code already exists: " + levelCode);
        }
    }

    private void validateExperienceRange(java.math.BigDecimal min, java.math.BigDecimal max) {
        if (min != null && max != null && max.compareTo(min) < 0) {
            throw new BusinessValidationException("Maximum experience must be greater than or equal to minimum experience");
        }
    }

    private String normalizeActiveFlag(String value) {
        if (value == null || value.isBlank()) {
            return ACTIVE;
        }
        return INACTIVE.equals(value.trim().toUpperCase(Locale.ROOT)) ? INACTIVE : ACTIVE;
    }
}

