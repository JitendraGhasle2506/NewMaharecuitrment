package com.maharecruitment.gov.in.master.service.impl;

import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.auth.dto.AgencyUserProvisioningRequest;
import com.maharecruitment.gov.in.auth.dto.AgencyUserProvisioningResult;
import com.maharecruitment.gov.in.auth.service.AgencyUserProvisioningService;
import com.maharecruitment.gov.in.master.dto.AgencyEscalationMatrixRequest;
import com.maharecruitment.gov.in.master.dto.AgencyMasterRequest;
import com.maharecruitment.gov.in.master.dto.AgencyMasterResponse;
import com.maharecruitment.gov.in.master.entity.AgencyEscalationMatrix;
import com.maharecruitment.gov.in.master.entity.AgencyMasterAuditAction;
import com.maharecruitment.gov.in.master.entity.AgencyMaster;
import com.maharecruitment.gov.in.master.entity.AgencyStatus;
import com.maharecruitment.gov.in.master.exception.BusinessValidationException;
import com.maharecruitment.gov.in.master.exception.DuplicateResourceException;
import com.maharecruitment.gov.in.master.exception.ResourceNotFoundException;
import com.maharecruitment.gov.in.master.mapper.AgencyMasterMapper;
import com.maharecruitment.gov.in.master.repository.AgencyMasterRepository;
import com.maharecruitment.gov.in.master.service.AgencyMasterAuditService;
import com.maharecruitment.gov.in.master.service.AgencyMasterService;
import com.maharecruitment.gov.in.master.service.AgencyTypeCatalog;
import com.maharecruitment.gov.in.master.service.CurrentActorProvider;

@Service
@Transactional(readOnly = true)
public class AgencyMasterServiceImpl implements AgencyMasterService {

    private final AgencyMasterRepository agencyRepository;
    private final AgencyMasterMapper mapper;
    private final AgencyUserProvisioningService agencyUserProvisioningService;
    private final AgencyMasterAuditService agencyMasterAuditService;
    private final AgencyTypeCatalog agencyTypeCatalog;
    private final CurrentActorProvider currentActorProvider;

    public AgencyMasterServiceImpl(
            AgencyMasterRepository agencyRepository,
            AgencyMasterMapper mapper,
            AgencyUserProvisioningService agencyUserProvisioningService,
            AgencyMasterAuditService agencyMasterAuditService,
            AgencyTypeCatalog agencyTypeCatalog,
            CurrentActorProvider currentActorProvider) {
        this.agencyRepository = agencyRepository;
        this.mapper = mapper;
        this.agencyUserProvisioningService = agencyUserProvisioningService;
        this.agencyMasterAuditService = agencyMasterAuditService;
        this.agencyTypeCatalog = agencyTypeCatalog;
        this.currentActorProvider = currentActorProvider;
    }

    @Override
    @Transactional
    public AgencyMasterResponse create(AgencyMasterRequest request) {
        validateBusinessRules(request, null);

        AgencyMaster entity = new AgencyMaster();
        applyRequest(entity, request);
        applyAuditMetadataForCreate(entity);

        AgencyMaster savedEntity = agencyRepository.save(entity);
        AgencyUserProvisioningResult provisioningResult = agencyUserProvisioningService.createOrSyncAgencyUser(
                buildProvisioningRequest(request, null));

        AgencyMasterResponse response = mapper.toResponse(savedEntity, true);
        applyProvisioningDetails(response, provisioningResult);
        agencyMasterAuditService.log(
                savedEntity.getAgencyId(),
                AgencyMasterAuditAction.CREATE,
                buildAuditDetails(savedEntity, "Agency created"));
        return response;
    }

