package com.maharecruitment.gov.in.common.mahaitprofile.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.common.mahaitprofile.dto.MahaItProfileAuditResponse;
import com.maharecruitment.gov.in.common.mahaitprofile.dto.MahaItProfileRequest;
import com.maharecruitment.gov.in.common.mahaitprofile.dto.MahaItProfileResponse;
import com.maharecruitment.gov.in.common.mahaitprofile.entity.MahaItProfile;
import com.maharecruitment.gov.in.common.mahaitprofile.entity.MahaItProfileAuditAction;
import com.maharecruitment.gov.in.common.mahaitprofile.entity.MahaItProfileAuditLog;
import com.maharecruitment.gov.in.common.mahaitprofile.repository.MahaItProfileAuditLogRepository;
import com.maharecruitment.gov.in.common.mahaitprofile.repository.MahaItProfileRepository;
import com.maharecruitment.gov.in.common.mahaitprofile.service.MahaItProfileAuditService;
import com.maharecruitment.gov.in.common.mahaitprofile.service.MahaItProfileService;
import com.maharecruitment.gov.in.common.service.CurrentActorProvider;

@Service
@Transactional(readOnly = true)
public class MahaItProfileServiceImpl implements MahaItProfileService {

    private final MahaItProfileRepository profileRepository;
    private final MahaItProfileAuditLogRepository auditLogRepository;
    private final MahaItProfileAuditService auditService;
    private final CurrentActorProvider currentActorProvider;

    public MahaItProfileServiceImpl(
            MahaItProfileRepository profileRepository,
            MahaItProfileAuditLogRepository auditLogRepository,
            MahaItProfileAuditService auditService,
            CurrentActorProvider currentActorProvider) {
        this.profileRepository = profileRepository;
        this.auditLogRepository = auditLogRepository;
        this.auditService = auditService;
        this.currentActorProvider = currentActorProvider;
    }

