package com.maharecruitment.gov.in.department.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.maharecruitment.gov.in.auth.entity.DepartmentContactEntity;
import com.maharecruitment.gov.in.auth.entity.DepartmentRegistrationEntity;
import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.DepartmentRegistrationRepository;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.auth.service.UserAffiliationService;
import com.maharecruitment.gov.in.department.dto.DepartmentProfileUpdateForm;
import com.maharecruitment.gov.in.department.exception.DepartmentApplicationException;
import com.maharecruitment.gov.in.department.service.DepartmentProfileDocumentStorageService;
import com.maharecruitment.gov.in.department.service.DepartmentProfileService;
import com.maharecruitment.gov.in.department.service.model.DepartmentProfileContactView;
import com.maharecruitment.gov.in.department.service.model.DepartmentProfileDocumentType;
import com.maharecruitment.gov.in.department.service.model.DepartmentProfileDocumentView;
import com.maharecruitment.gov.in.department.service.model.DepartmentProfileView;
import com.maharecruitment.gov.in.department.service.model.StoredDocument;
import com.maharecruitment.gov.in.master.repository.DepartmentMstRepository;
import com.maharecruitment.gov.in.master.repository.SubDepartmentRepository;

@Service
@Transactional(readOnly = true)
public class DepartmentProfileServiceImpl implements DepartmentProfileService {

    private static final Logger log = LoggerFactory.getLogger(DepartmentProfileServiceImpl.class);
    private static final String GST_DOCUMENT_MODULE_PATH = "department-registration/gst";
    private static final String PAN_DOCUMENT_MODULE_PATH = "department-registration/pan";
    private static final String TAN_DOCUMENT_MODULE_PATH = "department-registration/tan";

    private final UserRepository userRepository;
    private final UserAffiliationService userAffiliationService;
    private final DepartmentRegistrationRepository departmentRegistrationRepository;
    private final DepartmentMstRepository departmentMstRepository;
    private final SubDepartmentRepository subDepartmentRepository;
    private final DepartmentProfileDocumentStorageService documentStorageService;

    public DepartmentProfileServiceImpl(
            UserRepository userRepository,
            UserAffiliationService userAffiliationService,
            DepartmentRegistrationRepository departmentRegistrationRepository,
            DepartmentMstRepository departmentMstRepository,
            SubDepartmentRepository subDepartmentRepository,
            DepartmentProfileDocumentStorageService documentStorageService) {
        this.userRepository = userRepository;
        this.userAffiliationService = userAffiliationService;
        this.departmentRegistrationRepository = departmentRegistrationRepository;
        this.departmentMstRepository = departmentMstRepository;
        this.subDepartmentRepository = subDepartmentRepository;
        this.documentStorageService = documentStorageService;
    }

    @Override
    public DepartmentProfileView getProfile(String actorEmail) {
        User user = resolveAuthenticatedUser(actorEmail);
        DepartmentRegistrationEntity registration = resolveRegistration(user);

        String departmentName = resolveDepartmentName(registration.getDepartmentId(), registration.getDepartmentName());
        String subDepartmentName = resolveSubDepartmentName(registration.getSubDeptId());

        DepartmentProfileContactView primaryContact = mapContact(resolveContact(registration.getContacts(), true), true);
        DepartmentProfileContactView secondaryContact = mapContact(resolveContact(registration.getContacts(), false), false);

        DepartmentProfileView profileView = DepartmentProfileView.builder()
                .departmentRegistrationId(registration.getDepartmentRegistrationId())
                .departmentName(departmentName)
                .departmentMasterId(registration.getDepartmentId())
                .subDepartmentName(subDepartmentName)
                .subDepartmentMasterId(registration.getSubDeptId())
                .officeAddress(registration.getAddress())
                .billDepartmentName(registration.getBillDepartmentName())
                .gstNumber(registration.getGstNo())
                .panNumber(registration.getPanNo())
                .tanNumber(registration.getTanNo())
                .billingAddress(registration.getBillAddress())
                .gstDocumentName(extractFileName(registration.getGstFilePath()))
                .panDocumentName(extractFileName(registration.getPanFilePath()))
                .tanDocumentName(extractFileName(registration.getTanFilePath()))
                .active(Boolean.TRUE.equals(registration.getActive()))
                .termsAccepted(Boolean.TRUE.equals(registration.getIsTermsConditionAccepted()))
                .registeredOn(registration.getCreatedAt())
                .loginUserName(user.getName())
                .loginUserEmail(user.getEmail())
                .loginUserMobile(user.getMobileNo())
                .primaryContact(primaryContact)
                .secondaryContact(secondaryContact)
                .build();

        log.info(
                "Department profile loaded. actorEmail={}, registrationId={}, departmentId={}, subDepartmentId={}",
                user.getEmail(),
                registration.getDepartmentRegistrationId(),
                registration.getDepartmentId(),
                registration.getSubDeptId());

        return profileView;
    }