    @Override
    @Transactional
    public AgencyMasterResponse update(Long agencyId, AgencyMasterRequest request) {
        AgencyMaster entity = agencyRepository.findDetailedByAgencyId(agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Agency not found for id: " + agencyId));

        validateBusinessRules(request, agencyId);
        String previousEmail = entity.getOfficialEmail();

        applyRequest(entity, request);
        applyAuditMetadataForUpdate(entity);

        AgencyMaster savedEntity = agencyRepository.save(entity);
        AgencyUserProvisioningResult provisioningResult = agencyUserProvisioningService.createOrSyncAgencyUser(
                buildProvisioningRequest(request, previousEmail));

        AgencyMasterResponse response = mapper.toResponse(savedEntity, true);
        applyProvisioningDetails(response, provisioningResult);
        agencyMasterAuditService.log(
                savedEntity.getAgencyId(),
                AgencyMasterAuditAction.UPDATE,
                buildAuditDetails(savedEntity, "Agency updated"));
        return response;
    }

    @Override
    @Transactional
    public AgencyMasterResponse updateStatus(Long agencyId, AgencyStatus status) {
        AgencyMaster entity = agencyRepository.findById(agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Agency not found for id: " + agencyId));
        AgencyStatus previousStatus = entity.getStatus();
        entity.setStatus(status);
        applyAuditMetadataForUpdate(entity);
        AgencyMaster savedEntity = agencyRepository.save(entity);
        agencyMasterAuditService.log(
                savedEntity.getAgencyId(),
                AgencyMasterAuditAction.STATUS_UPDATE,
                "Agency status changed from " + previousStatus + " to " + savedEntity.getStatus());
        return mapper.toResponse(savedEntity, false);
    }

    @Override
    @Transactional
    public AgencyMasterResponse delete(Long agencyId) {
        AgencyMaster entity = agencyRepository.findById(agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Agency not found for id: " + agencyId));
        AgencyStatus previousStatus = entity.getStatus();
        entity.setStatus(AgencyStatus.INACTIVE);
        applyAuditMetadataForUpdate(entity);
        AgencyMaster savedEntity = agencyRepository.save(entity);
        agencyMasterAuditService.log(
                savedEntity.getAgencyId(),
                AgencyMasterAuditAction.DELETE,
                "Agency soft-deleted. Previous status=" + previousStatus + ", current status=" + savedEntity.getStatus());
        return mapper.toResponse(savedEntity, false);
    }

    @Override
    public AgencyMasterResponse getById(Long agencyId) {
        AgencyMaster entity = agencyRepository.findDetailedByAgencyId(agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Agency not found for id: " + agencyId));
        return mapper.toResponse(entity, true);
    }

    @Override
    public Page<AgencyMasterResponse> getAll(Pageable pageable) {
        return agencyRepository.findAll(pageable).map(entity -> mapper.toResponse(entity, false));
    }

    private void validateBusinessRules(AgencyMasterRequest request, Long excludeId) {
        String agencyName = normalizeName(request.getAgencyName());
        String officialEmail = normalizeEmail(request.getOfficialEmail());
        String panNumber = normalizeUppercase(request.getPanNumber());
        String gstNumber = normalizeUppercase(request.getGstNumber());

        if (!agencyTypeCatalog.isSupported(request.getAgencyType())) {
            throw new BusinessValidationException("MSG704: Agency type must be selected from the allowed list.");
        }

        if (agencyRepository.existsByAgencyNameExcludingId(agencyName, excludeId)) {
            throw new DuplicateResourceException("MSG701: Agency name already exists in the system.");
        }
        if (agencyRepository.existsByOfficialEmailExcludingId(officialEmail, excludeId)) {
            throw new DuplicateResourceException("MSG702: Agency official email id already exists in the system.");
        }
        if (agencyRepository.existsByPanNumberExcludingId(panNumber, excludeId)) {
            throw new DuplicateResourceException("MSG708: PAN number already exists in the system.");
        }
        if (agencyRepository.existsByGstNumberExcludingId(gstNumber, excludeId)) {
            throw new DuplicateResourceException("MSG712: GST number already exists in the system.");
        }
        if (request.getEscalationMatrixEntries() == null || request.getEscalationMatrixEntries().isEmpty()) {
            throw new BusinessValidationException("MSG717: At least one escalation matrix entry is required.");
        }
        validateDocumentPath(request.getPanCopyPath(), "MSG709", "pdf", "jpg", "jpeg", "png");
        validateDocumentPath(request.getCertificateDocumentPath(), "MSG711", "pdf");
        validateDocumentPath(request.getGstDocumentPath(), "MSG713", "pdf");
        validateDocumentPath(request.getCancelledChequePath(), "MSG723", "pdf", "jpg", "jpeg");
    }

    private void applyRequest(AgencyMaster entity, AgencyMasterRequest request) {
        entity.setAgencyName(normalizeName(request.getAgencyName()));
        entity.setOfficialEmail(normalizeEmail(request.getOfficialEmail()));
        entity.setTelephoneNumber(request.getTelephoneNumber().trim());
        entity.setAgencyType(agencyTypeCatalog.resolveCanonicalType(request.getAgencyType()));
        entity.setOfficialAddress(normalizeName(request.getOfficialAddress()));
        entity.setPermanentAddress(normalizeName(request.getPermanentAddress()));
        entity.setEntityType(request.getEntityType());
        entity.setPanNumber(normalizeUppercase(request.getPanNumber()));
        entity.setPanCopyPath(normalizePath(request.getPanCopyPath()));
        entity.setCertificateNumber(normalizeName(request.getCertificateNumber()));
        entity.setCertificateDocumentPath(normalizePath(request.getCertificateDocumentPath()));
        entity.setGstNumber(normalizeUppercase(request.getGstNumber()));
        entity.setGstDocumentPath(normalizePath(request.getGstDocumentPath()));
        entity.setContactPersonName(normalizeName(request.getContactPersonName()));
        entity.setContactPersonMobileNo(request.getContactPersonMobileNo().trim());
        entity.setMsmeRegistered(request.getMsmeRegistered());
        entity.setBankName(normalizeName(request.getBankName()));
        entity.setBankBranch(normalizeName(request.getBankBranch()));
        entity.setBankAccountNumber(request.getBankAccountNumber().trim());
        entity.setBankAccountType(request.getBankAccountType());
        entity.setIfscCode(normalizeUppercase(request.getIfscCode()));
        entity.setCancelledChequePath(normalizePath(request.getCancelledChequePath()));
        if (entity.getStatus() == null) {
            entity.setStatus(AgencyStatus.ACTIVE);
        }
        entity.replaceEscalationMatrixEntries(toEscalationEntities(request.getEscalationMatrixEntries()));
    }

    private void applyAuditMetadataForCreate(AgencyMaster entity) {
        Long actorUserId = currentActorProvider.getCurrentUserId();
        if (actorUserId == null) {
            return;
        }
        entity.setCreatedUserId(actorUserId);
        entity.setUpdatedUserId(actorUserId);
    }

    private void applyAuditMetadataForUpdate(AgencyMaster entity) {
        Long actorUserId = currentActorProvider.getCurrentUserId();
        if (actorUserId == null) {
            return;
        }
        if (entity.getCreatedUserId() == null) {
            entity.setCreatedUserId(actorUserId);
        }
        entity.setUpdatedUserId(actorUserId);
    }

    private List<AgencyEscalationMatrix> toEscalationEntities(List<AgencyEscalationMatrixRequest> requests) {
        return requests.stream().map(this::toEscalationEntity).toList();
    }

    private AgencyEscalationMatrix toEscalationEntity(AgencyEscalationMatrixRequest request) {
        AgencyEscalationMatrix entity = new AgencyEscalationMatrix();
        entity.setContactName(normalizeName(request.getContactName()));
        entity.setMobileNumber(request.getMobileNumber().trim());
        entity.setLevel(normalizeUppercase(request.getLevel()));
        entity.setDesignation(normalizeName(request.getDesignation()));
        entity.setCompanyEmailId(normalizeEmail(request.getCompanyEmailId()));
        return entity;
    }

    private AgencyUserProvisioningRequest buildProvisioningRequest(AgencyMasterRequest request, String previousEmail) {
        AgencyUserProvisioningRequest provisioningRequest = new AgencyUserProvisioningRequest();
        provisioningRequest.setName(normalizeName(request.getContactPersonName()));
        provisioningRequest.setEmail(normalizeEmail(request.getOfficialEmail()));
        provisioningRequest.setMobileNo(request.getContactPersonMobileNo().trim());
        provisioningRequest.setPreviousEmail(previousEmail);
        return provisioningRequest;
    }

    private void applyProvisioningDetails(
            AgencyMasterResponse response,
            AgencyUserProvisioningResult provisioningResult) {
        response.setProvisionedUserEmail(provisioningResult.getEmail());
        response.setAgencyUserCreated(provisioningResult.isCreated());
        response.setTemporaryPassword(provisioningResult.getTemporaryPassword());
    }

    private String normalizeName(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeEmail(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeUppercase(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizePath(String value) {
        return value == null ? null : value.trim();
    }

    private void validateDocumentPath(String path, String messageCode, String... allowedExtensions) {
        String normalizedPath = normalizePath(path);
        if (normalizedPath == null || !normalizedPath.contains(".")) {
            throw new BusinessValidationException(messageCode + ": Document path must include a valid file extension.");
        }

        String extension = normalizedPath.substring(normalizedPath.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        for (String allowedExtension : allowedExtensions) {
            if (allowedExtension.equalsIgnoreCase(extension)) {
                return;
            }
        }

        throw new BusinessValidationException(
                messageCode + ": Document format is invalid. Allowed formats are " + String.join(", ", allowedExtensions) + ".");
    }

    private String buildAuditDetails(AgencyMaster agencyMaster, String action) {
        return action
                + " | agencyName=" + agencyMaster.getAgencyName()
                + ", officialEmail=" + agencyMaster.getOfficialEmail()
                + ", mobile=" + agencyMaster.getContactPersonMobileNo()
                + ", status=" + agencyMaster.getStatus();
    }
}
