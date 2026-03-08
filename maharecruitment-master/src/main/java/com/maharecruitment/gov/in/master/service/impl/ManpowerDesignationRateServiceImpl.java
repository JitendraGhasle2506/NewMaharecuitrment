package com.maharecruitment.gov.in.master.service.impl;

import java.time.LocalDate;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.master.dto.ManpowerDesignationRateRequest;
import com.maharecruitment.gov.in.master.dto.ManpowerDesignationRateResponse;
import com.maharecruitment.gov.in.master.entity.ManpowerDesignationRate;
import com.maharecruitment.gov.in.master.exception.BusinessValidationException;
import com.maharecruitment.gov.in.master.exception.DuplicateResourceException;
import com.maharecruitment.gov.in.master.exception.ResourceNotFoundException;
import com.maharecruitment.gov.in.master.mapper.ManpowerDesignationRateMapper;
import com.maharecruitment.gov.in.master.repository.ManpowerDesignationMasterRepository;
import com.maharecruitment.gov.in.master.repository.ManpowerDesignationRateRepository;
import com.maharecruitment.gov.in.master.repository.ResourceLevelExperienceRepository;
import com.maharecruitment.gov.in.master.service.ManpowerDesignationRateService;

@Service
@Transactional(readOnly = true)
public class ManpowerDesignationRateServiceImpl implements ManpowerDesignationRateService {

    private static final String ACTIVE = "Y";
    private static final String INACTIVE = "N";

    private final ManpowerDesignationRateRepository rateRepository;
    private final ManpowerDesignationMasterRepository designationRepository;
    private final ResourceLevelExperienceRepository levelRepository;
    private final ManpowerDesignationRateMapper mapper;

    public ManpowerDesignationRateServiceImpl(
            ManpowerDesignationRateRepository rateRepository,
            ManpowerDesignationMasterRepository designationRepository,
            ResourceLevelExperienceRepository levelRepository,
            ManpowerDesignationRateMapper mapper) {
        this.rateRepository = rateRepository;
        this.designationRepository = designationRepository;
        this.levelRepository = levelRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public ManpowerDesignationRateResponse create(ManpowerDesignationRateRequest request) {
        validateEffectiveDateRange(request.getEffectiveFrom(), request.getEffectiveTo());
        validateActiveReferences(request.getDesignationId(), request.getLevelCode());
        ensureUniqueRate(request.getDesignationId(), request.getLevelCode(), request.getEffectiveFrom(), null);

        ManpowerDesignationRate entity = ManpowerDesignationRate.builder()
                .designationId(request.getDesignationId())
                .levelCode(request.getLevelCode())
                .grossMonthlyCtc(request.getGrossMonthlyCtc())
                .effectiveFrom(request.getEffectiveFrom())
                .effectiveTo(request.getEffectiveTo())
                .activeFlag(normalizeActiveFlag(request.getActiveFlag()))
                .build();

        return mapper.toResponse(rateRepository.save(entity));
    }

    @Override
    @Transactional
    public ManpowerDesignationRateResponse update(Long rateId, ManpowerDesignationRateRequest request) {
        ManpowerDesignationRate entity = rateRepository.findById(rateId)
                .orElseThrow(() -> new ResourceNotFoundException("Designation rate not found for id: " + rateId));

        validateEffectiveDateRange(request.getEffectiveFrom(), request.getEffectiveTo());
        validateActiveReferences(request.getDesignationId(), request.getLevelCode());
        ensureUniqueRate(request.getDesignationId(), request.getLevelCode(), request.getEffectiveFrom(), rateId);

        entity.setDesignationId(request.getDesignationId());
        entity.setLevelCode(request.getLevelCode());
        entity.setGrossMonthlyCtc(request.getGrossMonthlyCtc());
        entity.setEffectiveFrom(request.getEffectiveFrom());
        entity.setEffectiveTo(request.getEffectiveTo());
        entity.setActiveFlag(normalizeActiveFlag(request.getActiveFlag()));

        return mapper.toResponse(rateRepository.save(entity));
    }

    @Override
    public ManpowerDesignationRateResponse getById(Long rateId, boolean includeInactive) {
        ManpowerDesignationRate entity = includeInactive
                ? rateRepository.findById(rateId)
                        .orElseThrow(() -> new ResourceNotFoundException("Designation rate not found for id: " + rateId))
                : rateRepository.findByRateIdAndActiveFlagIgnoreCase(rateId, ACTIVE)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Active designation rate not found for id: " + rateId));

        return mapper.toResponse(entity);
    }

    @Override
    public Page<ManpowerDesignationRateResponse> getAll(Long designationId, boolean includeInactive, Pageable pageable) {
        Page<ManpowerDesignationRate> page;
        if (designationId == null) {
            page = includeInactive
                    ? rateRepository.findAll(pageable)
                    : rateRepository.findByActiveFlagIgnoreCase(ACTIVE, pageable);
        } else {
            page = includeInactive
                    ? rateRepository.findByDesignationId(designationId, pageable)
                    : rateRepository.findByDesignationIdAndActiveFlagIgnoreCase(designationId, ACTIVE, pageable);
        }

        return page.map(mapper::toResponse);
    }

    @Override
    @Transactional
    public void softDelete(Long rateId) {
        ManpowerDesignationRate entity = rateRepository.findById(rateId)
                .orElseThrow(() -> new ResourceNotFoundException("Designation rate not found for id: " + rateId));
        entity.setActiveFlag(INACTIVE);
        rateRepository.save(entity);
    }

    @Override
    @Transactional
    public void restore(Long rateId) {
        ManpowerDesignationRate entity = rateRepository.findById(rateId)
                .orElseThrow(() -> new ResourceNotFoundException("Designation rate not found for id: " + rateId));

        validateActiveReferences(entity.getDesignationId(), entity.getLevelCode());
        ensureUniqueRate(entity.getDesignationId(), entity.getLevelCode(), entity.getEffectiveFrom(), rateId);

        entity.setActiveFlag(ACTIVE);
        rateRepository.save(entity);
    }

    private void validateActiveReferences(Long designationId, String levelCode) {
        designationRepository.findByDesignationIdAndActiveFlagIgnoreCase(designationId, ACTIVE)
                .orElseThrow(() -> new BusinessValidationException(
                        "Designation must exist and be active for id: " + designationId));

        levelRepository.findByLevelCodeIgnoreCaseAndActiveFlagIgnoreCase(levelCode, ACTIVE)
                .orElseThrow(() -> new BusinessValidationException(
                        "Level code must exist and be active: " + levelCode));
    }

    private void ensureUniqueRate(Long designationId, String levelCode, LocalDate effectiveFrom, Long excludeId) {
        if (rateRepository.existsDuplicateExcludingId(designationId, levelCode, effectiveFrom, excludeId)) {
            throw new DuplicateResourceException(
                    "Rate already exists for designationId=" + designationId + ", levelCode=" + levelCode
                            + ", effectiveFrom=" + effectiveFrom);
        }
    }

    private void validateEffectiveDateRange(LocalDate effectiveFrom, LocalDate effectiveTo) {
        if (effectiveTo != null && effectiveFrom != null && effectiveTo.isBefore(effectiveFrom)) {
            throw new BusinessValidationException("effectiveTo cannot be before effectiveFrom");
        }
    }

    private String normalizeActiveFlag(String value) {
        if (value == null || value.isBlank()) {
            return ACTIVE;
        }
        return INACTIVE.equals(value.trim().toUpperCase(Locale.ROOT)) ? INACTIVE : ACTIVE;
    }
}

