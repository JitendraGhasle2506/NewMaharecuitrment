package com.maharecruitment.gov.in.web.service.master.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.maharecruitment.gov.in.master.dto.AgencyEscalationMatrixRequest;
import com.maharecruitment.gov.in.master.dto.AgencyMasterRequest;
import com.maharecruitment.gov.in.master.dto.AgencyMasterResponse;
import com.maharecruitment.gov.in.master.entity.AgencyStatus;
import com.maharecruitment.gov.in.master.service.AgencyMasterService;
import com.maharecruitment.gov.in.web.dto.FileUploadResult;
import com.maharecruitment.gov.in.web.dto.master.AgencyEscalationMatrixForm;
import com.maharecruitment.gov.in.web.dto.master.AgencyMasterForm;
import com.maharecruitment.gov.in.web.service.master.AgencyMasterPageService;
import com.maharecruitment.gov.in.web.service.storage.FileStorageService;
import com.maharecruitment.gov.in.web.service.verification.AccountNotificationService;

@Service
@Transactional
public class AgencyMasterPageServiceImpl implements AgencyMasterPageService {

    private final AgencyMasterService agencyMasterService;
    private final FileStorageService fileStorageService;
    private final AccountNotificationService accountNotificationService;

    public AgencyMasterPageServiceImpl(
            AgencyMasterService agencyMasterService,
            FileStorageService fileStorageService,
            AccountNotificationService accountNotificationService) {
        this.agencyMasterService = agencyMasterService;
        this.fileStorageService = fileStorageService;
        this.accountNotificationService = accountNotificationService;
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
            AgencyMasterRequest request = toRequest(form, newlyStoredFiles);
            AgencyMasterResponse response = agencyId == null
                    ? agencyMasterService.create(request)
                    : agencyMasterService.update(agencyId, request);

            if (Boolean.TRUE.equals(response.getAgencyUserCreated())
                    && StringUtils.hasText(response.getTemporaryPassword())) {
                accountNotificationService.sendAgencyCredentials(
                        response.getProvisionedUserEmail(),
                        response.getContactPersonName(),
                        response.getTemporaryPassword());
            }

            return response;
        } catch (RuntimeException ex) {
            newlyStoredFiles.forEach(fileStorageService::deleteQuietly);
            throw ex;
        }
    }

    private AgencyMasterRequest toRequest(AgencyMasterForm form, List<String> newlyStoredFiles) {
        AgencyMasterRequest request = new AgencyMasterRequest();
        request.setAgencyName(form.getAgencyName());
        request.setOfficialEmail(form.getOfficialEmail());
        request.setTelephoneNumber(form.getTelephoneNumber());
        request.setAgencyType(form.getAgencyType());
        request.setOfficialAddress(form.getOfficialAddress());
        request.setPermanentAddress(form.getPermanentAddress());
        request.setEntityType(form.getEntityType());
        request.setPanNumber(form.getPanNumber());
        request.setPanCopyPath(resolveDocumentPath(
                "agency-master/pan",
                form.getPanCopyFile(),
                form.getExistingPanCopyPath(),
                "PAN copy",
                newlyStoredFiles));
        request.setCertificateNumber(form.getCertificateNumber());
        request.setCertificateDocumentPath(resolveDocumentPath(
                "agency-master/certificate",
                form.getCertificateDocumentFile(),
                form.getExistingCertificateDocumentPath(),
                "certificate document",
                newlyStoredFiles));
        request.setGstNumber(form.getGstNumber());
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
        request.setBankAccountNumber(form.getBankAccountNumber());
        request.setBankAccountType(form.getBankAccountType());
        request.setIfscCode(form.getIfscCode());
        request.setCancelledChequePath(resolveDocumentPath(
                "agency-master/cancelled-cheque",
                form.getCancelledChequeFile(),
                form.getExistingCancelledChequePath(),
                "cancelled cheque",
                newlyStoredFiles));
        return request;
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
}
