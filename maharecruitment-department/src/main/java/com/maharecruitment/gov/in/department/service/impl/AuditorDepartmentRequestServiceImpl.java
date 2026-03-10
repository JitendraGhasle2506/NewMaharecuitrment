package com.maharecruitment.gov.in.department.service.impl;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.auth.entity.DepartmentRegistrationEntity;
import com.maharecruitment.gov.in.auth.repository.DepartmentRegistrationRepository;
import com.maharecruitment.gov.in.department.dto.DepartmentProjectApplicationActivityView;
import com.maharecruitment.gov.in.department.entity.AuditorReviewDecision;
import com.maharecruitment.gov.in.department.entity.DepartmentApplicationStatus;
import com.maharecruitment.gov.in.department.entity.DepartmentProjectApplicationEntity;
import com.maharecruitment.gov.in.department.entity.DepartmentProjectResourceRequirementEntity;
import com.maharecruitment.gov.in.department.entity.DepartmentTaxRateMasterEntity;
import com.maharecruitment.gov.in.department.exception.DepartmentApplicationException;
import com.maharecruitment.gov.in.department.repository.DepartmentProjectApplicationActivityRepository;
import com.maharecruitment.gov.in.department.repository.DepartmentProjectApplicationRepository;
import com.maharecruitment.gov.in.department.repository.DepartmentTaxRateMasterRepository;
import com.maharecruitment.gov.in.department.repository.projection.DepartmentSubmittedProjectCountProjection;
import com.maharecruitment.gov.in.department.service.AuditorDepartmentRequestService;
import com.maharecruitment.gov.in.department.service.DepartmentManpowerApplicationService;
import com.maharecruitment.gov.in.department.service.DepartmentProfileDocumentStorageService;
import com.maharecruitment.gov.in.department.service.model.AuditorApplicationTaxComponentView;
import com.maharecruitment.gov.in.department.service.model.AuditorDepartmentApplicationResourceRequirementView;
import com.maharecruitment.gov.in.department.service.model.AuditorDepartmentApplicationReviewDetailView;
import com.maharecruitment.gov.in.department.service.model.AuditorDepartmentRegistrationDetailView;
import com.maharecruitment.gov.in.department.service.model.AuditorDepartmentSubDepartmentRequestView;
import com.maharecruitment.gov.in.department.service.model.AuditorDepartmentSubmittedApplicationView;
import com.maharecruitment.gov.in.department.service.model.AuditorParentDepartmentRequestView;
import com.maharecruitment.gov.in.department.service.model.AuditorSubDepartmentApplicationDetailView;
import com.maharecruitment.gov.in.department.service.model.AuditorSubDepartmentProjectCountView;
import com.maharecruitment.gov.in.department.service.model.DepartmentProfileDocumentType;
import com.maharecruitment.gov.in.department.service.model.WorkOrderDocumentView;
import com.maharecruitment.gov.in.master.entity.DepartmentMst;
import com.maharecruitment.gov.in.master.entity.SubDepartment;
import com.maharecruitment.gov.in.master.repository.DepartmentMstRepository;
import com.maharecruitment.gov.in.master.repository.SubDepartmentRepository;

@Service
@Transactional(readOnly = true)
public class AuditorDepartmentRequestServiceImpl implements AuditorDepartmentRequestService {

    private static final Logger log = LoggerFactory.getLogger(AuditorDepartmentRequestServiceImpl.class);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private static final String UNMAPPED_SUB_DEPARTMENT = "Unmapped Sub-Department";

    private static final EnumSet<DepartmentApplicationStatus> AUDITOR_QUEUE_STATUSES = EnumSet.of(
            DepartmentApplicationStatus.HR_APPROVED,
            DepartmentApplicationStatus.AUDITOR_REVIEW,
            DepartmentApplicationStatus.AUDITOR_APPROVED);

    private final DepartmentRegistrationRepository departmentRegistrationRepository;
    private final DepartmentProjectApplicationRepository departmentProjectApplicationRepository;
    private final DepartmentProjectApplicationActivityRepository activityRepository;
    private final DepartmentTaxRateMasterRepository taxRateMasterRepository;
    private final DepartmentManpowerApplicationService manpowerApplicationService;
    private final DepartmentProfileDocumentStorageService profileDocumentStorageService;
    private final DepartmentMstRepository departmentMstRepository;
    private final SubDepartmentRepository subDepartmentRepository;