    @Override
    public DepartmentProfileUpdateForm getProfileForEdit(String actorEmail) {
        User user = resolveAuthenticatedUser(actorEmail);
        DepartmentRegistrationEntity registration = resolveRegistration(user);

        Optional<DepartmentContactEntity> primaryContact = resolveContact(registration.getContacts(), true);
        Optional<DepartmentContactEntity> secondaryContact = resolveContact(registration.getContacts(), false);

        DepartmentProfileUpdateForm form = new DepartmentProfileUpdateForm();
        form.setDepartmentName(resolveDepartmentName(registration.getDepartmentId(), registration.getDepartmentName()));
        form.setSubDepartmentName(resolveSubDepartmentName(registration.getSubDeptId()));
        form.setOfficeAddress(registration.getAddress());
        form.setBillDepartmentName(registration.getBillDepartmentName());
        form.setGstNumber(registration.getGstNo());
        form.setPanNumber(registration.getPanNo());
        form.setTanNumber(registration.getTanNo());
        form.setBillingAddress(registration.getBillAddress());

        form.setPrimaryContactName(primaryContact.map(DepartmentContactEntity::getContactName).orElse(user.getName()));
        form.setPrimaryDesignation(primaryContact.map(DepartmentContactEntity::getDesignation).orElse(null));
        form.setPrimaryMobileNumber(primaryContact.map(DepartmentContactEntity::getMobileNo).orElse(user.getMobileNo()));
        form.setPrimaryEmailAddress(primaryContact.map(DepartmentContactEntity::getEmail).orElse(user.getEmail()));

        form.setSecondaryContactName(secondaryContact.map(DepartmentContactEntity::getContactName).orElse(null));
        form.setSecondaryDesignation(secondaryContact.map(DepartmentContactEntity::getDesignation).orElse(null));
        form.setSecondaryMobileNumber(secondaryContact.map(DepartmentContactEntity::getMobileNo).orElse(null));
        form.setSecondaryEmailAddress(secondaryContact.map(DepartmentContactEntity::getEmail).orElse(null));

        form.setExistingGstDocumentName(extractFileName(registration.getGstFilePath()));
        form.setExistingPanDocumentName(extractFileName(registration.getPanFilePath()));
        form.setExistingTanDocumentName(extractFileName(registration.getTanFilePath()));
        return form;
    }

