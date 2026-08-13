package com.maharecruitment.gov.in.web.service.registration.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.auth.dto.DepartmentContactRequest;
import com.maharecruitment.gov.in.auth.dto.DepartmentRegistrationRequest;
import com.maharecruitment.gov.in.auth.dto.DepartmentUserProvisioningRequest;
import com.maharecruitment.gov.in.auth.dto.DepartmentUserProvisioningResult;
import com.maharecruitment.gov.in.auth.entity.DepartmentRegistrationEntity;
import com.maharecruitment.gov.in.auth.service.DepartmentRegistrationService;
import com.maharecruitment.gov.in.auth.service.DepartmentUserProvisioningService;
import com.maharecruitment.gov.in.master.dto.DepartmentRequest;
import com.maharecruitment.gov.in.master.dto.DepartmentResponse;
import com.maharecruitment.gov.in.master.dto.SubDepartmentRequest;
import com.maharecruitment.gov.in.master.dto.SubDepartmentResponse;
import com.maharecruitment.gov.in.master.service.DepartmentMstService;
import com.maharecruitment.gov.in.master.service.SubDepartmentService;
import com.maharecruitment.gov.in.web.dto.FileUploadResult;
import com.maharecruitment.gov.in.web.dto.registration.DepartmentRegistrationForm;
import com.maharecruitment.gov.in.web.dto.registration.DepartmentRegistrationResult;
import com.maharecruitment.gov.in.web.service.registration.DepartmentRegistrationPageService;
import com.maharecruitment.gov.in.web.service.storage.FileStorageService;
import com.maharecruitment.gov.in.web.service.security.CredentialEncryptionService;
import com.maharecruitment.gov.in.web.service.verification.AccountNotificationService;
import com.maharecruitment.gov.in.web.service.verification.OtpVerificationService;
import com.maharecruitment.gov.in.web.service.verification.VerificationPurposes;

import jakarta.servlet.http.HttpSession;

@Service
@Transactional
public class DepartmentRegistrationPageServiceImpl implements DepartmentRegistrationPageService {

