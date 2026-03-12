package com.maharecruitment.gov.in.department.service.impl;

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
import com.maharecruitment.gov.in.department.entity.DepartmentApplicationStatus;
import com.maharecruitment.gov.in.department.entity.DepartmentProjectApplicationEntity;
import com.maharecruitment.gov.in.department.entity.DepartmentProjectResourceRequirementEntity;
import com.maharecruitment.gov.in.department.entity.HrReviewDecision;
import com.maharecruitment.gov.in.department.exception.DepartmentApplicationException;
import com.maharecruitment.gov.in.department.repository.DepartmentProjectApplicationActivityRepository;
import com.maharecruitment.gov.in.department.repository.DepartmentProjectApplicationRepository;
import com.maharecruitment.gov.in.department.repository.projection.DepartmentSubmittedProjectCountProjection;
import com.maharecruitment.gov.in.department.service.DepartmentManpowerApplicationService;
import com.maharecruitment.gov.in.department.service.HrDepartmentRequestService;
import com.maharecruitment.gov.in.department.dto.DepartmentProjectApplicationActivityView;
import com.maharecruitment.gov.in.department.service.model.HrDepartmentApplicationReviewDetailView;
import com.maharecruitment.gov.in.department.service.model.HrDepartmentApplicationResourceRequirementView;
import com.maharecruitment.gov.in.department.service.model.HrDepartmentSubmittedApplicationView;
import com.maharecruitment.gov.in.department.service.model.HrDepartmentSubDepartmentRequestView;
import com.maharecruitment.gov.in.department.service.model.HrParentDepartmentRequestView;
import com.maharecruitment.gov.in.department.service.model.HrSubDepartmentApplicationDetailView;
import com.maharecruitment.gov.in.department.service.model.HrSubDepartmentProjectCountView;
import com.maharecruitment.gov.in.department.service.model.WorkOrderDocumentView;
import com.maharecruitment.gov.in.master.entity.DepartmentMst;
import com.maharecruitment.gov.in.master.entity.SubDepartment;
import com.maharecruitment.gov.in.master.repository.DepartmentMstRepository;
import com.maharecruitment.gov.in.master.repository.SubDepartmentRepository;

@Service
@Transactional(readOnly = true)
public class HrDepartmentRequestServiceImpl implements HrDepartmentRequestService {

    private static final Logger log = LoggerFactory.getLogger(HrDepartmentRequestServiceImpl.class);

    private static final String UNMAPPED_SUB_DEPARTMENT = "Unmapped Sub-Department";

    private static final EnumSet<DepartmentApplicationStatus> SUBMITTED_STATUSES = EnumSet.of(
            DepartmentApplicationStatus.SUBMITTED_TO_HR,
            DepartmentApplicationStatus.CORRECTED_BY_DEPARTMENT);

    private final DepartmentRegistrationRepository departmentRegistrationRepository;
    private final DepartmentProjectApplicationRepository departmentProjectApplicationRepository;
    private final DepartmentProjectApplicationActivityRepository activityRepository;
    private final DepartmentManpowerApplicationService manpowerApplicationService;
    private final DepartmentMstRepository departmentMstRepository;
    private final SubDepartmentRepository subDepartmentRepository;

    public HrDepartmentRequestServiceImpl(
            DepartmentRegistrationRepository departmentRegistrationRepository,
            DepartmentProjectApplicationRepository departmentProjectApplicationRepository,
            DepartmentProjectApplicationActivityRepository activityRepository,
            DepartmentManpowerApplicationService manpowerApplicationService,
            DepartmentMstRepository departmentMstRepository,
            SubDepartmentRepository subDepartmentRepository) {
        this.departmentRegistrationRepository = departmentRegistrationRepository;
        this.departmentProjectApplicationRepository = departmentProjectApplicationRepository;
        this.activityRepository = activityRepository;
        this.manpowerApplicationService = manpowerApplicationService;
        this.departmentMstRepository = departmentMstRepository;
        this.subDepartmentRepository = subDepartmentRepository;
    }

