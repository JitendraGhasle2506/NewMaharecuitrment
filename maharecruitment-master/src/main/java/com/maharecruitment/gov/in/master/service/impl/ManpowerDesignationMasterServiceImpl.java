package com.maharecruitment.gov.in.master.service.impl;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.master.dto.ManpowerDesignationMasterRequest;
import com.maharecruitment.gov.in.master.dto.ManpowerDesignationMasterResponse;
import com.maharecruitment.gov.in.master.entity.ManpowerDesignationMaster;
import com.maharecruitment.gov.in.master.entity.ResourceLevelExperience;
import com.maharecruitment.gov.in.master.exception.BusinessValidationException;
import com.maharecruitment.gov.in.master.exception.DuplicateResourceException;
import com.maharecruitment.gov.in.master.exception.ResourceNotFoundException;
import com.maharecruitment.gov.in.master.mapper.ManpowerDesignationMasterMapper;
import com.maharecruitment.gov.in.master.repository.ManpowerDesignationMasterRepository;
import com.maharecruitment.gov.in.master.repository.ResourceLevelExperienceRepository;
import com.maharecruitment.gov.in.master.service.ManpowerDesignationMasterService;

@Service
@Transactional(readOnly = true)
public class ManpowerDesignationMasterServiceImpl implements ManpowerDesignationMasterService {

    private static final String ACTIVE = "Y";
    private static final String INACTIVE = "N";

    private final ManpowerDesignationMasterRepository designationRepository;
    private final ResourceLevelExperienceRepository levelRepository;
    private final ManpowerDesignationMasterMapper mapper;

    public ManpowerDesignationMasterServiceImpl(
            ManpowerDesignationMasterRepository designationRepository,
            ResourceLevelExperienceRepository levelRepository,
            ManpowerDesignationMasterMapper mapper) {
        this.designationRepository = designationRepository;
        this.levelRepository = levelRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public ManpowerDesignationMasterResponse create(ManpowerDesignationMasterRequest request) {
        ensureUniqueDesignation(request.getCategory(), request.getDesignationName(), null);
        Set<ResourceLevelExperience> levels = resolveActiveLevels(request.getLevelIds());

        ManpowerDesignationMaster entity = ManpowerDesignationMaster.builder()
                .category(request.getCategory())
                .designationName(request.getDesignationName())
                .roleName(request.getRoleName())
                .educationalQualification(request.getEducationalQualification())
                .certification(request.getCertification())
                .activeFlag(normalizeActiveFlag(request.getActiveFlag()))
                .levels(mapper.normalizeLevels(levels))
                .build();

        return mapper.toResponse(designationRepository.save(entity));
    }

    @Override
    @Transactional
    public ManpowerDesignationMasterResponse update(Long designationId, ManpowerDesignationMasterRequest request) {
        ManpowerDesignationMaster entity = designationRepository.findById(designationId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Designation not found for id: " + designationId));

        ensureUniqueDesignation(request.getCategory(), request.getDesignationName(), designationId);
        Set<ResourceLevelExperience> levels = resolveActiveLevels(request.getLevelIds());

        entity.setCategory(request.getCategory());
        entity.setDesignationName(request.getDesignationName());
        entity.setRoleName(request.getRoleName());
        entity.setEducationalQualification(request.getEducationalQualification());
        entity.setCertification(request.getCertification());
        entity.setActiveFlag(normalizeActiveFlag(request.getActiveFlag()));
        entity.setLevels(mapper.normalizeLevels(levels));

        return mapper.toResponse(designationRepository.save(entity));
    }

    @Override
    public ManpowerDesignationMasterResponse getById(Long designationId, boolean includeInactive) {
        ManpowerDesignationMaster entity = includeInactive
                ? designationRepository.findById(designationId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Designation not found for id: " + designationId))
                : designationRepository.findByDesignationIdAndActiveFlagIgnoreCase(designationId, ACTIVE)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Active designation not found for id: " + designationId));

        return mapper.toResponse(entity);
    }

    @Override
    public Page<ManpowerDesignationMasterResponse> getAll(boolean includeInactive, Pageable pageable) {
        Page<ManpowerDesignationMaster> page = includeInactive
                ? designationRepository.findAll(pageable)
                : designationRepository.findByActiveFlagIgnoreCase(ACTIVE, pageable);
        return page.map(mapper::toResponse);
    }

    @Override
    @Transactional
    public void softDelete(Long designationId) {
        ManpowerDesignationMaster entity = designationRepository.findById(designationId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Designation not found for id: " + designationId));
        entity.setActiveFlag(INACTIVE);
        designationRepository.save(entity);
    }

    @Override
    @Transactional
    public void restore(Long designationId) {
        ManpowerDesignationMaster entity = designationRepository.findById(designationId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Designation not found for id: " + designationId));
        Set<ResourceLevelExperience> activeLevels = resolveActiveLevels(
                entity.getLevels().stream().map(ResourceLevelExperience::getLevelId).collect(Collectors.toSet()));
        entity.setLevels(activeLevels);
        entity.setActiveFlag(ACTIVE);
        designationRepository.save(entity);
    }

    private Set<ResourceLevelExperience> resolveActiveLevels(Set<Long> levelIds) {
        if (levelIds == null || levelIds.isEmpty()) {
            return new LinkedHashSet<>();
        }

        Set<Long> normalizedIds = levelIds.stream()
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (normalizedIds.isEmpty()) {
            return new LinkedHashSet<>();
        }

        Set<ResourceLevelExperience> levels = levelRepository.findByLevelIdInAndActiveFlagIgnoreCase(normalizedIds, ACTIVE)
                .stream()
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (levels.size() != normalizedIds.size()) {
            throw new BusinessValidationException("One or more levelIds are invalid or inactive");
        }

        return levels;
    }

    private void ensureUniqueDesignation(String category, String designationName, Long excludeId) {
        if (designationRepository.existsByCategoryAndDesignationNameExcludingId(category, designationName, excludeId)) {
            throw new DuplicateResourceException(
                    "Designation already exists for category '" + category + "' and name '" + designationName + "'");
        }
    }

    private String normalizeActiveFlag(String value) {
        if (value == null || value.isBlank()) {
            return ACTIVE;
        }
        return INACTIVE.equals(value.trim().toUpperCase(Locale.ROOT)) ? INACTIVE : ACTIVE;
    }
}