    @Override
    public Page<MahaItProfileResponse> getAll(Pageable pageable) {
        return profileRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    public MahaItProfileResponse getById(Long mahaitProfileId) {
        return toResponse(loadProfile(mahaitProfileId));
    }

    @Override
    @Transactional
    public MahaItProfileResponse create(MahaItProfileRequest request) {
        MahaItProfile entity = new MahaItProfile();
        applyRequest(entity, request);
        applyAuditMetadataForCreate(entity);

        MahaItProfile saved = profileRepository.save(entity);
        auditService.log(saved.getMahaItProfileId(), MahaItProfileAuditAction.CREATE, buildCreateDetails(saved));
        return toResponse(saved);
    }

    @Override
    @Transactional
    public MahaItProfileResponse update(Long mahaitProfileId, MahaItProfileRequest request) {
        MahaItProfile entity = loadProfile(mahaitProfileId);
        String changeSummary = buildChangeSummary(entity, request);

        applyRequest(entity, request);
        applyAuditMetadataForUpdate(entity);

        MahaItProfile saved = profileRepository.save(entity);
        auditService.log(saved.getMahaItProfileId(), MahaItProfileAuditAction.UPDATE, changeSummary);
        return toResponse(saved);
    }

    @Override
    public List<MahaItProfileAuditResponse> getAuditTrail(Long mahaitProfileId) {
        return auditLogRepository.findByMahaItProfileIdOrderByActionTimestampDesc(mahaitProfileId).stream()
                .map(this::toAuditResponse)
                .toList();
    }

    private MahaItProfile loadProfile(Long mahaitProfileId) {
        return profileRepository.findById(mahaitProfileId)
                .orElseThrow(() -> new IllegalArgumentException("MahaIT profile not found for id: " + mahaitProfileId));
    }

    private void applyRequest(MahaItProfile entity, MahaItProfileRequest request) {
        entity.setProfileName(normalizeText(request.getProfileName()));
        entity.setCompanyName(normalizeText(request.getCompanyName()));
        entity.setCompanyAddress(normalizeText(request.getCompanyAddress()));
        entity.setCinNumber(normalizeUppercase(request.getCinNumber()));
        entity.setPanNumber(normalizeUppercase(request.getPanNumber()));
        entity.setGstNumber(normalizeUppercase(request.getGstNumber()));
        entity.setBankName(normalizeText(request.getBankName()));
        entity.setBranchName(normalizeText(request.getBranchName()));
        entity.setAccountHolderName(normalizeText(request.getAccountHolderName()));
        entity.setAccountNumber(normalizeText(request.getAccountNumber()));
        entity.setIfscCode(normalizeUppercase(request.getIfscCode()));
        entity.setActive(request.getActive());
    }

    private void applyAuditMetadataForCreate(MahaItProfile entity) {
        LocalDateTime now = LocalDateTime.now();
        String actor = currentActorProvider.getCurrentActorEmail();
        entity.setCreatedBy(actor);
        entity.setCreatedDate(now);
        entity.setUpdatedBy(actor);
        entity.setUpdatedDate(now);
    }

    private void applyAuditMetadataForUpdate(MahaItProfile entity) {
        LocalDateTime now = LocalDateTime.now();
        String actor = currentActorProvider.getCurrentActorEmail();
        if (entity.getCreatedBy() == null || entity.getCreatedBy().isBlank()) {
            entity.setCreatedBy(actor);
        }
        if (entity.getCreatedDate() == null) {
            entity.setCreatedDate(now);
        }
        entity.setUpdatedBy(actor);
        entity.setUpdatedDate(now);
    }

    private String buildCreateDetails(MahaItProfile entity) {
        return "MahaIT profile created | profileName=" + entity.getProfileName()
                + ", companyName=" + entity.getCompanyName()
                + ", cinNumber=" + entity.getCinNumber()
                + ", gstNumber=" + entity.getGstNumber()
                + ", bankName=" + entity.getBankName()
                + ", active=" + entity.getActive();
    }

    private String buildChangeSummary(MahaItProfile existing, MahaItProfileRequest request) {
        List<String> changes = new ArrayList<>();

        appendChange(changes, "profileName", existing.getProfileName(), normalizeText(request.getProfileName()));
        appendChange(changes, "companyName", existing.getCompanyName(), normalizeText(request.getCompanyName()));
        appendChange(changes, "companyAddress", existing.getCompanyAddress(),
                normalizeText(request.getCompanyAddress()));
        appendChange(changes, "cinNumber", existing.getCinNumber(), normalizeUppercase(request.getCinNumber()));
        appendChange(changes, "panNumber", existing.getPanNumber(), normalizeUppercase(request.getPanNumber()));
        appendChange(changes, "gstNumber", existing.getGstNumber(), normalizeUppercase(request.getGstNumber()));
        appendChange(changes, "bankName", existing.getBankName(), normalizeText(request.getBankName()));
        appendChange(changes, "branchName", existing.getBranchName(), normalizeText(request.getBranchName()));
        appendChange(changes, "accountHolderName", existing.getAccountHolderName(),
                normalizeText(request.getAccountHolderName()));
        appendChange(changes, "accountNumber", existing.getAccountNumber(), normalizeText(request.getAccountNumber()));
        appendChange(changes, "ifscCode", existing.getIfscCode(), normalizeUppercase(request.getIfscCode()));
        appendChange(changes, "active", existing.getActive(), request.getActive());

        if (changes.isEmpty()) {
            return "MahaIT profile updated | no field changes detected";
        }
        return "MahaIT profile updated | " + String.join("; ", changes);
    }

    private void appendChange(List<String> changes, String fieldName, Object previousValue, Object newValue) {
        if (Objects.equals(previousValue, newValue)) {
            return;
        }
        changes.add(fieldName + ": '" + valueOrDash(previousValue) + "' -> '" + valueOrDash(newValue) + "'");
    }

    private String valueOrDash(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

    private MahaItProfileResponse toResponse(MahaItProfile entity) {
        MahaItProfileResponse response = new MahaItProfileResponse();
        response.setMahaItProfileId(entity.getMahaItProfileId());
        response.setProfileName(entity.getProfileName());
        response.setCompanyName(entity.getCompanyName());
        response.setCompanyAddress(entity.getCompanyAddress());
        response.setCinNumber(entity.getCinNumber());
        response.setPanNumber(entity.getPanNumber());
        response.setGstNumber(entity.getGstNumber());
        response.setBankName(entity.getBankName());
        response.setBranchName(entity.getBranchName());
        response.setAccountHolderName(entity.getAccountHolderName());
        response.setAccountNumber(entity.getAccountNumber());
        response.setIfscCode(entity.getIfscCode());
        response.setActive(entity.getActive());
        response.setCreatedBy(entity.getCreatedBy());
        response.setCreatedDate(entity.getCreatedDate());
        response.setUpdatedBy(entity.getUpdatedBy());
        response.setUpdatedDate(entity.getUpdatedDate());
        return response;
    }

    private MahaItProfileAuditResponse toAuditResponse(MahaItProfileAuditLog auditLog) {
        MahaItProfileAuditResponse response = new MahaItProfileAuditResponse();
        response.setAuditId(auditLog.getAuditId());
        response.setMahaItProfileId(auditLog.getMahaItProfileId());
        response.setActionType(auditLog.getActionType());
        response.setActorUserId(auditLog.getActorUserId());
        response.setActorUsername(auditLog.getActorUsername());
        response.setActionTimestamp(auditLog.getActionTimestamp());
        response.setDetails(auditLog.getDetails());
        return response;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeUppercase(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized.toUpperCase(Locale.ROOT);
    }
}
