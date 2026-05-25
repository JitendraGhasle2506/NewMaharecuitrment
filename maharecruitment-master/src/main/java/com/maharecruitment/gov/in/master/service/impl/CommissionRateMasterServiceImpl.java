package com.maharecruitment.gov.in.master.service.impl;

import java.time.LocalDate;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.master.dto.CommissionRateAuditLogResponse;
import com.maharecruitment.gov.in.master.dto.CommissionRateRequest;
import com.maharecruitment.gov.in.master.dto.CommissionRateResponse;
import com.maharecruitment.gov.in.master.entity.CommissionCode;
import com.maharecruitment.gov.in.master.entity.CommissionRateAuditAction;
import com.maharecruitment.gov.in.master.entity.CommissionRateMaster;
import com.maharecruitment.gov.in.master.exception.BusinessValidationException;
import com.maharecruitment.gov.in.master.exception.DuplicateResourceException;
import com.maharecruitment.gov.in.master.exception.ResourceNotFoundException;
import com.maharecruitment.gov.in.master.mapper.CommissionRateMapper;
import com.maharecruitment.gov.in.master.repository.CommissionRateAuditLogRepository;
import com.maharecruitment.gov.in.master.repository.CommissionRateMasterRepository;
import com.maharecruitment.gov.in.master.service.CommissionRateAuditService;
import com.maharecruitment.gov.in.master.service.CommissionRateMasterService;

@Service
@Transactional(readOnly = true)
public class CommissionRateMasterServiceImpl implements CommissionRateMasterService {

    private static final String ACTIVE = "Y";
    private static final String INACTIVE = "N";

    private final CommissionRateMasterRepository commissionRateRepository;
    private final CommissionRateAuditLogRepository auditLogRepository;
    private final CommissionRateAuditService auditService;
    private final CommissionRateMapper mapper;

