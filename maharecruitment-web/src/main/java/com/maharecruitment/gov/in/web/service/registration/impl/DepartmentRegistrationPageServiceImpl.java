package com.maharecruitment.gov.in.web.service.registration.impl;

import java.util.ArrayList;
import java.util.List;

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
import com.maharecruitment.gov.in.web.service.verification.AccountNotificationService;

@Service
@Transactional
public class DepartmentRegistrationPageServiceImpl implements DepartmentRegistrationPageService {

    private final DepartmentMstService departmentService;
    private final SubDepartmentService subDepartmentService;
    private final DepartmentRegistrationService registrationService;
    private final DepartmentUserProvisioningService departmentUserProvisioningService;
    private final FileStorageService fileStorageService;
    private final AccountNotificationService accountNotificationService;

    public DepartmentRegistrationPageServiceImpl(
            DepartmentMstService departmentService,
            SubDepartmentService subDepartmentService,
            DepartmentRegistrationService registrationService,
            DepartmentUserProvisioningService departmentUserProvisioningService,
            FileStorageService fileStorageService,
            AccountNotificationService accountNotificationService) {
        this.departmentService = departmentService;
        this.subDepartmentService = subDepartmentService;
        this.registrationService = registrationService;
        this.departmentUserProvisioningService = departmentUserProvisioningService;
        this.fileStorageService = fileStorageService;
        this.accountNotificationService = accountNotificationService;
    }

    @Override
    public DepartmentRegistrationResult register(DepartmentRegistrationForm form) {
        validateContactIndependence(form);

        ResolvedDepartment resolvedDepartment = resolveDepartment(form);
        ResolvedSubDepartment resolvedSubDepartment = resolveSubDepartment(form, resolvedDepartment.departmentId());

        List<String> storedFiles = new ArrayList<>();
        try {
            String gstPath = storeDocument(
                    "department-registration/gst",
                    form.getGstFile(),
                    form.getUploadedGstFilePath(),
                    false,
                    storedFiles);
            String panPath = storeDocument(
                    "department-registration/pan",
                    form.getPanFile(),
                    form.getUploadedPanFilePath(),
                    true,
                    storedFiles);
            String tanPath = storeDocument(
                    "department-registration/tan",
                    form.getTanFile(),
                    form.getUploadedTanFilePath(),
                    false,
                    storedFiles);

            DepartmentRegistrationRequest request = new DepartmentRegistrationRequest();
            request.setDepartmentId(resolvedDepartment.departmentId());
            request.setSubDeptId(resolvedSubDepartment != null ? resolvedSubDepartment.subDepartmentId() : null);
            request.setDepartmentName(resolvedDepartment.departmentName());
            request.setAddress(form.getAddress());
            request.setBillDepartmentName(form.getBillDepartmentName());
            request.setGstNo(form.getGstNo());
            request.setPanNo(form.getPanNo());
            request.setTanNo(form.getTanNo());
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

            return new DepartmentRegistrationResult(
                    registration.getDepartmentRegistrationId(),
                    userResult.getEmail(),
                    userResult.getTemporaryPassword());
        } catch (RuntimeException ex) {
            storedFiles.forEach(fileStorageService::deleteQuietly);
            throw ex;
        }
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
            String existingPath,
            boolean required,
            List<String> storedFiles) {
        if (file != null && !file.isEmpty()) {
            FileUploadResult result = fileStorageService.store(file, category);
            storedFiles.add(result.fullPath());

            if (fileStorageService.isManagedPath(existingPath) && !existingPath.equals(result.fullPath())) {
                fileStorageService.deleteQuietly(existingPath);
            }

            return result.fullPath();
        }

        if (StringUtils.hasText(existingPath)) {
            if (!fileStorageService.isManagedPath(existingPath)) {
                throw new IllegalArgumentException("Invalid uploaded document reference.");
            }
            return existingPath;
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
}
