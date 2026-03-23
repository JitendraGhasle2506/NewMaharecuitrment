package com.maharecruitment.gov.in.department.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.auth.entity.DepartmentRegistrationEntity;
import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.department.dto.DepartmentProjectApplicationActivityView;
import com.maharecruitment.gov.in.department.dto.DepartmentProjectApplicationForm;
import com.maharecruitment.gov.in.department.dto.DepartmentProjectApplicationSummaryView;
import com.maharecruitment.gov.in.department.dto.DepartmentProjectResourceRequirementForm;
import com.maharecruitment.gov.in.department.dto.LevelOptionView;
import com.maharecruitment.gov.in.department.entity.DepartmentApplicationActivityType;
import com.maharecruitment.gov.in.department.entity.DepartmentApplicationType;
import com.maharecruitment.gov.in.department.entity.DepartmentApplicationStatus;
import com.maharecruitment.gov.in.department.entity.DepartmentProjectApplicationActivityEntity;
import com.maharecruitment.gov.in.department.entity.DepartmentProjectApplicationEntity;
import com.maharecruitment.gov.in.department.entity.DepartmentProjectResourceRequirementEntity;
import com.maharecruitment.gov.in.department.entity.DepartmentProformaInvoiceEntity;
import com.maharecruitment.gov.in.department.entity.DepartmentTaxRateMasterEntity;
import com.maharecruitment.gov.in.department.entity.AuditorReviewDecision;
import com.maharecruitment.gov.in.department.entity.HrReviewDecision;
import com.maharecruitment.gov.in.department.exception.DepartmentApplicationException;
import com.maharecruitment.gov.in.department.repository.DepartmentProjectApplicationActivityRepository;
import com.maharecruitment.gov.in.department.repository.DepartmentProjectApplicationRepository;
import com.maharecruitment.gov.in.department.repository.DepartmentProformaInvoiceRepository;
import com.maharecruitment.gov.in.department.repository.DepartmentTaxRateMasterRepository;
import com.maharecruitment.gov.in.department.service.DepartmentManpowerApplicationService;
import com.maharecruitment.gov.in.department.service.DepartmentRequestIdGenerator;
import com.maharecruitment.gov.in.department.service.DepartmentWorkOrderStorageService;
import com.maharecruitment.gov.in.department.service.model.DepartmentActorContext;
import com.maharecruitment.gov.in.department.service.model.StoredDocument;
import com.maharecruitment.gov.in.department.service.model.WorkOrderDocumentView;
import com.maharecruitment.gov.in.master.dto.ManpowerDesignationMasterResponse;
import com.maharecruitment.gov.in.master.dto.ManpowerDesignationRateResponse;
import com.maharecruitment.gov.in.master.dto.ResourceLevelRefResponse;
import com.maharecruitment.gov.in.master.entity.ProjectType;
import com.maharecruitment.gov.in.master.service.ManpowerDesignationMasterService;
import com.maharecruitment.gov.in.master.service.ManpowerDesignationRateService;
import com.maharecruitment.gov.in.master.service.ProjectMstService;
import com.maharecruitment.gov.in.recruitment.service.RecruitmentNotificationService;
import com.maharecruitment.gov.in.recruitment.service.model.AuditorApprovedNotificationCommand;
import com.maharecruitment.gov.in.recruitment.service.model.DesignationVacancyInput;
import com.project.notification.dto.NotificationCreateRequest;
import com.project.notification.service.NotificationModules;
import com.project.notification.service.NotificationService;

@Service
@Transactional(readOnly = true)
public class DepartmentManpowerApplicationServiceImpl implements DepartmentManpowerApplicationService {

    private static final Logger log = LoggerFactory.getLogger(DepartmentManpowerApplicationServiceImpl.class);

    private static final String ACTION_DRAFT = "draft";
    private static final String ACTION_SUBMIT = "submit";
    private static final String ROLE_HR = "ROLE_HR";
    private static final String ROLE_AUDITOR = "ROLE_AUDITOR";
    private static final String ROLE_DEPARTMENT = "ROLE_DEPARTMENT";
    private static final String ROLE_USER = "ROLE_USER";
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final DepartmentProjectApplicationRepository applicationRepository;
    private final DepartmentProjectApplicationActivityRepository activityRepository;
    private final ManpowerDesignationMasterService designationService;
    private final ManpowerDesignationRateService designationRateService;
    private final ProjectMstService projectMstService;
    private final DepartmentRequestIdGenerator requestIdGenerator;
    private final RecruitmentNotificationService recruitmentNotificationService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final DepartmentWorkOrderStorageService storageService;
    private final DepartmentProformaInvoiceRepository proformaInvoiceRepository;
    private final DepartmentTaxRateMasterRepository taxRateMasterRepository;

    public DepartmentManpowerApplicationServiceImpl(
            DepartmentProjectApplicationRepository applicationRepository,
            DepartmentProjectApplicationActivityRepository activityRepository,
            ManpowerDesignationMasterService designationService,
            ManpowerDesignationRateService designationRateService,
            ProjectMstService projectMstService,
            DepartmentRequestIdGenerator requestIdGenerator,
            RecruitmentNotificationService recruitmentNotificationService,
            NotificationService notificationService,
            UserRepository userRepository,
            DepartmentWorkOrderStorageService storageService,
            DepartmentProformaInvoiceRepository proformaInvoiceRepository,
            DepartmentTaxRateMasterRepository taxRateMasterRepository) {
        this.applicationRepository = applicationRepository;
        this.activityRepository = activityRepository;
        this.designationService = designationService;
        this.designationRateService = designationRateService;
        this.projectMstService = projectMstService;
        this.requestIdGenerator = requestIdGenerator;
        this.recruitmentNotificationService = recruitmentNotificationService;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.proformaInvoiceRepository = proformaInvoiceRepository;
        this.taxRateMasterRepository = taxRateMasterRepository;
    }

    @Override
    public DepartmentProjectApplicationForm initializeApplicationForm(String actorEmail) {
        DepartmentActorContext actorContext = resolveDepartmentActorContext(actorEmail);

        DepartmentProjectApplicationForm form = new DepartmentProjectApplicationForm();
        form.setDepartmentId(actorContext.getDepartmentId());
        form.setDepartmentRegistrationId(actorContext.getDepartmentRegistrationId());
        return form;
    }