    public CommissionRateMasterServiceImpl(
            CommissionRateMasterRepository commissionRateRepository,
            CommissionRateAuditLogRepository auditLogRepository,
            CommissionRateAuditService auditService,
            CommissionRateMapper mapper) {
        this.commissionRateRepository = commissionRateRepository;
        this.auditLogRepository = auditLogRepository;
        this.auditService = auditService;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public CommissionRateResponse create(CommissionRateRequest request) {
        validateRequest(request);
        ensureUniqueRate(request.getCommissionCode(), request.getEffectiveDate(), null);

        CommissionRateMaster entity = CommissionRateMaster.builder()
                .commissionCode(request.getCommissionCode())
                .commissionPercentage(request.getCommissionPercentage())
                .effectiveDate(request.getEffectiveDate())
                .activeFlag(normalizeActiveFlag(request.getActiveFlag()))
                .build();

        CommissionRateMaster saved = commissionRateRepository.save(entity);
        auditService.log(saved.getCommissionRateId(), CommissionRateAuditAction.CREATED, details(saved));
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public CommissionRateResponse update(Long commissionRateId, CommissionRateRequest request) {
        CommissionRateMaster entity = findRequired(commissionRateId);
        validateRequest(request);
        ensureUniqueRate(request.getCommissionCode(), request.getEffectiveDate(), commissionRateId);

        String before = details(entity);
        entity.setCommissionCode(request.getCommissionCode());
        entity.setCommissionPercentage(request.getCommissionPercentage());
        entity.setEffectiveDate(request.getEffectiveDate());
        entity.setActiveFlag(normalizeActiveFlag(request.getActiveFlag()));

        CommissionRateMaster saved = commissionRateRepository.save(entity);
        auditService.log(saved.getCommissionRateId(), CommissionRateAuditAction.UPDATED,
                "Before: " + before + " | After: " + details(saved));
        return mapper.toResponse(saved);
    }

    @Override
    public CommissionRateResponse getById(Long commissionRateId, boolean includeInactive) {
        CommissionRateMaster entity = includeInactive
                ? findRequired(commissionRateId)
                : commissionRateRepository
                        .findByCommissionRateIdAndActiveFlagIgnoreCase(commissionRateId, ACTIVE)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Active commission rate not found for id: " + commissionRateId));
        return mapper.toResponse(entity);
    }

    @Override
    public Page<CommissionRateResponse> getAll(
            CommissionCode commissionCode,
            boolean includeInactive,
            Pageable pageable) {
        Page<CommissionRateMaster> page;
        if (commissionCode == null) {
            page = includeInactive
                    ? commissionRateRepository.findAll(pageable)
                    : commissionRateRepository.findByActiveFlagIgnoreCase(ACTIVE, pageable);
        } else {
            page = includeInactive
                    ? commissionRateRepository.findByCommissionCode(commissionCode, pageable)
                    : commissionRateRepository.findByCommissionCodeAndActiveFlagIgnoreCase(
                            commissionCode,
                            ACTIVE,
                            pageable);
        }
        return page.map(mapper::toResponse);
    }

    @Override
    public CommissionRateResponse getApplicableRate(CommissionCode commissionCode, LocalDate effectiveDate) {
        if (commissionCode == null) {
            throw new BusinessValidationException("Commission code is required");
        }
        LocalDate lookupDate = effectiveDate == null ? LocalDate.now() : effectiveDate;
        return commissionRateRepository.findApplicableActiveRates(commissionCode, lookupDate)
                .stream()
                .findFirst()
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No active commission rate found for " + commissionCode + " on " + lookupDate));
    }

    @Override
    public Page<CommissionRateAuditLogResponse> getAuditLogs(Long commissionRateId, Pageable pageable) {
        if (!commissionRateRepository.existsById(commissionRateId)) {
            throw new ResourceNotFoundException("Commission rate not found for id: " + commissionRateId);
        }
        return auditLogRepository.findByCommissionRateIdOrderByActionTimestampDesc(commissionRateId, pageable)
                .map(mapper::toAuditResponse);
    }

    @Override
    @Transactional
    public void softDelete(Long commissionRateId) {
        CommissionRateMaster entity = findRequired(commissionRateId);
        entity.setActiveFlag(INACTIVE);
        commissionRateRepository.save(entity);
        auditService.log(commissionRateId, CommissionRateAuditAction.DEACTIVATED, details(entity));
    }

    @Override
    @Transactional
    public void restore(Long commissionRateId) {
        CommissionRateMaster entity = findRequired(commissionRateId);
        ensureUniqueRate(entity.getCommissionCode(), entity.getEffectiveDate(), commissionRateId);
        entity.setActiveFlag(ACTIVE);
        commissionRateRepository.save(entity);
        auditService.log(commissionRateId, CommissionRateAuditAction.RESTORED, details(entity));
    }

    private CommissionRateMaster findRequired(Long commissionRateId) {
        return commissionRateRepository.findById(commissionRateId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Commission rate not found for id: " + commissionRateId));
    }

    private void validateRequest(CommissionRateRequest request) {
        if (request.getCommissionCode() == null) {
            throw new BusinessValidationException("Commission code is required");
        }
        if (request.getCommissionPercentage() == null) {
            throw new BusinessValidationException("Commission percentage is required");
        }
        if (request.getEffectiveDate() == null) {
            throw new BusinessValidationException("Effective date is required");
        }
    }

    private void ensureUniqueRate(CommissionCode commissionCode, LocalDate effectiveDate, Long excludeId) {
        boolean exists = excludeId == null
                ? commissionRateRepository.existsByCommissionCodeAndEffectiveDate(commissionCode, effectiveDate)
                : commissionRateRepository.existsByCommissionCodeAndEffectiveDateAndCommissionRateIdNot(
                        commissionCode,
                        effectiveDate,
                        excludeId);
        if (exists) {
            throw new DuplicateResourceException(
                    "Commission rate already exists for " + commissionCode + " on " + effectiveDate);
        }
    }

    private String normalizeActiveFlag(String value) {
        if (value == null || value.isBlank()) {
            return ACTIVE;
        }
        return INACTIVE.equals(value.trim().toUpperCase(Locale.ROOT)) ? INACTIVE : ACTIVE;
    }

    private String details(CommissionRateMaster entity) {
        return "code=" + entity.getCommissionCode()
                + ", percentage=" + entity.getCommissionPercentage()
                + ", effectiveDate=" + entity.getEffectiveDate()
                + ", activeFlag=" + entity.getActiveFlag();
    }
}