    @Override
    public List<HrParentDepartmentRequestView> getParentDepartmentRequests() {
        List<DepartmentRegistrationEntity> registrations = departmentRegistrationRepository.findAll(
                Sort.by(Sort.Direction.ASC, "departmentName"));

        if (registrations.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> projectCountByRegistration = getSubmittedProjectCountByRegistration();

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

        List<HrParentDepartmentRequestView> parentDepartments = parentDepartmentIds.stream()
                .map(departmentId -> HrParentDepartmentRequestView.builder()
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

        log.info("HR parent department request view loaded. parentDepartments={}", parentDepartments.size());
        return parentDepartments;
    }

    @Override
    public HrDepartmentSubDepartmentRequestView getSubDepartmentProjectCounts(Long departmentId) {
        if (departmentId == null) {
            throw new DepartmentApplicationException("Department id is required.");
        }

        List<DepartmentRegistrationEntity> departmentRegistrations = departmentRegistrationRepository
                .findByDepartmentIdOrderByCreatedAtAsc(departmentId);

        if (departmentRegistrations.isEmpty()) {
            throw new DepartmentApplicationException("No registered department found for selected department.");
        }

        Map<Long, Long> projectCountByRegistration = getSubmittedProjectCountByRegistration();
        Map<Long, Long> projectCountBySubDepartment = new LinkedHashMap<>();

        Set<Long> subDepartmentIds = new LinkedHashSet<>();
        departmentRegistrations.stream()
                .forEach(registration -> {
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

        List<HrSubDepartmentProjectCountView> subDepartmentViews = new ArrayList<>();

        if (projectCountBySubDepartment.containsKey(null)) {
            subDepartmentViews.add(HrSubDepartmentProjectCountView.builder()
                    .subDepartmentId(null)
                    .subDepartmentName(UNMAPPED_SUB_DEPARTMENT)
                    .projectApplicationCount(projectCountBySubDepartment.get(null))
                    .build());
        }

        subDepartmentIds.stream()
                .filter(Objects::nonNull)
                .forEach(subDepartmentId -> subDepartmentViews.add(HrSubDepartmentProjectCountView.builder()
                        .subDepartmentId(subDepartmentId)
                        .subDepartmentName(subDepartmentNameById.getOrDefault(subDepartmentId, "Sub-Department " + subDepartmentId))
                        .projectApplicationCount(projectCountBySubDepartment.getOrDefault(subDepartmentId, 0L))
                        .build()));

        List<HrSubDepartmentProjectCountView> sortedSubDepartmentViews = subDepartmentViews.stream()
                .sorted(Comparator.comparing(
                        subDepartment -> safeLower(subDepartment.getSubDepartmentName()),
                        Comparator.nullsLast(String::compareTo)))
                .toList();

        String departmentName = resolveDepartmentName(
                departmentId,
                departmentMstRepository.findById(departmentId).map(DepartmentMst::getDepartmentName).orElse(null),
                departmentRegistrations.get(0).getDepartmentName());

        log.info("HR sub-department drill loaded. departmentId={}, subDepartments={}",
                departmentId,
                sortedSubDepartmentViews.size());

        return HrDepartmentSubDepartmentRequestView.builder()
                .departmentId(departmentId)
                .departmentName(departmentName)
                .subDepartmentProjectCounts(sortedSubDepartmentViews)
                .build();
    }

    @Override
    public HrSubDepartmentApplicationDetailView getSubDepartmentApplications(Long departmentId, Long subDepartmentId) {
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
                        SUBMITTED_STATUSES);

        List<HrDepartmentSubmittedApplicationView> applicationViews = applications.stream()
                .map(this::toSubmittedApplicationView)
                .toList();

        String departmentName = resolveDepartmentName(
                departmentId,
                departmentMstRepository.findById(departmentId).map(DepartmentMst::getDepartmentName).orElse(null),
                departmentRegistrations.get(0).getDepartmentName());

        String subDepartmentName = subDepartmentRepository.findById(subDepartmentId)
                .map(SubDepartment::getSubDeptName)
                .orElse("Sub-Department " + subDepartmentId);

        log.info("HR application drill loaded. departmentId={}, subDepartmentId={}, applications={}",
                departmentId,
                subDepartmentId,
                applicationViews.size());

        return HrSubDepartmentApplicationDetailView.builder()
                .departmentId(departmentId)
                .departmentName(departmentName)
                .subDepartmentId(subDepartmentId)
                .subDepartmentName(subDepartmentName)
                .applications(applicationViews)
                .build();
    }

    @Override
    public HrDepartmentApplicationReviewDetailView getApplicationReviewDetail(
            Long departmentId,
            Long subDepartmentId,
            Long applicationId) {
        DepartmentProjectApplicationEntity application = findApplicationInRegisteredSubDepartment(
                departmentId,
                subDepartmentId,
                applicationId);

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

        List<HrDepartmentApplicationResourceRequirementView> requirementViews = application.getResourceRequirements()
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

        return HrDepartmentApplicationReviewDetailView.builder()
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
                .hrActionAllowed(isHrActionAllowed(application.getApplicationStatus()))
                .resourceRequirements(requirementViews)
                .activityTimeline(activityTimeline)
                .build();
    }

    @Override
    @Transactional
    public DepartmentApplicationStatus reviewApplicationByHr(
            Long departmentId,
            Long subDepartmentId,
            Long applicationId,
            HrReviewDecision decision,
            String remarks,
            String actorEmail) {
        findApplicationInRegisteredSubDepartment(departmentId, subDepartmentId, applicationId);

        if (decision == null) {
            throw new DepartmentApplicationException("Review decision is required.");
        }
        if (isRemarksMandatory(decision) && !StringUtils.hasText(remarks)) {
            throw new DepartmentApplicationException("Remarks are required for send back and reject decisions.");
        }

        return manpowerApplicationService.reviewByHr(
                applicationId,
                decision,
                remarks,
                actorEmail);
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

    private HrDepartmentSubmittedApplicationView toSubmittedApplicationView(
            DepartmentProjectApplicationEntity applicationEntity) {
        return HrDepartmentSubmittedApplicationView.builder()
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

    private HrDepartmentApplicationResourceRequirementView toRequirementView(
            DepartmentProjectResourceRequirementEntity requirementEntity) {
        return HrDepartmentApplicationResourceRequirementView.builder()
                .designationName(requirementEntity.getDesignationName())
                .levelName(requirementEntity.getLevelName())
                .monthlyRate(requirementEntity.getMonthlyRate())
                .requiredQuantity(requirementEntity.getRequiredQuantity())
                .durationInMonths(requirementEntity.getDurationInMonths())
                .totalCost(requirementEntity.getTotalCost())
                .build();
    }

    private Map<Long, Long> getSubmittedProjectCountByRegistration() {
        return departmentProjectApplicationRepository.countSubmittedProjectsByDepartmentRegistration(SUBMITTED_STATUSES)
                .stream()
                .filter(countProjection -> countProjection.getDepartmentRegistrationId() != null)
                .collect(Collectors.toMap(
                        DepartmentSubmittedProjectCountProjection::getDepartmentRegistrationId,
                        DepartmentSubmittedProjectCountProjection::getProjectCount,
                        (v1, v2) -> (v1 != null ? v1 : 0L) + (v2 != null ? v2 : 0L),                        LinkedHashMap::new));
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

    private boolean isHrActionAllowed(DepartmentApplicationStatus currentStatus) {
        return currentStatus == DepartmentApplicationStatus.SUBMITTED_TO_HR
                || currentStatus == DepartmentApplicationStatus.CORRECTED_BY_DEPARTMENT;
    }

    private boolean isRemarksMandatory(HrReviewDecision decision) {
        return decision == HrReviewDecision.SEND_BACK || decision == HrReviewDecision.REJECT;
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

    private String safeLower(String value) {
        if (value == null) {
            return null;
        }
        return value.toLowerCase(Locale.ROOT);
    }

    private List<HrSubDepartmentProjectCountView> resolveSubDepartmentProjectCounts(
            Long departmentId,
            Map<Long, Set<Long>> subDepartmentIdsByDepartment,
            Map<Long, String> subDepartmentNameById,
            Map<Long, Map<Long, Long>> projectCountByDepartmentAndSubDepartment) {
        Map<Long, Long> projectCountBySubDepartment = projectCountByDepartmentAndSubDepartment
                .getOrDefault(departmentId, Map.of());

        List<HrSubDepartmentProjectCountView> subDepartmentViews = new ArrayList<>();

        Long unmappedCount = projectCountBySubDepartment.get(null);
        if (unmappedCount != null) {
            subDepartmentViews.add(HrSubDepartmentProjectCountView.builder()
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

        subDepartmentIds.forEach(subDepartmentId -> subDepartmentViews.add(HrSubDepartmentProjectCountView.builder()
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
