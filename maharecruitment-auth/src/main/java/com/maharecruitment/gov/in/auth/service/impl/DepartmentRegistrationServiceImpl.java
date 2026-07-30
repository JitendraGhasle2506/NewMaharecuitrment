package com.maharecruitment.gov.in.auth.service.impl;

import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.auth.dto.DepartmentContactRequest;
import com.maharecruitment.gov.in.auth.dto.DepartmentRegistrationRequest;
import com.maharecruitment.gov.in.auth.entity.DepartmentContactEntity;
import com.maharecruitment.gov.in.auth.entity.DepartmentRegistrationEntity;
import com.maharecruitment.gov.in.auth.repository.DepartmentRegistrationRepository;
import com.maharecruitment.gov.in.auth.service.DepartmentRegistrationService;

@Service
@Transactional
public class DepartmentRegistrationServiceImpl implements DepartmentRegistrationService {

    private static final Pattern PAN_PATTERN = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]$");
    private static final Pattern GST_PATTERN = Pattern.compile(
            "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$");

    private final DepartmentRegistrationRepository departmentRegistrationRepository;

    public DepartmentRegistrationServiceImpl(DepartmentRegistrationRepository departmentRegistrationRepository) {
        this.departmentRegistrationRepository = departmentRegistrationRepository;
    }

    @Override
    public DepartmentRegistrationEntity registerDepartment(DepartmentRegistrationRequest request) {
        validateSensitiveIdentity(request);
        validateUniqueRegistration(request);

        DepartmentRegistrationEntity entity = new DepartmentRegistrationEntity();
        entity.setDepartmentId(request.getDepartmentId());
        entity.setSubDeptId(request.getSubDeptId());
        entity.setDepartmentName(normalizeText(request.getDepartmentName()));
        entity.setAddress(normalizeText(request.getAddress()));
        entity.setBillDepartmentName(normalizeText(request.getBillDepartmentName()));
        entity.setGstNo(normalizeUpper(request.getGstNo()));
        entity.setPanNo(normalizeUpper(request.getPanNo()));
        entity.setTanNo(normalizeUpper(request.getTanNo()));
        entity.setBillAddress(normalizeText(request.getBillAddress()));
        entity.setGstFilePath(request.getGstFilePath());
        entity.setPanFilePath(request.getPanFilePath());
        entity.setTanFilePath(request.getTanFilePath());
        entity.setIsTermsConditionAccepted(Boolean.TRUE.equals(request.getTermsConditionAccepted()));
        entity.setActive(Boolean.FALSE);

        entity.clearContacts();
        entity.addContact(toContactEntity(request.getPrimaryContact(), true));
        entity.addContact(toContactEntity(request.getSecondaryContact(), false));

        return departmentRegistrationRepository.save(entity);
    }

    private void validateSensitiveIdentity(DepartmentRegistrationRequest request) {
        String pan = request == null ? null : normalizeUpper(request.getPanNo());
        String gst = request == null ? null : normalizeUpper(request.getGstNo());
        if (pan == null || gst == null
                || !PAN_PATTERN.matcher(pan).matches()
                || !GST_PATTERN.matcher(gst).matches()) {
            throw new IllegalArgumentException("Unable to process the submitted identity information.");
        }
    }

    private void validateUniqueRegistration(DepartmentRegistrationRequest request) {
        if (request.getDepartmentId() != null
                && departmentRegistrationRepository.existsByDepartmentIdAndSubDeptId(
                        request.getDepartmentId(), request.getSubDeptId())) {
            throw new IllegalArgumentException(
                    "This department/sub-department combination is already registered.");
        }
        if (departmentRegistrationRepository.existsByGstNoIgnoreCase(normalizeUpper(request.getGstNo()))) {
            throw new IllegalArgumentException("A registration already exists for the provided GST number.");
        }
        if (departmentRegistrationRepository.existsByPanNoIgnoreCase(normalizeUpper(request.getPanNo()))) {
            throw new IllegalArgumentException("A registration already exists for the provided PAN number.");
        }
        if (departmentRegistrationRepository.existsByTanNoIgnoreCase(normalizeUpper(request.getTanNo()))) {
            throw new IllegalArgumentException("A registration already exists for the provided TAN number.");
        }
    }

    private DepartmentContactEntity toContactEntity(DepartmentContactRequest request, boolean primaryContact) {
        DepartmentContactEntity contact = new DepartmentContactEntity();
        contact.setContactName(normalizeText(request.getContactName()));
        contact.setDesignation(normalizeText(request.getDesignation()));
        contact.setMobileNo(request.getMobileNo().trim());
        contact.setEmail(request.getEmail().trim().toLowerCase(Locale.ROOT));
        contact.setPrimaryContact(primaryContact);
        return contact;
    }

    private String normalizeText(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeUpper(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }
}