    @Override
    @Transactional
    public void updateProfile(String actorEmail, DepartmentProfileUpdateForm updateForm) {
        if (updateForm == null) {
            throw new DepartmentApplicationException("Profile update payload is missing.");
        }

        User user = resolveAuthenticatedUser(actorEmail);
        DepartmentRegistrationEntity registration = resolveRegistration(user);

        validateContactIndependence(
                updateForm.getPrimaryMobileNumber(),
                updateForm.getPrimaryEmailAddress(),
                updateForm.getSecondaryMobileNumber(),
                updateForm.getSecondaryEmailAddress());
        validatePrimaryEmailUpdate(user, updateForm.getPrimaryEmailAddress());

        registration.setAddress(updateForm.getOfficeAddress());
        registration.setBillDepartmentName(updateForm.getBillDepartmentName());
        registration.setGstNo(normalizeUpper(updateForm.getGstNumber()));
        registration.setPanNo(normalizeUpper(updateForm.getPanNumber()));
        registration.setTanNo(normalizeUpper(updateForm.getTanNumber()));
        registration.setBillAddress(updateForm.getBillingAddress());

        String gstPath = resolveDocumentPath(
                updateForm.getGstDocumentFile(),
                registration.getGstFilePath(),
                false,
                GST_DOCUMENT_MODULE_PATH,
                "GST");
        String panPath = resolveDocumentPath(
                updateForm.getPanDocumentFile(),
                registration.getPanFilePath(),
                true,
                PAN_DOCUMENT_MODULE_PATH,
                "PAN");
        String tanPath = resolveDocumentPath(
                updateForm.getTanDocumentFile(),
                registration.getTanFilePath(),
                false,
                TAN_DOCUMENT_MODULE_PATH,
                "TAN");

        registration.setGstFilePath(gstPath);
        registration.setPanFilePath(panPath);
        registration.setTanFilePath(tanPath);

        registration.clearContacts();
        registration.addContact(createContact(
                updateForm.getPrimaryContactName(),
                updateForm.getPrimaryDesignation(),
                updateForm.getPrimaryMobileNumber(),
                updateForm.getPrimaryEmailAddress(),
                true));
        registration.addContact(createContact(
                updateForm.getSecondaryContactName(),
                updateForm.getSecondaryDesignation(),
                updateForm.getSecondaryMobileNumber(),
                updateForm.getSecondaryEmailAddress(),
                false));

        // Keep login identity aligned with primary contact (email remains immutable).
        user.setName(updateForm.getPrimaryContactName());
        user.setMobileNo(updateForm.getPrimaryMobileNumber());
        userRepository.save(user);
        departmentRegistrationRepository.save(registration);

        log.info(
                "Department profile updated. actorEmail={}, registrationId={}, departmentId={}, subDepartmentId={}",
                user.getEmail(),
                registration.getDepartmentRegistrationId(),
                registration.getDepartmentId(),
                registration.getSubDeptId());
    }

    @Override
    public DepartmentProfileDocumentView getProfileDocument(String actorEmail, DepartmentProfileDocumentType documentType) {
        User user = resolveAuthenticatedUser(actorEmail);
        DepartmentRegistrationEntity registration = resolveRegistration(user);

        String path = resolveDocumentPathByType(registration, documentType);
        if (!documentStorageService.isManagedPath(path)) {
            throw new DepartmentApplicationException(documentType.name() + " document is unavailable.");
        }

        return DepartmentProfileDocumentView.builder()
                .originalFileName(extractFileName(path))
                .fullPath(path)
                .contentType(resolveContentType(path))
                .build();
    }

    private User resolveAuthenticatedUser(String actorEmail) {
        if (!StringUtils.hasText(actorEmail)) {
            throw new DepartmentApplicationException("Authenticated user is required.");
        }

        User user = userAffiliationService.loadUserByEmail(actorEmail);

        if (!StringUtils.hasText(user.getEmail())) {
            throw new DepartmentApplicationException("Authenticated user email is invalid.");
        }

        return user;
    }

    private DepartmentRegistrationEntity resolveRegistration(User user) {
        DepartmentRegistrationEntity registration = userAffiliationService.resolvePrimaryDepartmentRegistration(user);
        if (registration == null || registration.getDepartmentRegistrationId() == null) {
            throw new DepartmentApplicationException("Department registration profile is not linked to this user.");
        }
        return registration;
    }

    private String resolveDepartmentName(Long departmentId, String fallbackDepartmentName) {
        if (departmentId == null) {
            return fallbackDepartmentName;
        }

        return departmentMstRepository.findById(departmentId)
                .map(department -> department.getDepartmentName())
                .filter(StringUtils::hasText)
                .orElseGet(() -> {
                    log.warn("Department master entry not found for departmentId={}", departmentId);
                    return fallbackDepartmentName;
                });
    }

    private String resolveSubDepartmentName(Long subDepartmentId) {
        if (subDepartmentId == null) {
            return null;
        }

        return subDepartmentRepository.findById(subDepartmentId)
                .map(subDepartment -> subDepartment.getSubDeptName())
                .filter(StringUtils::hasText)
                .orElseGet(() -> {
                    log.warn("Sub-department master entry not found for subDepartmentId={}", subDepartmentId);
                    return null;
                });
    }

    private Optional<DepartmentContactEntity> resolveContact(
            List<DepartmentContactEntity> contacts,
            boolean primaryContact) {
        if (contacts == null || contacts.isEmpty()) {
            return Optional.empty();
        }

        return contacts.stream()
                .filter(contact -> contact != null)
                .filter(contact -> Boolean.TRUE.equals(contact.getPrimaryContact()) == primaryContact)
                .sorted(Comparator.comparing(DepartmentContactEntity::getId, Comparator.nullsLast(Long::compareTo)))
                .findFirst();
    }