    private final DepartmentMstService departmentService;
    private final SubDepartmentService subDepartmentService;
    private final DepartmentRegistrationService registrationService;
    private final DepartmentUserProvisioningService departmentUserProvisioningService;
    private final FileStorageService fileStorageService;
    private final AccountNotificationService accountNotificationService;
    private final CredentialEncryptionService credentialEncryptionService;
    private final OtpVerificationService otpVerificationService;
    private static final Pattern PAN_PATTERN = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]$");
    private static final Pattern GST_PATTERN = Pattern.compile(
            "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$");
    private static final Pattern TAN_PATTERN = Pattern.compile("^[A-Z]{4}[0-9]{5}[A-Z]$");

    public DepartmentRegistrationPageServiceImpl(
            DepartmentMstService departmentService,
            SubDepartmentService subDepartmentService,
            DepartmentRegistrationService registrationService,
            DepartmentUserProvisioningService departmentUserProvisioningService,
            FileStorageService fileStorageService,
            AccountNotificationService accountNotificationService,
            CredentialEncryptionService credentialEncryptionService,
            OtpVerificationService otpVerificationService) {
        this.departmentService = departmentService;
        this.subDepartmentService = subDepartmentService;
        this.registrationService = registrationService;
        this.departmentUserProvisioningService = departmentUserProvisioningService;
        this.fileStorageService = fileStorageService;
        this.accountNotificationService = accountNotificationService;
        this.credentialEncryptionService = credentialEncryptionService;
        this.otpVerificationService = otpVerificationService;
    }

    @Override
    public DepartmentRegistrationResult register(DepartmentRegistrationForm form, HttpSession session) {
        validateContactIndependence(form);
        PlainSensitiveIdentity sensitiveIdentity;
        try {
            sensitiveIdentity = secureSensitiveIdentity(form);
        } finally {
            form.clearEncryptedSubmission();
        }

        ResolvedDepartment resolvedDepartment = resolveDepartment(form);
        ResolvedSubDepartment resolvedSubDepartment = resolveSubDepartment(form, resolvedDepartment.departmentId());

        List<String> storedFiles = new ArrayList<>();
        try {
            String gstPath = storeDocument(
                    "department-registration/gst",
                    form.getGstFile(),
                    true,
                    storedFiles);
            String panPath = storeDocument(
                    "department-registration/pan",
                    form.getPanFile(),
                    true,
                    storedFiles);
            String tanPath = storeDocument(
                    "department-registration/tan",
                    form.getTanFile(),
                    true,
                    storedFiles);

            DepartmentRegistrationRequest request = new DepartmentRegistrationRequest();
            request.setDepartmentId(resolvedDepartment.departmentId());
            request.setSubDeptId(resolvedSubDepartment != null ? resolvedSubDepartment.subDepartmentId() : null);
            request.setDepartmentName(resolvedDepartment.departmentName());
            request.setAddress(form.getAddress());
            request.setBillDepartmentName(form.getBillDepartmentName());
            request.setGstNo(sensitiveIdentity.gst());
            request.setPanNo(sensitiveIdentity.pan());
            request.setTanNo(sensitiveIdentity.tan());
            request.setBillAddress(form.getBillAddress());
            request.setGstFilePath(gstPath);
            request.setPanFilePath(panPath);
            request.setTanFilePath(tanPath);
            request.setTermsConditionAccepted(form.getIsTermsConditionAccepted());
            request.setPrimaryContact(toContactRequest(
                    form.getPrimaryContactName(),
                    form.getPrimaryDesignation(),
                    form.getPrimaryMobile(),
                    form.getPrimaryEmail(),
                    true));
            request.setSecondaryContact(toContactRequest(
                    form.getSecondaryContactName(),
                    form.getSecondaryDesignation(),
                    form.getSecondaryMobile(),
                    form.getSecondaryEmail(),
                    false));

            DepartmentRegistrationEntity registration = registrationService.registerDepartment(request);
            DepartmentUserProvisioningResult userResult = departmentUserProvisioningService.createDepartmentUser(
                    toProvisioningRequest(form, registration));
            accountNotificationService.sendDepartmentCredentials(
                    form.getPrimaryEmail(),
                    form.getPrimaryMobile(),
                    form.getPrimaryContactName(),
                    userResult.getEmail(),
                    userResult.getTemporaryPassword());
            otpVerificationService.clear(
                    session,
                    VerificationPurposes.DEPARTMENT_REGISTRATION_PRIMARY_CONTACT);

            return new DepartmentRegistrationResult(
                    registration.getDepartmentRegistrationId(),
                    userResult.getEmail(),
                    userResult.getTemporaryPassword());
        } catch (RuntimeException ex) {
            storedFiles.forEach(fileStorageService::deleteQuietly);
            throw ex;
        }
    }

    private PlainSensitiveIdentity secureSensitiveIdentity(DepartmentRegistrationForm form) {
        if (form.getTimestamp() == null) {
            throw sensitiveIdentityFailure();
        }

        try {
            // Replay metadata is validated and consumed once for the entire form request.
            Map<String, String> decrypted = credentialEncryptionService.decryptSensitivePayloads(
                    Map.of(
                            "panNumber", form.getPanNumberEncrypted(),
                            "gstNumber", form.getGstNumberEncrypted(),
                            "tanNumber", form.getTanNumberEncrypted()),
                    form.getEncryptionKeyId(),
                    form.getTimestamp(),
                    form.getNonce(),
                    "DEPARTMENT_REGISTRATION");
            String pan = decrypted.get("panNumber");
            String gst = decrypted.get("gstNumber");
            String tan = decrypted.get("tanNumber");

            String panNormalized = pan.trim().toUpperCase(Locale.ROOT);
            String gstNormalized = gst.trim().toUpperCase(Locale.ROOT);
            String tanNormalized = tan.trim().toUpperCase(Locale.ROOT);
            if (!PAN_PATTERN.matcher(panNormalized).matches()
                    || !GST_PATTERN.matcher(gstNormalized).matches()
                    || !TAN_PATTERN.matcher(tanNormalized).matches()) {
                throw sensitiveIdentityFailure();
            }
            return new PlainSensitiveIdentity(gstNormalized, panNormalized, tanNormalized);
        } catch (RuntimeException ex) {
            throw sensitiveIdentityFailure();
        }
    }

    private IllegalArgumentException sensitiveIdentityFailure() {
        return new IllegalArgumentException("Unable to process the submitted identity information.");
    }

    private ResolvedDepartment resolveDepartment(DepartmentRegistrationForm form) {
        if (form.isOtherDepartmentSelected()) {
            DepartmentRequest request = new DepartmentRequest();
            request.setDepartmentName(form.getNewDepartmentName());
            DepartmentResponse response = departmentService.create(request);
            return new ResolvedDepartment(response.getDepartmentId(), response.getDepartmentName());
        }

        DepartmentResponse response = departmentService.getById(form.getDepartmentId());
        return new ResolvedDepartment(response.getDepartmentId(), response.getDepartmentName());
    }

    private ResolvedSubDepartment resolveSubDepartment(DepartmentRegistrationForm form, Long departmentId) {
        if (form.isOtherDepartmentSelected() || form.isOtherSubDepartmentSelected()) {
            if (StringUtils.hasText(form.getNewSubDeptName())) {
                SubDepartmentRequest request = new SubDepartmentRequest();
                request.setDepartmentId(departmentId);
                request.setSubDeptName(form.getNewSubDeptName());
                SubDepartmentResponse response = subDepartmentService.create(request);
                return new ResolvedSubDepartment(response.getSubDeptId(), response.getSubDeptName());
            }
            return null;
        }

        if (form.getSubDeptId() == null) {
            return null;
        }

        SubDepartmentResponse response = subDepartmentService.getById(form.getSubDeptId());
        if (!departmentId.equals(response.getDepartmentId())) {
            throw new IllegalArgumentException("Selected sub-department does not belong to the chosen department.");
        }
        return new ResolvedSubDepartment(response.getSubDeptId(), response.getSubDeptName());
    }

    private void validateContactIndependence(DepartmentRegistrationForm form) {
        if (form.getPrimaryMobile() != null && form.getPrimaryMobile().equals(form.getSecondaryMobile())) {
            throw new IllegalArgumentException("Primary and secondary mobile numbers must be different.");
        }
        if (form.getPrimaryEmail() != null
                && form.getPrimaryEmail().trim().equalsIgnoreCase(form.getSecondaryEmail())) {
            throw new IllegalArgumentException("Primary and secondary email addresses must be different.");
        }
    }

    private String storeDocument(
            String category,
            org.springframework.web.multipart.MultipartFile file,
            boolean required,
            List<String> storedFiles) {
        if (file != null && !file.isEmpty()) {
            FileUploadResult result = fileStorageService.store(file, category);
            storedFiles.add(result.fullPath());

            return result.fullPath();
        }

        if (required) {
            throw new IllegalArgumentException("Required document is missing.");
        }

        return null;
    }

    private DepartmentContactRequest toContactRequest(
            String contactName,
            String designation,
            String mobileNo,
            String email,
            boolean primaryContact) {
        DepartmentContactRequest request = new DepartmentContactRequest();
        request.setContactName(contactName);
        request.setDesignation(designation);
        request.setMobileNo(mobileNo);
        request.setEmail(email);
        request.setPrimaryContact(primaryContact);
        return request;
    }

    private DepartmentUserProvisioningRequest toProvisioningRequest(
            DepartmentRegistrationForm form,
            DepartmentRegistrationEntity registration) {
        DepartmentUserProvisioningRequest request = new DepartmentUserProvisioningRequest();
        request.setName(form.getPrimaryContactName());
        request.setEmail(form.getPrimaryEmail());
        request.setMobileNo(form.getPrimaryMobile());
        request.setDepartmentRegistration(registration);
        return request;
    }

    private record ResolvedDepartment(Long departmentId, String departmentName) {
    }

    private record ResolvedSubDepartment(Long subDepartmentId, String subDepartmentName) {
    }

    private record PlainSensitiveIdentity(String gst, String pan, String tan) {
    }
}