    @Override
    public DepartmentProjectApplicationForm getApplicationForEdit(Long applicationId, String actorEmail) {
        DepartmentActorContext actorContext = resolveDepartmentActorContext(actorEmail);
        DepartmentProjectApplicationEntity entity = findOwnedApplication(applicationId, actorContext);
        return toForm(entity);
    }

    @Override
    @Transactional
    public Long saveApplication(DepartmentProjectApplicationForm form, String actionStatus, String actorEmail) {
        DepartmentActorContext actorContext = resolveDepartmentActorContext(actorEmail);
        String normalizedAction = normalizeActionStatus(actionStatus);

        DepartmentProjectApplicationEntity entity;
        boolean createOperation = form.getDepartmentProjectApplicationId() == null;

        if (createOperation) {
            entity = new DepartmentProjectApplicationEntity();
            entity.setDepartmentId(actorContext.getDepartmentId());
            entity.setDepartmentRegistrationId(actorContext.getDepartmentRegistrationId());
            entity.setSubDepartmentId(actorContext.getSubDepartmentId());
            entity.setApplicationStatus(DepartmentApplicationStatus.DRAFT);
            entity.setActive(true);
        } else {
            entity = findOwnedApplication(form.getDepartmentProjectApplicationId(), actorContext);
        }

        DepartmentApplicationStatus previousStatus = entity.getApplicationStatus();
        validateEditAllowed(previousStatus, normalizedAction);

        mapHeader(form, entity);
        if (createOperation) {
            entity.setRequestId(requestIdGenerator.generate("E"));
        }

        List<DepartmentProjectResourceRequirementEntity> requirements = toRequirementEntities(
                form.getResourceRequirements(),
                normalizedAction);
        entity.replaceResourceRequirements(requirements);

        BigDecimal totalEstimatedCost = calculateTotalEstimatedCost(requirements);
        entity.setTotalEstimatedCost(totalEstimatedCost);

        boolean workOrderUploaded = handleWorkOrderDocument(form, entity, normalizedAction);
        DepartmentApplicationStatus nextStatus = resolveNextStatus(previousStatus, normalizedAction);
        entity.setApplicationStatus(nextStatus);

        applyAudit(entity, actorContext, createOperation);

        DepartmentProjectApplicationEntity saved = applicationRepository.save(entity);

        if (createOperation) {
            recordActivity(saved, DepartmentApplicationActivityType.CREATED, previousStatus, nextStatus, actorContext, form.getRemarks());
        } else if (previousStatus != nextStatus) {
            recordActivity(saved,
                    DepartmentApplicationActivityType.STATUS_CHANGED,
                    previousStatus,
                    nextStatus,
                    actorContext,
                    "Application moved to " + nextStatus.getDisplayName() + ".");
        } else if (!workOrderUploaded) {
            recordActivity(saved, DepartmentApplicationActivityType.UPDATED, previousStatus, nextStatus, actorContext, form.getRemarks());
        }

        if (workOrderUploaded) {
            recordActivity(saved,
                    DepartmentApplicationActivityType.WORK_ORDER_UPLOADED,
                    nextStatus,
                    nextStatus,
                    actorContext,
                    "Work-order document uploaded or replaced.");
        }

        if (ACTION_SUBMIT.equals(normalizedAction)) {
            syncProjectMaster(saved);
            notifyHrOnApplicationSubmit(saved);
        }

        log.info(
                "Department manpower application saved. requestId={}, applicationId={}, action={}, status={}, actor={}",
                saved.getRequestId(),
                saved.getDepartmentProjectApplicationId(),
                normalizedAction,
                saved.getApplicationStatus(),
                actorContext.getActorEmail());

        return saved.getDepartmentProjectApplicationId();
    }

    @Override
    public List<DepartmentProjectApplicationSummaryView> getApplicationSummaries(String actorEmail) {
        DepartmentActorContext actorContext = resolveDepartmentActorContext(actorEmail);

        List<DepartmentProjectApplicationEntity> applications = applicationRepository
                .findByDepartmentRegistrationIdOrderByDepartmentProjectApplicationIdDesc(
                        actorContext.getDepartmentRegistrationId());

        return applications.stream()
                .map(entity -> DepartmentProjectApplicationSummaryView.builder()
                        .departmentProjectApplicationId(entity.getDepartmentProjectApplicationId())
                        .requestId(entity.getRequestId())
                        .projectName(entity.getProjectName())
                        .projectCode(entity.getProjectCode())
                        .applicationType(entity.getApplicationType())
                        .applicationStatus(entity.getApplicationStatus())
                        .totalEstimatedCost(entity.getTotalEstimatedCost())
                        .createdDate(entity.getCreatedDate())
                        .updatedDate(entity.getUpdatedDate())
                        .build())
                .toList();
    }

    @Override
    public List<DepartmentProjectApplicationActivityView> getApplicationActivities(Long applicationId,
            String actorEmail) {
        DepartmentActorContext actorContext = resolveDepartmentActorContext(actorEmail);
        DepartmentProjectApplicationEntity application = findOwnedApplication(applicationId, actorContext);

        List<DepartmentProjectApplicationActivityEntity> activities = activityRepository
                .findByApplicationDepartmentProjectApplicationIdOrderByActionTimestampDesc(
                        application.getDepartmentProjectApplicationId());

        return activities.stream()
                .map(activity -> DepartmentProjectApplicationActivityView.builder()
                        .activityType(activity.getActivityType())
                        .previousStatus(activity.getPreviousStatus())
                        .newStatus(activity.getNewStatus())
                        .actorName(activity.getActorName())
                        .actorEmail(activity.getActorEmail())
                        .activityRemarks(activity.getActivityRemarks())
                        .actionTimestamp(activity.getActionTimestamp())
                        .build())
                .toList();
    }

    @Override
    public List<ManpowerDesignationMasterResponse> getAvailableDesignations() {
        return designationService.getAll(false, Pageable.unpaged()).getContent();
    }

    @Override
    public List<LevelOptionView> getLevelsByDesignation(Long designationId) {
        if (designationId == null) {
            return List.of();
        }

        ManpowerDesignationMasterResponse designation = designationService.getById(designationId, false);
        if (designation.getLevels() == null || designation.getLevels().isEmpty()) {
            return List.of();
        }

        return designation.getLevels().stream()
                .sorted(Comparator.comparing(ResourceLevelRefResponse::getLevelName, String.CASE_INSENSITIVE_ORDER))
                .map(level -> LevelOptionView.builder()
                        .levelCode(level.getLevelCode())
                        .levelName(level.getLevelName())
                        .build())
                .toList();
    }