    private DepartmentProfileContactView mapContact(Optional<DepartmentContactEntity> contactOptional, boolean primaryContact) {
        if (contactOptional.isEmpty()) {
            return null;
        }

        DepartmentContactEntity contact = contactOptional.get();
        return DepartmentProfileContactView.builder()
                .contactName(contact.getContactName())
                .designation(contact.getDesignation())
                .mobileNumber(contact.getMobileNo())
                .emailAddress(contact.getEmail())
                .primaryContact(primaryContact)
                .build();
    }

    private void validateContactIndependence(
            String primaryMobileNumber,
            String primaryEmailAddress,
            String secondaryMobileNumber,
            String secondaryEmailAddress) {
        if (StringUtils.hasText(primaryMobileNumber)
                && primaryMobileNumber.trim().equals(secondaryMobileNumber != null ? secondaryMobileNumber.trim() : null)) {
            throw new DepartmentApplicationException("Primary and secondary mobile numbers must be different.");
        }

        if (StringUtils.hasText(primaryEmailAddress)
                && primaryEmailAddress.trim().equalsIgnoreCase(
                        secondaryEmailAddress != null ? secondaryEmailAddress.trim() : null)) {
            throw new DepartmentApplicationException("Primary and secondary email addresses must be different.");
        }
    }

    private void validatePrimaryEmailUpdate(User user, String primaryEmailAddress) {
        String existingEmail = user.getEmail() == null ? null : user.getEmail().trim();
        String updatedEmail = primaryEmailAddress == null ? null : primaryEmailAddress.trim();

        if (existingEmail == null || updatedEmail == null) {
            throw new DepartmentApplicationException("Primary email is required.");
        }

        if (!existingEmail.equalsIgnoreCase(updatedEmail)) {
            throw new DepartmentApplicationException(
                    "Primary email cannot be changed from profile because it is linked with login username.");
        }
    }

    private DepartmentContactEntity createContact(
            String contactName,
            String designation,
            String mobileNumber,
            String emailAddress,
            boolean primaryContact) {
        DepartmentContactEntity contact = new DepartmentContactEntity();
        contact.setContactName(contactName);
        contact.setDesignation(designation);
        contact.setMobileNo(mobileNumber);
        contact.setEmail(emailAddress);
        contact.setPrimaryContact(primaryContact);
        return contact;
    }

    private String resolveDocumentPath(
            MultipartFile newDocument,
            String existingPath,
            boolean required,
            String modulePath,
            String documentLabel) {
        if (newDocument != null && !newDocument.isEmpty()) {
            StoredDocument storedDocument = documentStorageService.storeDocument(newDocument, modulePath, existingPath);
            return storedDocument.getFullPath();
        }

        if (StringUtils.hasText(existingPath)) {
            if (!documentStorageService.isManagedPath(existingPath)) {
                throw new DepartmentApplicationException(
                        documentLabel + " document path is invalid. Upload the document again.");
            }
            return existingPath;
        }

        if (required) {
            throw new DepartmentApplicationException(documentLabel + " document is required.");
        }
        return null;
    }

    private String resolveDocumentPathByType(
            DepartmentRegistrationEntity registration,
            DepartmentProfileDocumentType documentType) {
        if (documentType == null) {
            throw new DepartmentApplicationException("Document type is required.");
        }

        switch (documentType) {
            case GST:
                return registration.getGstFilePath();
            case PAN:
                return registration.getPanFilePath();
            case TAN:
                return registration.getTanFilePath();
            default:
                throw new DepartmentApplicationException("Unsupported document type: " + documentType);
        }
    }

    private String resolveContentType(String fullPath) {
        try {
            String detected = Files.probeContentType(Paths.get(fullPath).toAbsolutePath().normalize());
            if (StringUtils.hasText(detected)) {
                return detected;
            }
        } catch (IOException ex) {
            log.warn("Unable to detect content type for {}", fullPath, ex);
        }
        return "application/octet-stream";
    }

    private String normalizeUpper(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private String extractFileName(String filePath) {
        if (!StringUtils.hasText(filePath)) {
            return null;
        }

        String normalized = filePath.trim().replace("\\", "/");
        int index = normalized.lastIndexOf('/');
        if (index >= 0 && index < normalized.length() - 1) {
            return normalized.substring(index + 1);
        }
        return normalized;
    }
}
