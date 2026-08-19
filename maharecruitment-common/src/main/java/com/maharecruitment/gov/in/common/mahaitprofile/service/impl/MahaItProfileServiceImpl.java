package com.maharecruitment.gov.in.common.mahaitprofile.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
import com.maharecruitment.gov.in.common.security.SensitivePayloadDecryptor;
import com.maharecruitment.gov.in.common.service.CurrentActorProvider;

@Service
@Transactional(readOnly = true)
public class MahaItProfileServiceImpl implements MahaItProfileService {

    private static final String SENSITIVE_PURPOSE = "MAHAIT_PROFILE";
    private static final Pattern CIN_PATTERN = Pattern.compile(
            "^[LU][0-9]{5}[A-Z]{2}[0-9]{4}[A-Z]{3}[0-9]{6}$");
    private static final Pattern PAN_PATTERN = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]$");
    private static final Pattern GST_PATTERN = Pattern.compile(
            "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$");
    private static final Pattern ACCOUNT_NUMBER_PATTERN = Pattern.compile("^[0-9]{6,30}$");
    private static final Pattern IFSC_PATTERN = Pattern.compile("^[A-Z]{4}0[A-Z0-9]{6}$");

    private final MahaItProfileRepository profileRepository;
    private final MahaItProfileAuditLogRepository auditLogRepository;
    private final MahaItProfileAuditService auditService;
    private final CurrentActorProvider currentActorProvider;
    private final SensitivePayloadDecryptor sensitivePayloadDecryptor;

    public MahaItProfileServiceImpl(
            MahaItProfileRepository profileRepository,
            MahaItProfileAuditLogRepository auditLogRepository,
            MahaItProfileAuditService auditService,
            CurrentActorProvider currentActorProvider,
            SensitivePayloadDecryptor sensitivePayloadDecryptor) {
        this.profileRepository = profileRepository;
        this.auditLogRepository = auditLogRepository;
        this.auditService = auditService;
        this.currentActorProvider = currentActorProvider;
        this.sensitivePayloadDecryptor = sensitivePayloadDecryptor;
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
        SensitiveProfileDetails sensitiveDetails = decryptSensitiveDetails(request, false);
        applyRequest(entity, request, sensitiveDetails);
        applyAuditMetadataForCreate(entity);

        MahaItProfile saved = profileRepository.save(entity);
        auditService.log(saved.getMahaItProfileId(), MahaItProfileAuditAction.CREATE, buildCreateDetails(saved));
        return toResponse(saved);
    }

    @Override
    @Transactional
    public MahaItProfileResponse update(Long mahaitProfileId, MahaItProfileRequest request) {
        MahaItProfile entity = loadProfile(mahaitProfileId);
        SensitiveProfileDetails sensitiveDetails = decryptSensitiveDetails(request, true);
        String changeSummary = buildChangeSummary(entity, request, sensitiveDetails);

        applyRequest(entity, request, sensitiveDetails);
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

    private void applyRequest(
            MahaItProfile entity,
            MahaItProfileRequest request,
            SensitiveProfileDetails sensitiveDetails) {
        entity.setProfileName(normalizeText(request.getProfileName()));
        entity.setCompanyName(normalizeText(request.getCompanyName()));
        entity.setCompanyAddress(normalizeText(request.getCompanyAddress()));
        if (sensitiveDetails.cinNumber() != null) {
            entity.setCinNumber(sensitiveDetails.cinNumber());
        }
        if (sensitiveDetails.panNumber() != null) {
            entity.setPanNumber(sensitiveDetails.panNumber());
        }
        if (sensitiveDetails.gstNumber() != null) {
            entity.setGstNumber(sensitiveDetails.gstNumber());
        }
        entity.setBankName(normalizeText(request.getBankName()));
        entity.setBranchName(normalizeText(request.getBranchName()));
        entity.setAccountHolderName(normalizeText(request.getAccountHolderName()));
        if (sensitiveDetails.accountNumber() != null) {
            entity.setAccountNumber(sensitiveDetails.accountNumber());
        }
        if (sensitiveDetails.ifscCode() != null) {
            entity.setIfscCode(sensitiveDetails.ifscCode());
        }
        entity.setActive(request.getActive());
    }

    private SensitiveProfileDetails decryptSensitiveDetails(MahaItProfileRequest request, boolean update) {
        Map<String, String> encryptedValues = new LinkedHashMap<>();
        addEncryptedValue(encryptedValues, "cinNumber", request.getCinNumberEncrypted());
        addEncryptedValue(encryptedValues, "panNumber", request.getPanNumberEncrypted());
        addEncryptedValue(encryptedValues, "gstNumber", request.getGstNumberEncrypted());
        addEncryptedValue(encryptedValues, "accountNumber", request.getAccountNumberEncrypted());
        addEncryptedValue(encryptedValues, "ifscCode", request.getIfscCodeEncrypted());

        try {
            if (encryptedValues.isEmpty()) {
                if (!update) {
                    throw sensitiveDetailsFailure();
                }
                return SensitiveProfileDetails.unchanged();
            }
            if (request.getTimestamp() == null) {
                throw sensitiveDetailsFailure();
            }

            Map<String, String> decrypted = sensitivePayloadDecryptor.decryptSensitivePayloads(
                    encryptedValues,
                    request.getEncryptionKeyId(),
                    request.getTimestamp(),
                    request.getNonce(),
                    SENSITIVE_PURPOSE);
            SensitiveProfileDetails details = new SensitiveProfileDetails(
                    normalizeOptionalUppercase(decrypted.get("cinNumber")),
                    normalizeOptionalUppercase(decrypted.get("panNumber")),
                    normalizeOptionalUppercase(decrypted.get("gstNumber")),
                    normalizeOptional(decrypted.get("accountNumber")),
                    normalizeOptionalUppercase(decrypted.get("ifscCode")));

            if ((!update && !details.isComplete())
                    || !isValid(details.cinNumber(), CIN_PATTERN)
                    || !isValid(details.panNumber(), PAN_PATTERN)
                    || !isValid(details.gstNumber(), GST_PATTERN)
                    || !isValid(details.accountNumber(), ACCOUNT_NUMBER_PATTERN)
                    || !isValid(details.ifscCode(), IFSC_PATTERN)) {
                throw sensitiveDetailsFailure();
            }
            return details;
        } catch (RuntimeException ex) {
            throw sensitiveDetailsFailure();
        } finally {
            request.clearEncryptedSubmission();
            request.setCinNumber(null);
            request.setPanNumber(null);
            request.setGstNumber(null);
            request.setAccountNumber(null);
            request.setIfscCode(null);
        }
    }

    private void addEncryptedValue(Map<String, String> values, String fieldName, String encryptedValue) {
        if (StringUtils.hasText(encryptedValue)) {
            values.put(fieldName, encryptedValue.trim());
        }
    }

    private boolean isValid(String value, Pattern pattern) {
        return value == null || pattern.matcher(value).matches();
    }

    private String normalizeOptionalUppercase(String value) {
        String normalized = normalizeOptional(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private IllegalArgumentException sensitiveDetailsFailure() {
        return new IllegalArgumentException("Unable to process the submitted MahaIT profile identifiers.");
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
                + ", bankName=" + entity.getBankName()
                + ", sensitiveIdentifiers=provided"
                + ", active=" + entity.getActive();
    }

    private String buildChangeSummary(
            MahaItProfile existing,
            MahaItProfileRequest request,
            SensitiveProfileDetails sensitiveDetails) {
        List<String> changes = new ArrayList<>();

        appendChange(changes, "profileName", existing.getProfileName(), normalizeText(request.getProfileName()));
        appendChange(changes, "companyName", existing.getCompanyName(), normalizeText(request.getCompanyName()));
        appendChange(changes, "companyAddress", existing.getCompanyAddress(),
                normalizeText(request.getCompanyAddress()));
        appendSensitiveChange(changes, "cinNumber", existing.getCinNumber(), sensitiveDetails.cinNumber());
        appendSensitiveChange(changes, "panNumber", existing.getPanNumber(), sensitiveDetails.panNumber());
        appendSensitiveChange(changes, "gstNumber", existing.getGstNumber(), sensitiveDetails.gstNumber());
        appendChange(changes, "bankName", existing.getBankName(), normalizeText(request.getBankName()));
        appendChange(changes, "branchName", existing.getBranchName(), normalizeText(request.getBranchName()));
        appendChange(changes, "accountHolderName", existing.getAccountHolderName(),
                normalizeText(request.getAccountHolderName()));
        appendSensitiveChange(
                changes, "accountNumber", existing.getAccountNumber(), sensitiveDetails.accountNumber());
        appendSensitiveChange(changes, "ifscCode", existing.getIfscCode(), sensitiveDetails.ifscCode());
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

    private void appendSensitiveChange(
            List<String> changes,
            String fieldName,
            String previousValue,
            String submittedValue) {
        if (submittedValue != null && !Objects.equals(previousValue, submittedValue)) {
            changes.add(fieldName + ": updated");
        }
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

    private record SensitiveProfileDetails(
            String cinNumber,
            String panNumber,
            String gstNumber,
            String accountNumber,
            String ifscCode) {

        private static SensitiveProfileDetails unchanged() {
            return new SensitiveProfileDetails(null, null, null, null, null);
        }

        private boolean isComplete() {
            return cinNumber != null && panNumber != null && gstNumber != null
                    && accountNumber != null && ifscCode != null;
        }
    }
}