    @Override
    public BigDecimal getMonthlyRate(Long designationId, String levelCode) {
        if (designationId == null || !StringUtils.hasText(levelCode)) {
            throw new DepartmentApplicationException("Designation and level are required.");
        }

        LocalDate today = LocalDate.now();
        List<ManpowerDesignationRateResponse> rates = designationRateService
                .getAll(designationId, false, Pageable.unpaged())
                .getContent();

        return rates.stream()
                .filter(rate -> rate.getLevelCode() != null && rate.getLevelCode().equalsIgnoreCase(levelCode.trim()))
                .filter(rate -> rate.getEffectiveFrom() != null && !rate.getEffectiveFrom().isAfter(today))
                .filter(rate -> rate.getEffectiveTo() == null || !rate.getEffectiveTo().isBefore(today))
                .max(Comparator.comparing(ManpowerDesignationRateResponse::getEffectiveFrom))
                .map(ManpowerDesignationRateResponse::getGrossMonthlyCtc)
                .orElseThrow(() -> new DepartmentApplicationException(
                        "No active rate found for selected designation and level."));
    }

    @Override
    public WorkOrderDocumentView getWorkOrderDocument(Long applicationId, String actorEmail) {
        DepartmentActorContext actorContext = resolveDepartmentActorContext(actorEmail);
        DepartmentProjectApplicationEntity application = findOwnedApplication(applicationId, actorContext);

        if (!storageService.isManagedPath(application.getWorkOrderFilePath())) {
            throw new DepartmentApplicationException("Work-order file is unavailable.");
        }

        return WorkOrderDocumentView.builder()
                .originalFileName(application.getWorkOrderOriginalName())
                .fullPath(application.getWorkOrderFilePath())
                .contentType(application.getWorkOrderFileType())
                .build();
    }

    @Override
    @Transactional
    public DepartmentApplicationStatus reviewByHr(
            Long applicationId,
            HrReviewDecision decision,
            String remarks,
            String actorEmail) {
        DepartmentActorContext actorContext = resolveWorkflowActorContext(actorEmail);
        ensureActorHasRole(actorContext.getActorEmail(), ROLE_HR);

        DepartmentProjectApplicationEntity application = findApplicationById(applicationId);
        DepartmentApplicationStatus previousStatus = application.getApplicationStatus();
        DepartmentApplicationStatus nextStatus = resolveHrDecision(previousStatus, decision);
        application.setApplicationStatus(nextStatus);
        applyAudit(application, actorContext, false);

        DepartmentProjectApplicationEntity saved = applicationRepository.save(application);
        recordActivity(saved, DepartmentApplicationActivityType.HR_REVIEWED, previousStatus, nextStatus, actorContext,
                remarks);
        recordActivity(saved,
                DepartmentApplicationActivityType.STATUS_CHANGED,
                previousStatus,
                nextStatus,
                actorContext,
                "HR decision " + decision + " applied. (" + nextStatus.getDisplayName() + ")");
        notifyDepartmentAfterHrReview(saved, decision, remarks);
        notifyAuditorAfterHrReview(saved, decision, remarks);

        log.info("HR reviewed application. applicationId={}, decision={}, status={} actor={}",
                applicationId,
                decision,
                nextStatus,
                actorContext.getActorEmail());

        return nextStatus;
    }

    @Override
    @Transactional
    public DepartmentApplicationStatus reviewByAuditor(
            Long applicationId,
            AuditorReviewDecision decision,
            String remarks,
            String actorEmail) {
        DepartmentActorContext actorContext = resolveWorkflowActorContext(actorEmail);
        ensureActorHasRole(actorContext.getActorEmail(), ROLE_AUDITOR);

        DepartmentProjectApplicationEntity application = findApplicationById(applicationId);
        DepartmentApplicationStatus currentStatus = application.getApplicationStatus();

        if (currentStatus == DepartmentApplicationStatus.HR_APPROVED) {
            application.setApplicationStatus(DepartmentApplicationStatus.AUDITOR_REVIEW);
            recordActivity(
                    application,
                    DepartmentApplicationActivityType.STATUS_CHANGED,
                    DepartmentApplicationStatus.HR_APPROVED,
                    DepartmentApplicationStatus.AUDITOR_REVIEW,
                    actorContext,
                    "Application moved to " + DepartmentApplicationStatus.AUDITOR_REVIEW.getDisplayName() + ".");
            currentStatus = DepartmentApplicationStatus.AUDITOR_REVIEW;
        }

        DepartmentApplicationStatus nextStatus = resolveAuditorDecision(currentStatus, decision);
        application.setApplicationStatus(nextStatus);
        applyAudit(application, actorContext, false);

        DepartmentProjectApplicationEntity saved = applicationRepository.save(application);
        recordActivity(saved, DepartmentApplicationActivityType.AUDITOR_REVIEWED, currentStatus, nextStatus,
                actorContext, remarks);
        recordActivity(saved,
                DepartmentApplicationActivityType.STATUS_CHANGED,
                currentStatus,
                nextStatus,
                actorContext,
                "Auditor decision " + decision + " applied. (" + nextStatus.getDisplayName() + ")");
        notifyDepartmentAfterAuditorReview(saved, decision, remarks);

        if (nextStatus == DepartmentApplicationStatus.AUDITOR_APPROVED) {
            syncProjectMaster(saved);
            publishRecruitmentNotification(saved);
            generateProformaInvoice(saved);
        }

        log.info("Auditor reviewed application. applicationId={}, decision={}, status={} actor={}",
                applicationId,
                decision,
                nextStatus,
                actorContext.getActorEmail());

        return nextStatus;
    }

