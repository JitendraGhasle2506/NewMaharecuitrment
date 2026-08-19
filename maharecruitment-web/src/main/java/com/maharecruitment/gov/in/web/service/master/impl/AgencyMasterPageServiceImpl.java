package com.maharecruitment.gov.in.web.service.master.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.maharecruitment.gov.in.master.dto.AgencyEscalationMatrixRequest;
import com.maharecruitment.gov.in.master.dto.AgencyEscalationMatrixResponse;
import com.maharecruitment.gov.in.master.dto.AgencyMasterRequest;
import com.maharecruitment.gov.in.master.dto.AgencyMasterResponse;
import com.maharecruitment.gov.in.master.entity.AgencyMaster;
import com.maharecruitment.gov.in.master.entity.AgencyStatus;
import com.maharecruitment.gov.in.master.repository.AgencyMasterRepository;
import com.maharecruitment.gov.in.master.service.AgencyMasterService;
import com.maharecruitment.gov.in.web.dto.FileUploadResult;
import com.maharecruitment.gov.in.web.dto.master.AgencyEscalationMatrixForm;
import com.maharecruitment.gov.in.web.dto.master.AgencyMasterForm;
import com.maharecruitment.gov.in.web.service.agency.AgencyAccessService;
import com.maharecruitment.gov.in.web.service.agency.AgencyUserContext;
import com.maharecruitment.gov.in.web.service.master.AgencyMasterPageService;
import com.maharecruitment.gov.in.web.service.security.CredentialEncryptionService;
import com.maharecruitment.gov.in.web.service.storage.FileStorageService;
import com.maharecruitment.gov.in.web.service.verification.AccountNotificationService;

@Service
@Transactional
public class AgencyMasterPageServiceImpl implements AgencyMasterPageService {