    public AuditorDepartmentRequestServiceImpl(
            DepartmentRegistrationRepository departmentRegistrationRepository,
            DepartmentProjectApplicationRepository departmentProjectApplicationRepository,
            DepartmentProjectApplicationActivityRepository activityRepository,
            DepartmentTaxRateMasterRepository taxRateMasterRepository,
            DepartmentManpowerApplicationService manpowerApplicationService,
            DepartmentProfileDocumentStorageService profileDocumentStorageService,
            DepartmentMstRepository departmentMstRepository,
            SubDepartmentRepository subDepartmentRepository) {
        this.departmentRegistrationRepository = departmentRegistrationRepository;
        this.departmentProjectApplicationRepository = departmentProjectApplicationRepository;
        this.activityRepository = activityRepository;
        this.taxRateMasterRepository = taxRateMasterRepository;
        this.manpowerApplicationService = manpowerApplicationService;
        this.profileDocumentStorageService = profileDocumentStorageService;
        this.departmentMstRepository = departmentMstRepository;
        this.subDepartmentRepository = subDepartmentRepository;
    }

    @Override
    public List<AuditorParentDepartmentRequestView> getParentDepartmentRequests() {
        List<DepartmentRegistrationEntity> registrations = departmentRegistrationRepository.findAll(
                Sort.by(Sort.Direction.ASC, "departmentName"));

        if (registrations.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> projectCountByRegistration = getQueueProjectCountByRegistration();

        Map<Long, String> departmentNameFallbackById = new LinkedHashMap<>();
        Map<Long, Set<Long>> subDepartmentIdsByDepartment = new LinkedHashMap<>();
        Map<Long, Long> projectCountByDepartment = new LinkedHashMap<>();
        Map<Long, Map<Long, Long>> projectCountByDepartmentAndSubDepartment = new LinkedHashMap<>();

        registrations.stream()
                .filter(registration -> registration.getDepartmentId() != null)
                .forEach(registration -> {
                    Long departmentId = registration.getDepartmentId();
                    Long subDepartmentId = registration.getSubDeptId();
                    Long registrationId = registration.getDepartmentRegistrationId();

                    if (StringUtils.hasText(registration.getDepartmentName())
                            && !StringUtils.hasText(departmentNameFallbackById.get(departmentId))) {
                        departmentNameFallbackById.put(departmentId, registration.getDepartmentName());
                    }

                    subDepartmentIdsByDepartment.computeIfAbsent(departmentId, ignored -> new LinkedHashSet<>())
                            .add(subDepartmentId);

                    Long projectCount = projectCountByRegistration.getOrDefault(registrationId, 0L);
                    projectCountByDepartment.put(
                            departmentId,
                            projectCountByDepartment.getOrDefault(departmentId, 0L) + projectCount);

                    Map<Long, Long> projectCountBySubDepartment = projectCountByDepartmentAndSubDepartment
                            .computeIfAbsent(departmentId, ignored -> new LinkedHashMap<>());
                    projectCountBySubDepartment.put(
                            subDepartmentId,
                            projectCountBySubDepartment.getOrDefault(subDepartmentId, 0L) + projectCount);
                });

        Set<Long> parentDepartmentIds = new LinkedHashSet<>(subDepartmentIdsByDepartment.keySet());
        if (parentDepartmentIds.isEmpty()) {
            return List.of();
        }

        Set<Long> allSubDepartmentIds = subDepartmentIdsByDepartment.values().stream()
                .flatMap(Set::stream)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<Long, String> subDepartmentNameById = subDepartmentRepository.findAllById(allSubDepartmentIds)
                .stream()
                .collect(Collectors.toMap(
                        SubDepartment::getSubDeptId,
                        SubDepartment::getSubDeptName,
                        (first, second) -> first));

        Map<Long, String> departmentNameMasterById = departmentMstRepository.findAllById(parentDepartmentIds)
                .stream()
                .collect(Collectors.toMap(
                        DepartmentMst::getDepartmentId,
                        DepartmentMst::getDepartmentName,
                        (first, second) -> first));

        List<AuditorParentDepartmentRequestView> parentDepartments = parentDepartmentIds.stream()
                .map(departmentId -> AuditorParentDepartmentRequestView.builder()
                        .departmentId(departmentId)
                        .departmentName(resolveDepartmentName(
                                departmentId,
                                departmentNameMasterById,
                                departmentNameFallbackById))
                        .projectApplicationCount(projectCountByDepartment.getOrDefault(departmentId, 0L))
                        .registeredSubDepartments(resolveRegisteredSubDepartments(
                                departmentId,
                                subDepartmentIdsByDepartment,
                                subDepartmentNameById))
                        .subDepartmentProjectCounts(resolveSubDepartmentProjectCounts(
                                departmentId,
                                subDepartmentIdsByDepartment,
                                subDepartmentNameById,
                                projectCountByDepartmentAndSubDepartment))
                        .build())
                .sorted(Comparator.comparing(
                        parent -> safeLower(parent.getDepartmentName()),
                        Comparator.nullsLast(String::compareTo)))
                .toList();

        log.info("Auditor parent department queue loaded. parentDepartments={}", parentDepartments.size());
        return parentDepartments;
    }

    @Override
    public AuditorDepartmentSubDepartmentRequestView getSubDepartmentProjectCounts(Long departmentId) {
        if (departmentId == null) {
            throw new DepartmentApplicationException("Department id is required.");
        }

        List<DepartmentRegistrationEntity> departmentRegistrations = departmentRegistrationRepository
                .findByDepartmentIdOrderByCreatedAtAsc(departmentId);

        if (departmentRegistrations.isEmpty()) {
            throw new DepartmentApplicationException("No registered department found for selected department.");
        }

        Map<Long, Long> projectCountByRegistration = getQueueProjectCountByRegistration();
        Map<Long, Long> projectCountBySubDepartment = new LinkedHashMap<>();

        Set<Long> subDepartmentIds = new LinkedHashSet<>();
        departmentRegistrations.forEach(registration -> {
            Long subDepartmentId = registration.getSubDeptId();
            subDepartmentIds.add(subDepartmentId);

            Long projectCount = projectCountByRegistration.getOrDefault(
                    registration.getDepartmentRegistrationId(),
                    0L);
            projectCountBySubDepartment.put(
                    subDepartmentId,
                    projectCountBySubDepartment.getOrDefault(subDepartmentId, 0L) + projectCount);
        });

        Map<Long, String> subDepartmentNameById = subDepartmentRepository.findAllById(
                subDepartmentIds.stream().filter(Objects::nonNull).toList())
                .stream()
                .collect(Collectors.toMap(
                        SubDepartment::getSubDeptId,
                        SubDepartment::getSubDeptName,
                        (first, second) -> first));

        List<AuditorSubDepartmentProjectCountView> subDepartmentViews = new ArrayList<>();

        if (projectCountBySubDepartment.containsKey(null)) {
            subDepartmentViews.add(AuditorSubDepartmentProjectCountView.builder()
                    .subDepartmentId(null)
                    .subDepartmentName(UNMAPPED_SUB_DEPARTMENT)
                    .projectApplicationCount(projectCountBySubDepartment.get(null))
                    .build());
        }

        subDepartmentIds.stream()
                .filter(Objects::nonNull)
                .forEach(subDepartmentId -> subDepartmentViews.add(AuditorSubDepartmentProjectCountView.builder()
                        .subDepartmentId(subDepartmentId)
                        .subDepartmentName(subDepartmentNameById.getOrDefault(subDepartmentId, "Sub-Department " + subDepartmentId))
                        .projectApplicationCount(projectCountBySubDepartment.getOrDefault(subDepartmentId, 0L))
                        .build()));

        List<AuditorSubDepartmentProjectCountView> sortedSubDepartmentViews = subDepartmentViews.stream()
                .sorted(Comparator.comparing(
                        subDepartment -> safeLower(subDepartment.getSubDepartmentName()),
                        Comparator.nullsLast(String::compareTo)))
                .toList();

        String departmentName = resolveDepartmentName(
                departmentId,
                departmentMstRepository.findById(departmentId).map(DepartmentMst::getDepartmentName).orElse(null),
                departmentRegistrations.get(0).getDepartmentName());

        log.info("Auditor sub-department queue loaded. departmentId={}, subDepartments={}",
                departmentId,
                sortedSubDepartmentViews.size());

        return AuditorDepartmentSubDepartmentRequestView.builder()
                .departmentId(departmentId)
                .departmentName(departmentName)
                .subDepartmentProjectCounts(sortedSubDepartmentViews)
                .build();
    }

    @Override
    public AuditorSubDepartmentApplicationDetailView getSubDepartmentApplications(Long departmentId, Long subDepartmentId) {
        if (departmentId == null) {
            throw new DepartmentApplicationException("Department id is required.");
        }
        if (subDepartmentId == null) {
            throw new DepartmentApplicationException("Sub-department id is required.");
        }

        List<DepartmentRegistrationEntity> departmentRegistrations = departmentRegistrationRepository
                .findByDepartmentIdOrderByCreatedAtAsc(departmentId);

        if (departmentRegistrations.isEmpty()) {
            throw new DepartmentApplicationException("No registered department found for selected department.");
        }

        List<Long> registrationIds = departmentRegistrations.stream()
                .filter(registration -> Objects.equals(registration.getSubDeptId(), subDepartmentId))
                .map(DepartmentRegistrationEntity::getDepartmentRegistrationId)
                .toList();

        if (registrationIds.isEmpty()) {
            throw new DepartmentApplicationException("No registered sub-department found for selected department.");
        }

        List<DepartmentProjectApplicationEntity> applications = departmentProjectApplicationRepository
                .findByDepartmentRegistrationIdInAndApplicationStatusInOrderByDepartmentProjectApplicationIdDesc(
                        registrationIds,
                        AUDITOR_QUEUE_STATUSES);

        List<AuditorDepartmentSubmittedApplicationView> applicationViews = applications.stream()
                .map(this::toSubmittedApplicationView)
                .toList();

        String departmentName = resolveDepartmentName(
                departmentId,
                departmentMstRepository.findById(departmentId).map(DepartmentMst::getDepartmentName).orElse(null),
                departmentRegistrations.get(0).getDepartmentName());

        String subDepartmentName = subDepartmentRepository.findById(subDepartmentId)
                .map(SubDepartment::getSubDeptName)
                .orElse("Sub-Department " + subDepartmentId);

        log.info("Auditor application queue loaded. departmentId={}, subDepartmentId={}, applications={}",
                departmentId,
                subDepartmentId,
                applicationViews.size());

        return AuditorSubDepartmentApplicationDetailView.builder()
                .departmentId(departmentId)
                .departmentName(departmentName)
                .subDepartmentId(subDepartmentId)
                .subDepartmentName(subDepartmentName)
                .applications(applicationViews)
                .build();
    }

    @Override
    public AuditorDepartmentApplicationReviewDetailView getApplicationReviewDetail(
            Long departmentId,
            Long subDepartmentId,
            Long applicationId) {
        DepartmentProjectApplicationEntity application = findApplicationInRegisteredSubDepartment(
                departmentId,
                subDepartmentId,
                applicationId);
        DepartmentRegistrationEntity registration = findRegistrationForApplication(application);

        List<DepartmentRegistrationEntity> registrations = departmentRegistrationRepository
                .findByDepartmentIdOrderByCreatedAtAsc(departmentId);
        String fallbackDepartmentName = registrations.stream()
                .map(DepartmentRegistrationEntity::getDepartmentName)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);

        String departmentName = resolveDepartmentName(
                departmentId,
                departmentMstRepository.findById(departmentId).map(DepartmentMst::getDepartmentName).orElse(null),
                fallbackDepartmentName);

        String subDepartmentName = subDepartmentRepository.findById(subDepartmentId)
                .map(SubDepartment::getSubDeptName)
                .orElse("Sub-Department " + subDepartmentId);

        List<AuditorDepartmentApplicationResourceRequirementView> requirementViews = application.getResourceRequirements()
                .stream()
                .map(this::toRequirementView)
                .toList();

        List<DepartmentProjectApplicationActivityView> activityTimeline = activityRepository
                .findByApplicationDepartmentProjectApplicationIdOrderByActionTimestampDesc(applicationId)
                .stream()
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
        AuditorDepartmentRegistrationDetailView registrationDetail = toRegistrationDetail(registration);
        LocalDate taxApplicableDate = resolveTaxApplicableDate(application);
        BigDecimal baseCost = toScaledAmount(application.getTotalEstimatedCost());
        List<AuditorApplicationTaxComponentView> taxComponents = calculateTaxComponents(baseCost, taxApplicableDate);
        BigDecimal totalTaxAmount = taxComponents.stream()
                .map(AuditorApplicationTaxComponentView::getTaxAmount)
                .reduce(ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalCostIncludingTax = baseCost.add(totalTaxAmount).setScale(2, RoundingMode.HALF_UP);

        return AuditorDepartmentApplicationReviewDetailView.builder()
                .departmentId(departmentId)
                .departmentName(departmentName)
                .subDepartmentId(subDepartmentId)
                .subDepartmentName(subDepartmentName)
                .departmentProjectApplicationId(application.getDepartmentProjectApplicationId())
                .requestId(application.getRequestId())
                .projectName(application.getProjectName())
                .projectCode(application.getProjectCode())
                .applicationType(application.getApplicationType())
                .applicationStatus(application.getApplicationStatus())
                .totalEstimatedCost(application.getTotalEstimatedCost())
                .remarks(application.getRemarks())
                .mahaitContact(application.getMahaitContact())
                .createdDate(application.getCreatedDate())
                .updatedDate(application.getUpdatedDate())
                .workOrderAvailable(StringUtils.hasText(application.getWorkOrderFilePath()))
                .workOrderOriginalName(application.getWorkOrderOriginalName())
                .auditorActionAllowed(isAuditorActionAllowed(application.getApplicationStatus()))
                .completionAllowed(isCompletionAllowed(application.getApplicationStatus()))
                .taxApplicableDate(taxApplicableDate)
                .totalTaxAmount(totalTaxAmount)
                .totalCostIncludingTax(totalCostIncludingTax)
                .taxComponents(taxComponents)
                .registrationDetail(registrationDetail)
                .resourceRequirements(requirementViews)
                .activityTimeline(activityTimeline)
                .build();
    }

    @Override
    @Transactional
    public DepartmentApplicationStatus reviewApplicationByAuditor(
            Long departmentId,
            Long subDepartmentId,
            Long applicationId,
            AuditorReviewDecision decision,
            String remarks,
            String actorEmail) {
        findApplicationInRegisteredSubDepartment(departmentId, subDepartmentId, applicationId);

        if (decision == null) {
            throw new DepartmentApplicationException("Review decision is required.");
        }
        if (isRemarksMandatory(decision) && !StringUtils.hasText(remarks)) {
            throw new DepartmentApplicationException("Remarks are required for send back decision.");
        }

        return manpowerApplicationService.reviewByAuditor(
                applicationId,
                decision,
                remarks,
                actorEmail);
    }

    @Override
    @Transactional
    public DepartmentApplicationStatus completeApplication(
            Long departmentId,
            Long subDepartmentId,
            Long applicationId,
            String remarks,
            String actorEmail) {
        findApplicationInRegisteredSubDepartment(departmentId, subDepartmentId, applicationId);
        return manpowerApplicationService.markCompleted(applicationId, remarks, actorEmail);
    }

    @Override
    public WorkOrderDocumentView getApplicationWorkOrderDocument(
            Long departmentId,
            Long subDepartmentId,
            Long applicationId) {
        DepartmentProjectApplicationEntity application = findApplicationInRegisteredSubDepartment(
                departmentId,
                subDepartmentId,
                applicationId);

        if (!StringUtils.hasText(application.getWorkOrderFilePath())) {
            throw new DepartmentApplicationException("Work-order document is unavailable.");
        }

        return WorkOrderDocumentView.builder()
                .originalFileName(application.getWorkOrderOriginalName())
                .fullPath(application.getWorkOrderFilePath())
                .contentType(application.getWorkOrderFileType())
                .build();
    }

    @Override
    public WorkOrderDocumentView getDepartmentRegistrationDocument(
            Long departmentId,
            Long subDepartmentId,
            Long applicationId,
            DepartmentProfileDocumentType documentType) {
        DepartmentProjectApplicationEntity application = findApplicationInRegisteredSubDepartment(
                departmentId,
                subDepartmentId,
                applicationId);
        DepartmentRegistrationEntity registration = findRegistrationForApplication(application);

        String documentPath = resolveRegistrationDocumentPath(registration, documentType);
        if (!profileDocumentStorageService.isManagedPath(documentPath)) {
            throw new DepartmentApplicationException(documentType.name() + " document is unavailable.");
        }

        return WorkOrderDocumentView.builder()
                .originalFileName(extractFileName(documentPath))
                .fullPath(documentPath)
                .contentType(resolveContentType(documentPath))
                .build();
    }

    private AuditorDepartmentSubmittedApplicationView toSubmittedApplicationView(
            DepartmentProjectApplicationEntity applicationEntity) {
        return AuditorDepartmentSubmittedApplicationView.builder()
                .departmentProjectApplicationId(applicationEntity.getDepartmentProjectApplicationId())
                .requestId(applicationEntity.getRequestId())
                .projectName(applicationEntity.getProjectName())
                .projectCode(applicationEntity.getProjectCode())
                .applicationType(applicationEntity.getApplicationType())
                .applicationStatus(applicationEntity.getApplicationStatus())
                .totalEstimatedCost(applicationEntity.getTotalEstimatedCost())
                .createdDate(applicationEntity.getCreatedDate())
                .updatedDate(applicationEntity.getUpdatedDate())
                .build();
    }

    private AuditorDepartmentApplicationResourceRequirementView toRequirementView(
            DepartmentProjectResourceRequirementEntity requirementEntity) {
        return AuditorDepartmentApplicationResourceRequirementView.builder()
                .designationName(requirementEntity.getDesignationName())
                .levelName(requirementEntity.getLevelName())
                .monthlyRate(requirementEntity.getMonthlyRate())
                .requiredQuantity(requirementEntity.getRequiredQuantity())
                .durationInMonths(requirementEntity.getDurationInMonths())
                .totalCost(requirementEntity.getTotalCost())
                .build();
    }

    private Map<Long, Long> getQueueProjectCountByRegistration() {
        return departmentProjectApplicationRepository.countSubmittedProjectsByDepartmentRegistration(AUDITOR_QUEUE_STATUSES)
                .stream()
                .filter(countProjection -> countProjection.getDepartmentRegistrationId() != null)
                .collect(Collectors.toMap(
                        DepartmentSubmittedProjectCountProjection::getDepartmentRegistrationId,
                        DepartmentSubmittedProjectCountProjection::getProjectCount,
                        Long::sum,
                        LinkedHashMap::new));
    }

    private DepartmentRegistrationEntity findRegistrationForApplication(DepartmentProjectApplicationEntity application) {
        if (application.getDepartmentRegistrationId() == null) {
            throw new DepartmentApplicationException("Department registration is missing for application.");
        }
        return departmentRegistrationRepository.findById(application.getDepartmentRegistrationId())
                .orElseThrow(() -> new DepartmentApplicationException("Department registration not found."));
    }

    private DepartmentProjectApplicationEntity findApplicationInRegisteredSubDepartment(
            Long departmentId,
            Long subDepartmentId,
            Long applicationId) {
        if (departmentId == null) {
            throw new DepartmentApplicationException("Department id is required.");
        }
        if (subDepartmentId == null) {
            throw new DepartmentApplicationException("Sub-department id is required.");
        }
        if (applicationId == null) {
            throw new DepartmentApplicationException("Application id is required.");
        }

        List<DepartmentRegistrationEntity> departmentRegistrations = departmentRegistrationRepository
                .findByDepartmentIdOrderByCreatedAtAsc(departmentId);
        if (departmentRegistrations.isEmpty()) {
            throw new DepartmentApplicationException("No registered department found for selected department.");
        }

        List<Long> registrationIds = departmentRegistrations.stream()
                .filter(registration -> Objects.equals(registration.getSubDeptId(), subDepartmentId))
                .map(DepartmentRegistrationEntity::getDepartmentRegistrationId)
                .toList();
        if (registrationIds.isEmpty()) {
            throw new DepartmentApplicationException("No registered sub-department found for selected department.");
        }

        DepartmentProjectApplicationEntity application = departmentProjectApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new DepartmentApplicationException("Application not found."));

        if (!registrationIds.contains(application.getDepartmentRegistrationId())) {
            throw new DepartmentApplicationException("Application does not belong to selected department and sub-department.");
        }
        return application;
    }