    @Override
    @Transactional
    public DepartmentApplicationStatus markCompleted(
            Long applicationId,
            String remarks,
            String actorEmail) {
        DepartmentActorContext actorContext = resolveWorkflowActorContext(actorEmail);
        ensureActorHasRole(actorContext.getActorEmail(), ROLE_AUDITOR);

        DepartmentProjectApplicationEntity application = findApplicationById(applicationId);
        if (application.getApplicationStatus() != DepartmentApplicationStatus.AUDITOR_APPROVED) {
            throw new DepartmentApplicationException("Only auditor-approved applications can be marked completed.");
        }

        DepartmentApplicationStatus previousStatus = application.getApplicationStatus();
        application.setApplicationStatus(DepartmentApplicationStatus.COMPLETED);
        applyAudit(application, actorContext, false);

        DepartmentProjectApplicationEntity saved = applicationRepository.save(application);
        recordActivity(saved, DepartmentApplicationActivityType.COMPLETED, previousStatus,
                DepartmentApplicationStatus.COMPLETED, actorContext, remarks);
        recordActivity(saved,
                DepartmentApplicationActivityType.STATUS_CHANGED,
                previousStatus,
                DepartmentApplicationStatus.COMPLETED,
                actorContext,
                "Application marked completed.");

        log.info("Application marked completed. applicationId={}, actor={}", applicationId,
                actorContext.getActorEmail());
        return DepartmentApplicationStatus.COMPLETED;
    }

    private DepartmentProjectApplicationForm toForm(DepartmentProjectApplicationEntity entity) {
        DepartmentProjectApplicationForm form = new DepartmentProjectApplicationForm();
        form.setDepartmentProjectApplicationId(entity.getDepartmentProjectApplicationId());
        form.setDepartmentId(entity.getDepartmentId());
        form.setDepartmentRegistrationId(entity.getDepartmentRegistrationId());
        form.setRequestId(entity.getRequestId());
        form.setProjectName(entity.getProjectName());
        form.setProjectCode(entity.getProjectCode());
        form.setApplicationType(entity.getApplicationType());
        form.setRemarks(entity.getRemarks());
        form.setMahaitContact(entity.getMahaitContact());
        form.setExistingWorkOrderFilePath(entity.getWorkOrderFilePath());
        form.setExistingWorkOrderOriginalName(entity.getWorkOrderOriginalName());
        form.setTotalEstimatedCost(entity.getTotalEstimatedCost());

        List<DepartmentProjectResourceRequirementForm> requirementForms = entity.getResourceRequirements()
                .stream()
                .map(requirement -> {
                    DepartmentProjectResourceRequirementForm resourceForm = new DepartmentProjectResourceRequirementForm();
                    resourceForm.setDesignationId(requirement.getDesignationId());
                    resourceForm.setDesignationName(requirement.getDesignationName());
                    resourceForm.setLevelCode(requirement.getLevelCode());
                    resourceForm.setLevelName(requirement.getLevelName());
                    resourceForm.setMonthlyRate(requirement.getMonthlyRate());
                    resourceForm.setRequiredQuantity(requirement.getRequiredQuantity());
                    resourceForm.setDurationInMonths(requirement.getDurationInMonths());
                    resourceForm.setTotalCost(requirement.getTotalCost());
                    return resourceForm;
                })
                .toList();

        form.setResourceRequirements(new ArrayList<>(requirementForms));
        return form;
    }

    private void mapHeader(DepartmentProjectApplicationForm form, DepartmentProjectApplicationEntity entity) {
        entity.setProjectName(form.getProjectName());
        entity.setProjectCode(form.getProjectCode());
        entity.setApplicationType(form.getApplicationType());
        entity.setRemarks(form.getRemarks());
        entity.setMahaitContact(form.getMahaitContact());
    }

    private List<DepartmentProjectResourceRequirementEntity> toRequirementEntities(
            List<DepartmentProjectResourceRequirementForm> requirements,
            String actionStatus) {
        if (requirements == null || requirements.isEmpty()) {
            if (ACTION_SUBMIT.equals(actionStatus)) {
                throw new DepartmentApplicationException(
                        "At least one resource requirement is required for submission.");
            }
            return List.of();
        }

        List<DepartmentProjectResourceRequirementEntity> entities = new ArrayList<>();
        for (DepartmentProjectResourceRequirementForm requirement : requirements) {
            validateRequirement(requirement);

            BigDecimal totalCost = requirement.getMonthlyRate()
                    .multiply(BigDecimal.valueOf(requirement.getRequiredQuantity()))
                    .multiply(BigDecimal.valueOf(requirement.getDurationInMonths()))
                    .setScale(2, RoundingMode.HALF_UP);

            DepartmentProjectResourceRequirementEntity entity = new DepartmentProjectResourceRequirementEntity();
            entity.setDesignationId(requirement.getDesignationId());
            entity.setDesignationName(requirement.getDesignationName());
            entity.setLevelCode(requirement.getLevelCode());
            entity.setLevelName(requirement.getLevelName());
            entity.setMonthlyRate(requirement.getMonthlyRate().setScale(2, RoundingMode.HALF_UP));
            entity.setRequiredQuantity(requirement.getRequiredQuantity());
            entity.setDurationInMonths(requirement.getDurationInMonths());
            entity.setTotalCost(totalCost);

            entities.add(entity);
        }
        return entities;
    }

    private void validateRequirement(DepartmentProjectResourceRequirementForm requirement) {
        if (requirement == null) {
            throw new DepartmentApplicationException("Resource requirement row is invalid.");
        }

        if (!StringUtils.hasText(requirement.getDesignationName())) {
            throw new DepartmentApplicationException("Designation name is required in resource requirements.");
        }
        if (!StringUtils.hasText(requirement.getLevelCode())) {
            throw new DepartmentApplicationException("Level code is required in resource requirements.");
        }
        if (requirement.getMonthlyRate() == null || requirement.getMonthlyRate().compareTo(BigDecimal.ZERO) <= 0) {
            throw new DepartmentApplicationException("Monthly rate must be greater than zero.");
        }
        if (requirement.getRequiredQuantity() == null || requirement.getRequiredQuantity() <= 0) {
            throw new DepartmentApplicationException("Required quantity must be at least 1.");
        }
        if (requirement.getDurationInMonths() == null || requirement.getDurationInMonths() <= 0) {
            throw new DepartmentApplicationException("Duration in months must be at least 1.");
        }
    }