    private static final String SENSITIVE_PURPOSE = "AGENCY_MASTER";
    private static final Pattern PAN_PATTERN = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]$");
    private static final Pattern GST_PATTERN = Pattern.compile(
            "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$");
    private static final Pattern BANK_ACCOUNT_PATTERN = Pattern.compile("^[0-9]{9,30}$");
    private static final Pattern IFSC_PATTERN = Pattern.compile("^[A-Z]{4}0[A-Z0-9]{6}$");
	
	@Autowired
	AgencyMasterRepository agencyMasterRepository;

    private final AgencyMasterService agencyMasterService;
    private final FileStorageService fileStorageService;
    private final AccountNotificationService accountNotificationService;
    private final AgencyAccessService agencyAccessService;
    private final CredentialEncryptionService credentialEncryptionService;

    public AgencyMasterPageServiceImpl(
            AgencyMasterService agencyMasterService,
            FileStorageService fileStorageService,
            AccountNotificationService accountNotificationService,
            AgencyAccessService agencyAccessService,
            CredentialEncryptionService credentialEncryptionService) {
        this.agencyMasterService = agencyMasterService;
        this.fileStorageService = fileStorageService;
        this.accountNotificationService = accountNotificationService;
        this.agencyAccessService = agencyAccessService;
        this.credentialEncryptionService = credentialEncryptionService;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AgencyMasterResponse> getAll(Pageable pageable) {
        return agencyMasterService.getAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public AgencyMasterResponse getById(Long agencyId) {
        return agencyMasterService.getById(agencyId);
    }

    @Override
    public AgencyMasterResponse create(AgencyMasterForm form) {
        return save(null, form);
    }

    @Override
    public AgencyMasterResponse update(Long agencyId, AgencyMasterForm form) {
        return save(agencyId, form);
    }

    @Override
    public AgencyMasterResponse updateStatus(Long agencyId, AgencyStatus status) {
        return agencyMasterService.updateStatus(agencyId, status);
    }

    private AgencyMasterResponse save(Long agencyId, AgencyMasterForm form) {
        List<String> newlyStoredFiles = new ArrayList<>();
        try {
            SensitiveAgencyIdentity sensitiveIdentity = secureSensitiveIdentity(form, agencyId != null);
            AgencyMasterRequest request = toRequest(form, sensitiveIdentity, newlyStoredFiles);
            AgencyMasterResponse response = agencyId == null
                    ? agencyMasterService.create(request)
                    : agencyMasterService.update(agencyId, request);

            if (Boolean.TRUE.equals(response.getAgencyUserCreated())
                    && StringUtils.hasText(response.getTemporaryPassword())) {
                accountNotificationService.sendAgencyCredentials(
                        response.getProvisionedUserEmail(),
                        response.getContactPersonMobileNo(),
                        response.getContactPersonName(),
                        response.getProvisionedUserEmail(),
                        response.getTemporaryPassword());
                response.setTemporaryPassword(null);
            }

            return response;
        } catch (RuntimeException ex) {
            newlyStoredFiles.forEach(fileStorageService::deleteQuietly);
            throw ex;
        }
    }

    private AgencyMasterRequest toRequest(
            AgencyMasterForm form,
            SensitiveAgencyIdentity sensitiveIdentity,
            List<String> newlyStoredFiles) {
        AgencyMasterRequest request = new AgencyMasterRequest();
        request.setAgencyName(form.getAgencyName());
        request.setOfficialEmail(form.getOfficialEmail());
        request.setTelephoneNumber(form.getTelephoneNumber());
        request.setAgencyType(form.getAgencyType());
        request.setOfficialAddress(form.getOfficialAddress());
        request.setPermanentAddress(form.getPermanentAddress());
        request.setEntityType(form.getEntityType());
        request.setPanNumber(sensitiveIdentity.panNumber());
        request.setPanCopyPath(resolveDocumentPath(
                "agency-master/pan",
                form.getPanCopyFile(),
                form.getExistingPanCopyPath(),
                "PAN copy",
                newlyStoredFiles));
        request.setCertificateNumber(sensitiveIdentity.certificateNumber());
        request.setCertificateDocumentPath(resolveDocumentPath(
                "agency-master/certificate",
                form.getCertificateDocumentFile(),
                form.getExistingCertificateDocumentPath(),
                "certificate document",
                newlyStoredFiles));
        request.setGstNumber(sensitiveIdentity.gstNumber());
        request.setGstDocumentPath(resolveDocumentPath(
                "agency-master/gst",
                form.getGstDocumentFile(),
                form.getExistingGstDocumentPath(),
                "GST document",
                newlyStoredFiles));
        request.setContactPersonName(form.getContactPersonName());
        request.setContactPersonMobileNo(form.getContactPersonMobileNo());
        request.setMsmeRegistered(form.getMsmeRegistered());
        request.setEscalationMatrixEntries(form.getEscalationMatrixEntries().stream()
                .map(this::toEscalationRequest)
                .toList());
        request.setBankName(form.getBankName());
        request.setBankBranch(form.getBankBranch());
        request.setBankAccountNumber(sensitiveIdentity.bankAccountNumber());
        request.setBankAccountType(form.getBankAccountType());
        request.setIfscCode(sensitiveIdentity.ifscCode());
        request.setCancelledChequePath(resolveDocumentPath(
                "agency-master/cancelled-cheque",
                form.getCancelledChequeFile(),
                form.getExistingCancelledChequePath(),
                "cancelled cheque",
                newlyStoredFiles));
        return request;
    }

    private SensitiveAgencyIdentity secureSensitiveIdentity(
            AgencyMasterForm form,
            boolean update) {
        Map<String, String> encryptedValues = new LinkedHashMap<>();
        addEncryptedValue(encryptedValues, "panNumber", form.getPanNumberEncrypted());
        addEncryptedValue(encryptedValues, "gstNumber", form.getGstNumberEncrypted());
        addEncryptedValue(encryptedValues, "bankAccountNumber", form.getBankAccountNumberEncrypted());
        addEncryptedValue(encryptedValues, "ifscCode", form.getIfscCodeEncrypted());
        addEncryptedValue(encryptedValues, "certificateNumber", form.getCertificateNumberEncrypted());

        try {
            if (encryptedValues.isEmpty()) {
                if (!update) {
                    throw sensitiveIdentityFailure();
                }
                return SensitiveAgencyIdentity.unchanged();
            }

            Map<String, String> decrypted = Map.of();
            if (form.getTimestamp() == null) {
                throw sensitiveIdentityFailure();
            }
            decrypted = credentialEncryptionService.decryptSensitivePayloads(
                    encryptedValues,
                    form.getEncryptionKeyId(),
                    form.getTimestamp(),
                    form.getNonce(),
                    SENSITIVE_PURPOSE);

            String panNumber = normalizeOptionalUppercase(decrypted.get("panNumber"));
            String gstNumber = normalizeOptionalUppercase(decrypted.get("gstNumber"));
            String bankAccountNumber = normalizeOptional(decrypted.get("bankAccountNumber"));
            String ifscCode = normalizeOptionalUppercase(decrypted.get("ifscCode"));
            String certificateNumber = normalizeOptional(decrypted.get("certificateNumber"));

            if ((!update && (panNumber == null || gstNumber == null
                    || bankAccountNumber == null || ifscCode == null || certificateNumber == null))
                    || (panNumber != null && !PAN_PATTERN.matcher(panNumber).matches())
                    || (gstNumber != null && !GST_PATTERN.matcher(gstNumber).matches())
                    || (bankAccountNumber != null && !BANK_ACCOUNT_PATTERN.matcher(bankAccountNumber).matches())
                    || (ifscCode != null && !IFSC_PATTERN.matcher(ifscCode).matches())
                    || (certificateNumber != null && certificateNumber.length() > 100)) {
                throw sensitiveIdentityFailure();
            }
            return new SensitiveAgencyIdentity(
                    panNumber,
                    gstNumber,
                    bankAccountNumber,
                    ifscCode,
                    certificateNumber);
        } catch (RuntimeException ex) {
            throw sensitiveIdentityFailure();
        } finally {
            form.clearEncryptedSubmission();
            form.setPanNumber(null);
            form.setGstNumber(null);
            form.setBankAccountNumber(null);
            form.setIfscCode(null);
            form.setCertificateNumber(null);
        }
    }

    private void addEncryptedValue(Map<String, String> values, String fieldName, String encryptedValue) {
        if (StringUtils.hasText(encryptedValue)) {
            values.put(fieldName, encryptedValue.trim());
        }
    }

    private String normalizeOptionalUppercase(String value) {
        String normalized = normalizeOptional(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private IllegalArgumentException sensitiveIdentityFailure() {
        return new IllegalArgumentException("Unable to process the submitted agency identity information.");
    }

    private AgencyEscalationMatrixRequest toEscalationRequest(AgencyEscalationMatrixForm form) {
        AgencyEscalationMatrixRequest request = new AgencyEscalationMatrixRequest();
        request.setContactName(form.getContactName());
        request.setMobileNumber(form.getMobileNumber());
        request.setLevel(form.getLevel());
        request.setDesignation(form.getDesignation());
        request.setCompanyEmailId(form.getCompanyEmailId());
        return request;
    }

    private String resolveDocumentPath(
            String module,
            MultipartFile file,
            String existingPath,
            String documentLabel,
            List<String> newlyStoredFiles) {
        if (file != null && !file.isEmpty()) {
            FileUploadResult uploadResult = fileStorageService.store(file, module);
            newlyStoredFiles.add(uploadResult.fullPath());
            return uploadResult.fullPath();
        }

        if (StringUtils.hasText(existingPath)) {
            return existingPath.trim();
        }

        throw new IllegalArgumentException(documentLabel + " is required.");
    }

    public AgencyMasterResponse getAgencyProfile(String email) {
        AgencyUserContext context = agencyAccessService.requireActiveAgencyContext(email);
        AgencyMaster agency = agencyMasterRepository.findDetailedByAgencyId(context.agencyId())
                .orElseThrow(() -> new IllegalArgumentException("No agency profile is linked with this login user."));

        AgencyMasterResponse response = new AgencyMasterResponse();

        BeanUtils.copyProperties(agency, response);
        if (agency.getEscalationMatrixEntries() != null) {

            List<AgencyEscalationMatrixResponse> escalationList =
                    agency.getEscalationMatrixEntries()
                            .stream()
                            .map(e -> {
                                AgencyEscalationMatrixResponse r =
                                        new AgencyEscalationMatrixResponse();

                                r.setContactName(e.getContactName());
                                r.setMobileNumber(e.getMobileNumber());
                                r.setLevel(e.getLevel());
                                r.setDesignation(e.getDesignation());
                                r.setCompanyEmailId(e.getCompanyEmailId());

                                return r;
                            })
                            .toList();

            response.setEscalationMatrixEntries(escalationList);
        }

        return response;
    }

    private record SensitiveAgencyIdentity(
            String panNumber,
            String gstNumber,
            String bankAccountNumber,
            String ifscCode,
            String certificateNumber) {

        private static SensitiveAgencyIdentity unchanged() {
            return new SensitiveAgencyIdentity(null, null, null, null, null);
        }
    }
}