    private boolean isAuditorActionAllowed(DepartmentApplicationStatus currentStatus) {
        return currentStatus == DepartmentApplicationStatus.HR_APPROVED
                || currentStatus == DepartmentApplicationStatus.AUDITOR_REVIEW;
    }

    private boolean isCompletionAllowed(DepartmentApplicationStatus currentStatus) {
        return currentStatus == DepartmentApplicationStatus.AUDITOR_APPROVED;
    }

    private boolean isRemarksMandatory(AuditorReviewDecision decision) {
        return decision == AuditorReviewDecision.SEND_BACK;
    }

    private LocalDate resolveTaxApplicableDate(DepartmentProjectApplicationEntity application) {
        if (application.getCreatedDate() != null) {
            return application.getCreatedDate().toLocalDate();
        }
        return LocalDate.now();
    }

    private BigDecimal toScaledAmount(BigDecimal amount) {
        if (amount == null) {
            return ZERO;
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private List<AuditorApplicationTaxComponentView> calculateTaxComponents(
            BigDecimal baseCost,
            LocalDate applicableDate) {
        List<DepartmentTaxRateMasterEntity> taxRates = taxRateMasterRepository.findApplicableTaxRates(applicableDate);
        if (taxRates.isEmpty()) {
            return List.of();
        }

        return taxRates.stream()
                .filter(taxRate -> taxRate.getRatePercentage() != null && taxRate.getRatePercentage().compareTo(BigDecimal.ZERO) > 0)
                .map(taxRate -> {
                    BigDecimal taxAmount = baseCost
                            .multiply(taxRate.getRatePercentage())
                            .divide(HUNDRED, 2, RoundingMode.HALF_UP);

                    return AuditorApplicationTaxComponentView.builder()
                            .taxCode(taxRate.getTaxCode())
                            .taxName(taxRate.getTaxName())
                            .ratePercentage(taxRate.getRatePercentage())
                            .taxAmount(taxAmount)
                            .build();
                })
                .toList();
    }

    private AuditorDepartmentRegistrationDetailView toRegistrationDetail(DepartmentRegistrationEntity registration) {
        String masterDepartmentName = registration.getDepartmentId() == null
                ? null
                : departmentMstRepository.findById(registration.getDepartmentId())
                        .map(DepartmentMst::getDepartmentName)
                        .orElse(null);
        String registrationDepartmentName = resolveDepartmentName(
                registration.getDepartmentId(),
                masterDepartmentName,
                registration.getDepartmentName());
        String registrationSubDepartmentName = resolveSubDepartmentName(registration.getSubDeptId());

        String gstPath = registration.getGstFilePath();
        String panPath = registration.getPanFilePath();
        String tanPath = registration.getTanFilePath();

        return AuditorDepartmentRegistrationDetailView.builder()
                .departmentName(registrationDepartmentName)
                .subDepartmentName(registrationSubDepartmentName)
                .billingDepartmentName(registration.getBillDepartmentName())
                .billingAddress(registration.getBillAddress())
                .gstNumber(registration.getGstNo())
                .panNumber(registration.getPanNo())
                .tanNumber(registration.getTanNo())
                .gstDocumentName(extractFileName(gstPath))
                .panDocumentName(extractFileName(panPath))
                .tanDocumentName(extractFileName(tanPath))
                .gstDocumentAvailable(profileDocumentStorageService.isManagedPath(gstPath))
                .panDocumentAvailable(profileDocumentStorageService.isManagedPath(panPath))
                .tanDocumentAvailable(profileDocumentStorageService.isManagedPath(tanPath))
                .build();
    }

    private String resolveDepartmentName(Long departmentId, Map<Long, String> departmentNameMasterById,
            Map<Long, String> departmentNameFallbackById) {
        String masterName = departmentNameMasterById.get(departmentId);
        if (StringUtils.hasText(masterName)) {
            return masterName;
        }

        String fallbackName = departmentNameFallbackById.get(departmentId);
        if (StringUtils.hasText(fallbackName)) {
            return fallbackName;
        }

        return "Department " + departmentId;
    }

    private String resolveDepartmentName(Long departmentId, String masterDepartmentName, String fallbackDepartmentName) {
        if (StringUtils.hasText(masterDepartmentName)) {
            return masterDepartmentName;
        }
        if (StringUtils.hasText(fallbackDepartmentName)) {
            return fallbackDepartmentName;
        }
        return "Department " + departmentId;
    }

    private String resolveSubDepartmentName(Long subDepartmentId) {
        if (subDepartmentId == null) {
            return null;
        }
        return subDepartmentRepository.findById(subDepartmentId)
                .map(SubDepartment::getSubDeptName)
                .orElse("Sub-Department " + subDepartmentId);
    }

    private String resolveRegistrationDocumentPath(
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

    private String safeLower(String value) {
        if (value == null) {
            return null;
        }
        return value.toLowerCase(Locale.ROOT);
    }

    private List<AuditorSubDepartmentProjectCountView> resolveSubDepartmentProjectCounts(
            Long departmentId,
            Map<Long, Set<Long>> subDepartmentIdsByDepartment,
            Map<Long, String> subDepartmentNameById,
            Map<Long, Map<Long, Long>> projectCountByDepartmentAndSubDepartment) {
        Map<Long, Long> projectCountBySubDepartment = projectCountByDepartmentAndSubDepartment
                .getOrDefault(departmentId, Map.of());

        List<AuditorSubDepartmentProjectCountView> subDepartmentViews = new ArrayList<>();

        Long unmappedCount = projectCountBySubDepartment.get(null);
        if (unmappedCount != null) {
            subDepartmentViews.add(AuditorSubDepartmentProjectCountView.builder()
                    .subDepartmentId(null)
                    .subDepartmentName(UNMAPPED_SUB_DEPARTMENT)
                    .projectApplicationCount(unmappedCount)
                    .build());
        }

        Set<Long> subDepartmentIds = new LinkedHashSet<>(subDepartmentIdsByDepartment.getOrDefault(
                departmentId,
                Set.of()));
        projectCountBySubDepartment.keySet().stream()
                .filter(Objects::nonNull)
                .forEach(subDepartmentIds::add);

        subDepartmentIds.forEach(subDepartmentId -> subDepartmentViews.add(AuditorSubDepartmentProjectCountView.builder()
                .subDepartmentId(subDepartmentId)
                .subDepartmentName(subDepartmentNameById.getOrDefault(
                        subDepartmentId,
                        "Sub-Department " + subDepartmentId))
                .projectApplicationCount(projectCountBySubDepartment.getOrDefault(subDepartmentId, 0L))
                .build()));

        return subDepartmentViews.stream()
                .sorted(Comparator.comparing(
                        subDepartment -> safeLower(subDepartment.getSubDepartmentName()),
                        Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    private List<String> resolveRegisteredSubDepartments(
            Long departmentId,
            Map<Long, Set<Long>> subDepartmentIdsByDepartment,
            Map<Long, String> subDepartmentNameById) {
        Set<Long> subDepartmentIds = subDepartmentIdsByDepartment.getOrDefault(departmentId, Set.of());
        if (subDepartmentIds.isEmpty()) {
            return List.of();
        }

        return subDepartmentIds.stream()
                .filter(Objects::nonNull)
                .map(subDepartmentId -> subDepartmentNameById.getOrDefault(
                        subDepartmentId,
                        "Sub-Department " + subDepartmentId))
                .filter(StringUtils::hasText)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }
}