    private BigDecimal calculateTotalEstimatedCost(List<DepartmentProjectResourceRequirementEntity> requirements) {
        if (requirements == null || requirements.isEmpty()) {
            return ZERO;
        }

        return requirements.stream()
                .map(DepartmentProjectResourceRequirementEntity::getTotalCost)
                .reduce(ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private boolean handleWorkOrderDocument(
            DepartmentProjectApplicationForm form,
            DepartmentProjectApplicationEntity entity,
            String actionStatus) {
        if (form.getWorkOrderFile() != null && !form.getWorkOrderFile().isEmpty()) {
            StoredDocument storedDocument = storageService.storeWorkOrder(form.getWorkOrderFile(),
                    entity.getWorkOrderFilePath());
            entity.setWorkOrderOriginalName(storedDocument.getOriginalFileName());
            entity.setWorkOrderFilePath(storedDocument.getFullPath());
            entity.setWorkOrderFileType(storedDocument.getContentType());
            entity.setWorkOrderFileSize(storedDocument.getFileSize());
            return true;
        }

        if (!storageService.isManagedPath(entity.getWorkOrderFilePath())) {
            throw new DepartmentApplicationException("Work-order document is mandatory.");
        }

        return false;
    }

    private void validateEditAllowed(DepartmentApplicationStatus currentStatus, String actionStatus) {
        if (currentStatus == null) {
            return;
        }

        if (DepartmentApplicationStatus.HR_REJECTED == currentStatus
                || DepartmentApplicationStatus.HR_APPROVED == currentStatus
                || DepartmentApplicationStatus.AUDITOR_REVIEW == currentStatus
                || DepartmentApplicationStatus.AUDITOR_APPROVED == currentStatus
                || DepartmentApplicationStatus.COMPLETED == currentStatus) {
            throw new DepartmentApplicationException(
                    "Application cannot be modified in current workflow state: " + currentStatus);
        }

        if (DepartmentApplicationStatus.SUBMITTED_TO_HR == currentStatus) {
            throw new DepartmentApplicationException("Application is under HR review and cannot be modified.");
        }
    }

    private DepartmentApplicationStatus resolveNextStatus(
            DepartmentApplicationStatus currentStatus,
            String actionStatus) {
        if (ACTION_DRAFT.equals(actionStatus)) {
            if (currentStatus == DepartmentApplicationStatus.HR_SENT_BACK
                    || currentStatus == DepartmentApplicationStatus.AUDITOR_SENT_BACK) {
                return DepartmentApplicationStatus.CORRECTED_BY_DEPARTMENT;
            }
            return DepartmentApplicationStatus.DRAFT;
        }

        if (ACTION_SUBMIT.equals(actionStatus)) {
            if (currentStatus == null || currentStatus == DepartmentApplicationStatus.DRAFT) {
                return DepartmentApplicationStatus.SUBMITTED_TO_HR;
            }
            if (currentStatus == DepartmentApplicationStatus.HR_SENT_BACK
                    || currentStatus == DepartmentApplicationStatus.AUDITOR_SENT_BACK
                    || currentStatus == DepartmentApplicationStatus.CORRECTED_BY_DEPARTMENT) {
                return DepartmentApplicationStatus.CORRECTED_BY_DEPARTMENT;
            }
            throw new DepartmentApplicationException(
                    "Application cannot be submitted in current state: " + currentStatus);
        }

        throw new DepartmentApplicationException("Invalid action status supplied.");
    }

    private String normalizeActionStatus(String actionStatus) {
        if (!StringUtils.hasText(actionStatus)) {
            return ACTION_SUBMIT;
        }

        String normalized = actionStatus.trim().toLowerCase(Locale.ROOT);
        if (!ACTION_DRAFT.equals(normalized) && !ACTION_SUBMIT.equals(normalized)) {
            throw new DepartmentApplicationException("Unsupported action status: " + actionStatus);
        }
        return normalized;
    }

    private void recordActivity(
            DepartmentProjectApplicationEntity application,
            DepartmentApplicationActivityType activityType,
            DepartmentApplicationStatus previousStatus,
            DepartmentApplicationStatus newStatus,
            DepartmentActorContext actorContext,
            String remarks) {
        DepartmentProjectApplicationActivityEntity activity = new DepartmentProjectApplicationActivityEntity();
        activity.setApplication(application);
        activity.setActivityType(activityType);
        activity.setPreviousStatus(previousStatus);
        activity.setNewStatus(newStatus);
        activity.setActorUserId(actorContext.getUserId());
        activity.setActorEmail(actorContext.getActorEmail());
        activity.setActorName(actorContext.getActorName());
        activity.setActivityRemarks(remarks);
        activity.setActionTimestamp(LocalDateTime.now());
        activityRepository.save(activity);
    }

    private DepartmentProjectApplicationEntity findOwnedApplication(
            Long applicationId,
            DepartmentActorContext actorContext) {
        return applicationRepository
                .findByDepartmentProjectApplicationIdAndDepartmentRegistrationId(
                        applicationId,
                        actorContext.getDepartmentRegistrationId())
                .orElseThrow(() -> new DepartmentApplicationException("Application not found."));
    }

    private DepartmentProjectApplicationEntity findApplicationById(Long applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new DepartmentApplicationException("Application not found."));
    }

    private DepartmentActorContext resolveDepartmentActorContext(String actorEmail) {
        if (!StringUtils.hasText(actorEmail)) {
            throw new DepartmentApplicationException("Authenticated user is required.");
        }

        User user = userRepository.findByEmail(actorEmail);
        if (user == null) {
            throw new DepartmentApplicationException("Authenticated user not found.");
        }

        DepartmentRegistrationEntity departmentRegistration = user.getDepartmentRegistrationId();
        if (departmentRegistration == null) {
            throw new DepartmentApplicationException("Department profile is not linked to this user.");
        }

        return DepartmentActorContext.builder()
                .userId(user.getId())
                .actorName(user.getName())
                .actorEmail(user.getEmail())
                .departmentId(departmentRegistration.getDepartmentId())
                .departmentRegistrationId(departmentRegistration.getDepartmentRegistrationId())
                .subDepartmentId(departmentRegistration.getSubDeptId())
                .build();
    }

    private DepartmentActorContext resolveWorkflowActorContext(String actorEmail) {
        if (!StringUtils.hasText(actorEmail)) {
            throw new DepartmentApplicationException("Authenticated user is required.");
        }

        User user = userRepository.findByEmail(actorEmail);
        if (user == null) {
            throw new DepartmentApplicationException("Authenticated user not found.");
        }

        DepartmentRegistrationEntity departmentRegistration = user.getDepartmentRegistrationId();

        return DepartmentActorContext.builder()
                .userId(user.getId())
                .actorName(user.getName())
                .actorEmail(user.getEmail())
                .departmentId(departmentRegistration != null ? departmentRegistration.getDepartmentId() : null)
                .departmentRegistrationId(departmentRegistration != null
                        ? departmentRegistration.getDepartmentRegistrationId()
                        : null)
                .subDepartmentId(departmentRegistration != null ? departmentRegistration.getSubDeptId() : null)
                .build();
    }

    private void ensureActorHasRole(String actorEmail, String... acceptedRoles) {
        User user = userRepository.findByEmail(actorEmail);
        if (user == null || user.getRoles() == null) {
            throw new DepartmentApplicationException("Authenticated user does not have required role.");
        }

        List<String> normalized = List.of(acceptedRoles).stream()
                .map(role -> role.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toList());

        boolean authorized = user.getRoles().stream()
                .map(role -> role.getName() == null ? "" : role.getName().trim().toUpperCase(Locale.ROOT))
                .anyMatch(roleName -> normalized.contains(roleName));

        if (!authorized) {
            throw new DepartmentApplicationException("User is not authorized for this workflow action.");
        }
    }

    private void applyAudit(
            DepartmentProjectApplicationEntity entity,
            DepartmentActorContext actorContext,
            boolean createOperation) {
        LocalDateTime now = LocalDateTime.now();

        if (createOperation && entity.getCreatedDate() == null) {
            entity.setCreatedDate(now);
            entity.setCreatedBy(actorContext.getActorEmail());
        }
        entity.setUpdatedDate(now);
        entity.setUpdatedBy(actorContext.getActorEmail());
    }

    private DepartmentApplicationStatus resolveHrDecision(
            DepartmentApplicationStatus currentStatus,
            HrReviewDecision decision) {
        if (decision == null) {
            throw new DepartmentApplicationException("HR decision is required.");
        }

        if (currentStatus != DepartmentApplicationStatus.SUBMITTED_TO_HR
                && currentStatus != DepartmentApplicationStatus.CORRECTED_BY_DEPARTMENT) {
            throw new DepartmentApplicationException("HR review is not allowed in current state: " + currentStatus);
        }

        switch (decision) {
            case APPROVE:
                return DepartmentApplicationStatus.HR_APPROVED;
            case REJECT:
                return DepartmentApplicationStatus.HR_REJECTED;
            case SEND_BACK:
                return DepartmentApplicationStatus.HR_SENT_BACK;
            default:
                throw new DepartmentApplicationException("Unsupported HR decision: " + decision);
        }
    }

    private DepartmentApplicationStatus resolveAuditorDecision(
            DepartmentApplicationStatus currentStatus,
            AuditorReviewDecision decision) {
        if (decision == null) {
            throw new DepartmentApplicationException("Auditor decision is required.");
        }

        if (currentStatus != DepartmentApplicationStatus.AUDITOR_REVIEW) {
            throw new DepartmentApplicationException(
                    "Auditor review is not allowed in current state: " + currentStatus);
        }

        switch (decision) {
            case APPROVE:
                return DepartmentApplicationStatus.AUDITOR_APPROVED;
            case SEND_BACK:
                return DepartmentApplicationStatus.AUDITOR_SENT_BACK;
            default:
                throw new DepartmentApplicationException("Unsupported auditor decision: " + decision);
        }
    }

    private void generateProformaInvoice(DepartmentProjectApplicationEntity application) {
        try {
            BigDecimal baseAmount = application.getTotalEstimatedCost() != null ? application.getTotalEstimatedCost() : ZERO;
            LocalDate applicableDate = application.getCreatedDate() != null ? application.getCreatedDate().toLocalDate() : LocalDate.now();
            
            BigDecimal taxAmount = calculateTaxAmount(baseAmount, applicableDate);
            BigDecimal totalAmount = baseAmount.add(taxAmount).setScale(2, RoundingMode.HALF_UP);

            String piNumber = generatePiNumber();

            DepartmentProformaInvoiceEntity piEntity = DepartmentProformaInvoiceEntity.builder()
                    .application(application)
                    .piNumber(piNumber)
                    .baseAmount(baseAmount)
                    .taxAmount(taxAmount)
                    .totalAmount(totalAmount)
                    .active(true)
                    .build();

            // Audit fields are handled by Auditable or manually if needed
            piEntity.setCreatedBy(application.getUpdatedBy());
            piEntity.setCreatedDate(LocalDateTime.now());
            piEntity.setUpdatedBy(application.getUpdatedBy());
            piEntity.setUpdatedDate(LocalDateTime.now());

            proformaInvoiceRepository.save(piEntity);
            log.info("Proforma Invoice generated for application. applicationId={}, piNumber={}, totalAmount={}", 
                    application.getDepartmentProjectApplicationId(), piNumber, totalAmount);
        } catch (Exception e) {
            log.error("Failed to generate Proforma Invoice for application {}. Reason: {}", 
                    application.getDepartmentProjectApplicationId(), e.getMessage(), e);
            // We don't throw exception here to avoid rolling back the approval, 
            // but in a real system we might want to ensure PI is generated.
        }
    }

    private BigDecimal calculateTaxAmount(BigDecimal baseCost, LocalDate applicableDate) {
        List<DepartmentTaxRateMasterEntity> taxRates = taxRateMasterRepository.findApplicableTaxRates(applicableDate);
        if (taxRates.isEmpty()) {
            return ZERO;
        }

        BigDecimal hundred = BigDecimal.valueOf(100);
        return taxRates.stream()
                .filter(taxRate -> taxRate.getRatePercentage() != null && taxRate.getRatePercentage().compareTo(BigDecimal.ZERO) > 0)
                .map(taxRate -> baseCost
                        .multiply(taxRate.getRatePercentage())
                        .divide(hundred, 2, RoundingMode.HALF_UP))
                .reduce(ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String generatePiNumber() {
        int year = LocalDate.now().getYear();
        String prefix = "PI/" + year + "/";
        String maxPiNumber = proformaInvoiceRepository.findMaxPiNumberByPrefix(prefix + "%");
        
        int nextSequence = 1;
        if (maxPiNumber != null && maxPiNumber.length() > prefix.length()) {
            try {
                String sequencePart = maxPiNumber.substring(prefix.length());
                nextSequence = Integer.parseInt(sequencePart) + 1;
            } catch (NumberFormatException e) {
                log.warn("Unable to parse sequence from max PI number: {}. Starting from 1.", maxPiNumber);
            }
        }
        
        return String.format("%s%05d", prefix, nextSequence);
    }

    private void syncProjectMaster(DepartmentProjectApplicationEntity applicationEntity) {
        if (applicationEntity == null) {
            return;
        }

        if (!StringUtils.hasText(applicationEntity.getProjectName())) {
            throw new DepartmentApplicationException("Project name is required for project master sync.");
        }
        if (applicationEntity.getDepartmentRegistrationId() == null) {
            throw new DepartmentApplicationException("Department registration id is required for project master sync.");
        }
        if (applicationEntity.getDepartmentProjectApplicationId() == null) {
            throw new DepartmentApplicationException("Application id is required for project master sync.");
        }
        if (applicationEntity.getApplicationType() == null) {
            throw new DepartmentApplicationException("Application type is required for project master sync.");
        }

        ProjectType projectType = mapToProjectType(applicationEntity.getApplicationType());

        projectMstService.upsertFromDepartmentApplication(
                applicationEntity.getProjectName(),
                projectType,
                applicationEntity.getDepartmentRegistrationId(),
                applicationEntity.getDepartmentProjectApplicationId());
    }

    private ProjectType mapToProjectType(DepartmentApplicationType applicationType) {
        try {
            return ProjectType.valueOf(applicationType.name());
        } catch (IllegalArgumentException ex) {
            throw new DepartmentApplicationException(
                    "Unsupported application type for project master sync: " + applicationType);
        }
    }

    private void publishRecruitmentNotification(DepartmentProjectApplicationEntity applicationEntity) {
        if (applicationEntity == null || applicationEntity.getResourceRequirements() == null
                || applicationEntity.getResourceRequirements().isEmpty()) {
            throw new DepartmentApplicationException(
                    "Resource requirements are required to generate recruitment notification.");
        }

        AuditorApprovedNotificationCommand command = AuditorApprovedNotificationCommand.builder()
                .requestId(applicationEntity.getRequestId())
                .departmentRegistrationId(applicationEntity.getDepartmentRegistrationId())
                .departmentProjectApplicationId(applicationEntity.getDepartmentProjectApplicationId())
                .designationVacancies(applicationEntity.getResourceRequirements().stream()
                        .map(requirement -> DesignationVacancyInput.builder()
                                .designationId(requirement.getDesignationId())
                                .levelCode(requirement.getLevelCode())
                                .numberOfVacancy(requirement.getRequiredQuantity() != null
                                        ? requirement.getRequiredQuantity().longValue()
                                        : null)
                                .build())
                        .toList())
                .build();

        recruitmentNotificationService.upsertFromAuditorApproval(command);
    }

    private void notifyHrOnApplicationSubmit(DepartmentProjectApplicationEntity applicationEntity) {
        if (applicationEntity == null || applicationEntity.getDepartmentProjectApplicationId() == null) {
            return;
        }

        List<Long> hrUserIds = resolveUserIdsByRoleNames(ROLE_HR);
        if (hrUserIds.isEmpty()) {
            log.warn("No HR users found to notify for applicationId={}, requestId={}",
                    applicationEntity.getDepartmentProjectApplicationId(),
                    applicationEntity.getRequestId());
            return;
        }

        String requestId = StringUtils.hasText(applicationEntity.getRequestId())
                ? applicationEntity.getRequestId()
                : "N/A";
        String projectName = StringUtils.hasText(applicationEntity.getProjectName())
                ? applicationEntity.getProjectName()
                : "Department project";

        String message = "Request " + requestId + " for project " + projectName
                + " has been submitted and is awaiting HR review.";

        notificationService.createNotification(NotificationCreateRequest.builder()
                .eventType("MANPOWER_REQUEST_SUBMITTED")
                .title("New manpower request submitted")
                .message(message)
                .referenceId(applicationEntity.getDepartmentProjectApplicationId())
                .module(NotificationModules.HR_DEPARTMENT_REQUESTS)
                .userIds(hrUserIds)
                .actionRequired(true)
                .build());

        log.info("Notification pushed to HR users. applicationId={}, recipients={}",
                applicationEntity.getDepartmentProjectApplicationId(),
                hrUserIds.size());
    }

    private void notifyDepartmentAfterHrReview(
            DepartmentProjectApplicationEntity applicationEntity,
            HrReviewDecision decision,
            String remarks) {
        if (applicationEntity == null || decision == null) {
            return;
        }

        if (decision != HrReviewDecision.REJECT && decision != HrReviewDecision.SEND_BACK) {
            return;
        }

        List<Long> recipientUserIds = resolveDepartmentRecipientUserIds(applicationEntity);
        if (recipientUserIds.isEmpty()) {
            return;
        }

        String requestId = StringUtils.hasText(applicationEntity.getRequestId())
                ? applicationEntity.getRequestId()
                : "N/A";
        String projectName = StringUtils.hasText(applicationEntity.getProjectName())
                ? applicationEntity.getProjectName()
                : "Department project";

        boolean actionRequired = decision == HrReviewDecision.SEND_BACK;
        String title = actionRequired
                ? "Manpower request sent back by HR"
                : "Manpower request rejected by HR";
        String eventType = actionRequired
                ? "MANPOWER_REQUEST_HR_SENT_BACK"
                : "MANPOWER_REQUEST_HR_REJECTED";

        String baseMessage = actionRequired
                ? "Request " + requestId + " for project " + projectName + " has been sent back by HR for correction."
                : "Request " + requestId + " for project " + projectName + " has been rejected by HR.";
        String message = StringUtils.hasText(remarks)
                ? baseMessage + " Remarks: " + remarks.trim()
                : baseMessage;

        notificationService.createNotification(NotificationCreateRequest.builder()
                .eventType(eventType)
                .title(title)
                .message(message)
                .referenceId(applicationEntity.getDepartmentProjectApplicationId())
                .module(NotificationModules.DEPARTMENT_MANPOWER)
                .userIds(recipientUserIds)
                .actionRequired(actionRequired)
                .build());
    }

    private void notifyAuditorAfterHrReview(
            DepartmentProjectApplicationEntity applicationEntity,
            HrReviewDecision decision,
            String remarks) {
        if (applicationEntity == null || decision == null || decision != HrReviewDecision.APPROVE) {
            return;
        }

        List<Long> auditorUserIds = resolveUserIdsByRoleNames(ROLE_AUDITOR);
        if (auditorUserIds.isEmpty()) {
            log.warn("No Auditor users found to notify for HR-approved applicationId={}, requestId={}",
                    applicationEntity.getDepartmentProjectApplicationId(),
                    applicationEntity.getRequestId());
            return;
        }

        String requestId = StringUtils.hasText(applicationEntity.getRequestId())
                ? applicationEntity.getRequestId()
                : "N/A";
        String projectName = StringUtils.hasText(applicationEntity.getProjectName())
                ? applicationEntity.getProjectName()
                : "Department project";

        String baseMessage = "Request " + requestId + " for project " + projectName
                + " has been approved by HR and is pending auditor review.";
        String message = StringUtils.hasText(remarks)
                ? baseMessage + " HR remarks: " + remarks.trim()
                : baseMessage;

        notificationService.createNotification(NotificationCreateRequest.builder()
                .eventType("MANPOWER_REQUEST_HR_APPROVED")
                .title("Manpower request pending auditor review")
                .message(message)
                .referenceId(applicationEntity.getDepartmentProjectApplicationId())
                .module(NotificationModules.AUDITOR_DEPARTMENT_REQUESTS)
                .userIds(auditorUserIds)
                .actionRequired(true)
                .build());

        log.info("Notification pushed to Auditor users. applicationId={}, recipients={}",
                applicationEntity.getDepartmentProjectApplicationId(),
                auditorUserIds.size());
    }

    private void notifyDepartmentAfterAuditorReview(
            DepartmentProjectApplicationEntity applicationEntity,
            AuditorReviewDecision decision,
            String remarks) {
        if (applicationEntity == null || decision == null) {
            return;
        }

        if (decision != AuditorReviewDecision.SEND_BACK && decision != AuditorReviewDecision.APPROVE) {
            return;
        }

        List<Long> recipientUserIds = resolveDepartmentRecipientUserIds(applicationEntity);
        if (recipientUserIds.isEmpty()) {
            return;
        }

        String requestId = StringUtils.hasText(applicationEntity.getRequestId())
                ? applicationEntity.getRequestId()
                : "N/A";
        String projectName = StringUtils.hasText(applicationEntity.getProjectName())
                ? applicationEntity.getProjectName()
                : "Department project";

        boolean actionRequired = decision == AuditorReviewDecision.SEND_BACK;
        String eventType = actionRequired
                ? "MANPOWER_REQUEST_AUDITOR_SENT_BACK"
                : "MANPOWER_REQUEST_AUDITOR_APPROVED";
        String title = actionRequired
                ? "Manpower request sent back by Auditor"
                : "Manpower request approved by Auditor";

        String baseMessage = actionRequired
                ? "Request " + requestId + " for project " + projectName
                        + " has been sent back by Auditor for corrections."
                : "Request " + requestId + " for project " + projectName
                        + " has been approved by Auditor.";
        String message = StringUtils.hasText(remarks)
                ? baseMessage + " Auditor remarks: " + remarks.trim()
                : baseMessage;

        notificationService.createNotification(NotificationCreateRequest.builder()
                .eventType(eventType)
                .title(title)
                .message(message)
                .referenceId(applicationEntity.getDepartmentProjectApplicationId())
                .module(NotificationModules.DEPARTMENT_MANPOWER)
                .userIds(recipientUserIds)
                .actionRequired(actionRequired)
                .build());
    }

    private List<Long> resolveDepartmentRecipientUserIds(DepartmentProjectApplicationEntity applicationEntity) {
        List<Long> recipients = new ArrayList<>();

        if (applicationEntity.getDepartmentRegistrationId() != null) {
            List<Long> departmentUserIds = resolveDepartmentUserIdsByRoleNames(
                    applicationEntity.getDepartmentRegistrationId(),
                    ROLE_DEPARTMENT,
                    ROLE_USER);
            if (departmentUserIds != null && !departmentUserIds.isEmpty()) {
                recipients.addAll(departmentUserIds);
            }

            if (recipients.isEmpty()) {
                List<Long> linkedUserIds = userRepository.findDistinctUserIdsByDepartmentRegistrationId(
                        applicationEntity.getDepartmentRegistrationId());
                if (linkedUserIds != null && !linkedUserIds.isEmpty()) {
                    recipients.addAll(linkedUserIds);
                }
            }
        }

        if (StringUtils.hasText(applicationEntity.getCreatedBy())) {
            User creatorUser = userRepository.findByEmailIgnoreCase(applicationEntity.getCreatedBy()).orElse(null);
            if (creatorUser != null && creatorUser.getId() != null) {
                recipients.add(creatorUser.getId());
            }
        }

        List<Long> distinctRecipients = recipients.stream().distinct().toList();
        if (distinctRecipients.isEmpty()) {
            log.warn("No department recipients resolved for applicationId={}, requestId={}, departmentRegistrationId={}",
                    applicationEntity.getDepartmentProjectApplicationId(),
                    applicationEntity.getRequestId(),
                    applicationEntity.getDepartmentRegistrationId());
        } else {
            log.info("Department recipients resolved for applicationId={}, recipients={}",
                    applicationEntity.getDepartmentProjectApplicationId(),
                    distinctRecipients.size());
        }

        return distinctRecipients;
    }

    private List<Long> resolveUserIdsByRoleNames(String... roleNames) {
        List<Long> userIds = new ArrayList<>();
        if (roleNames == null || roleNames.length == 0) {
            return userIds;
        }

        for (String roleName : roleNames) {
            if (!StringUtils.hasText(roleName)) {
                continue;
            }
            List<Long> matchedIds = userRepository.findDistinctUserIdsByRoleName(roleName);
            if (matchedIds != null && !matchedIds.isEmpty()) {
                userIds.addAll(matchedIds);
            }
        }
        return userIds.stream().distinct().toList();
    }

    private List<Long> resolveDepartmentUserIdsByRoleNames(Long departmentRegistrationId, String... roleNames) {
        List<Long> userIds = new ArrayList<>();
        if (departmentRegistrationId == null || roleNames == null || roleNames.length == 0) {
            return userIds;
        }

        for (String roleName : roleNames) {
            if (!StringUtils.hasText(roleName)) {
                continue;
            }
            List<Long> matchedIds = userRepository.findDistinctUserIdsByDepartmentRegistrationIdAndRoleName(
                    departmentRegistrationId,
                    roleName);
            if (matchedIds != null && !matchedIds.isEmpty()) {
                userIds.addAll(matchedIds);
            }
        }
        return userIds.stream().distinct().toList();
    }
}
