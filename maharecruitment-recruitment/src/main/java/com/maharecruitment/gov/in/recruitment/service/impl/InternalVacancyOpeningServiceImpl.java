package com.maharecruitment.gov.in.recruitment.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.maharecruitment.gov.in.auth.entity.Role;
import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.RoleRepository;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.master.dto.ManpowerDesignationMasterResponse;
import com.maharecruitment.gov.in.master.dto.ManpowerDesignationRateResponse;
import com.maharecruitment.gov.in.master.dto.ResourceLevelRefResponse;
import com.maharecruitment.gov.in.master.entity.ManpowerDesignationMaster;
import com.maharecruitment.gov.in.master.entity.ProjectMst;
import com.maharecruitment.gov.in.master.entity.ProjectScopeType;
import com.maharecruitment.gov.in.master.entity.ResourceLevelExperience;
import com.maharecruitment.gov.in.master.repository.ManpowerDesignationMasterRepository;
import com.maharecruitment.gov.in.master.repository.ProjectMstRepository;
import com.maharecruitment.gov.in.master.service.ManpowerDesignationMasterService;
import com.maharecruitment.gov.in.master.service.ManpowerDesignationRateService;
import com.maharecruitment.gov.in.recruitment.dto.hr.InternalVacancyOpeningForm;
import com.maharecruitment.gov.in.recruitment.dto.hr.InternalVacancyRequirementForm;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.entity.InternalVacancyInterviewAuthorityEntity;
import com.maharecruitment.gov.in.recruitment.entity.InternalVacancyInterviewEmployeeEntity;
import com.maharecruitment.gov.in.recruitment.entity.InternalVacancyInterviewRoleEntity;
import com.maharecruitment.gov.in.recruitment.entity.InternalVacancyHiringRequestType;
import com.maharecruitment.gov.in.recruitment.entity.InternalVacancyOpeningEntity;
import com.maharecruitment.gov.in.recruitment.entity.InternalVacancyOpeningRequirementEntity;
import com.maharecruitment.gov.in.recruitment.entity.InternalVacancyOpeningStatus;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.recruitment.repository.InternalVacancyOpeningRepository;
import com.maharecruitment.gov.in.recruitment.repository.projection.InternalVacancyOpeningStatusCountProjection;
import com.maharecruitment.gov.in.recruitment.service.InternalVacancyOpeningService;
import com.maharecruitment.gov.in.recruitment.service.InternalVacancyApprovalDocumentStorageService;
import com.maharecruitment.gov.in.recruitment.service.RecruitmentNotificationService;
import com.maharecruitment.gov.in.recruitment.service.RecruitmentRequestIdGenerator;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyInterviewAuthorityRoleOptionView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyApprovalDocumentView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyInterviewAuthorityUserOptionView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyInterviewEmployeeOptionView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyOpeningListMetricsView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyOpeningDetailsView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalProjectOptionView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyOpeningCommand;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyOpeningLevelOptionView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyOpeningResult;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyOpeningSummaryView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyRequirementCommand;
import com.maharecruitment.gov.in.recruitment.service.model.StoredInternalVacancyApprovalDocument;

@Service
@Transactional(readOnly = true)
public class InternalVacancyOpeningServiceImpl implements InternalVacancyOpeningService {

    private static final Logger log = LoggerFactory.getLogger(InternalVacancyOpeningServiceImpl.class);
    private static final String INTERNAL_REQUEST_TYPE = "I";
    private static final Long EMPLOYEE_ROLE_ID = -100L;
    private static final List<String> ALLOWED_INTERVIEW_AUTHORITY_ROLE_NAMES = java.util.Collections.unmodifiableList(
            java.util.Arrays.asList("ROLE_HOD", "ROLE_PM", "ROLE_STM")
    );

    private final InternalVacancyOpeningRepository internalVacancyOpeningRepository;
    private final ProjectMstRepository projectRepository;
    private final ManpowerDesignationMasterRepository designationRepository;
    private final ManpowerDesignationMasterService designationService;
    private final ManpowerDesignationRateService designationRateService;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final RecruitmentRequestIdGenerator recruitmentRequestIdGenerator;
    private final RecruitmentNotificationService recruitmentNotificationService;
    private final InternalVacancyApprovalDocumentStorageService approvalDocumentStorageService;

    public InternalVacancyOpeningServiceImpl(
            InternalVacancyOpeningRepository internalVacancyOpeningRepository,
            ProjectMstRepository projectRepository,
            ManpowerDesignationMasterRepository designationRepository,
            ManpowerDesignationMasterService designationService,
            ManpowerDesignationRateService designationRateService,
            RoleRepository roleRepository,
            UserRepository userRepository,
            EmployeeRepository employeeRepository,
            RecruitmentRequestIdGenerator recruitmentRequestIdGenerator,
            RecruitmentNotificationService recruitmentNotificationService,
            InternalVacancyApprovalDocumentStorageService approvalDocumentStorageService) {
        this.internalVacancyOpeningRepository = internalVacancyOpeningRepository;
        this.projectRepository = projectRepository;
        this.designationRepository = designationRepository;
        this.designationService = designationService;
        this.designationRateService = designationRateService;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.recruitmentRequestIdGenerator = recruitmentRequestIdGenerator;
        this.recruitmentNotificationService = recruitmentNotificationService;
        this.approvalDocumentStorageService = approvalDocumentStorageService;
    }

    @Override
    @Transactional
    public InternalVacancyOpeningResult saveOpening(InternalVacancyOpeningCommand command) {
        validateCommand(command);

        String actorEmail = normalizeActorEmail(command.getActorEmail());
        ProjectMst project = findInternalProject(command.getProjectId());
        InternalVacancyOpeningStatus targetStatus = resolveTargetStatus(command.getTargetStatus());
        boolean isEdit = command.getInternalVacancyOpeningId() != null;
        InternalVacancyOpeningEntity entity = isEdit
                ? findOpeningForUpdate(command.getInternalVacancyOpeningId())
                : new InternalVacancyOpeningEntity();
        validateSaveTransition(entity, targetStatus);
        InternalVacancyHiringRequestType hiringRequestType = resolveHiringRequestType(command.getHiringRequestType());
        List<EmployeeEntity> replacementEmployees = resolveReplacementEmployees(
                command,
                hiringRequestType,
                targetStatus);
        validateHiringRequestDetails(command, entity, hiringRequestType, targetStatus);

        List<InternalVacancyRequirementCommand> effectiveRequirements =
                hiringRequestType == InternalVacancyHiringRequestType.EMPLOYEE_REPLACEMENT
                        ? buildReplacementRequirements(replacementEmployees)
                        : command.getRequirements();
        if (targetStatus != InternalVacancyOpeningStatus.DRAFT
                && (effectiveRequirements == null || effectiveRequirements.isEmpty())) {
            throw new RecruitmentNotificationException(
                    "At least one designation requirement is required to submit.");
        }

        List<InternalVacancyRequirementCommand> safeRequirements = effectiveRequirements == null
                ? List.of()
                : effectiveRequirements;
        Map<Long, ManpowerDesignationMaster> designationById = safeRequirements.isEmpty()
                ? Map.of()
                : resolveDesignations(safeRequirements);
        List<InternalVacancyOpeningRequirementEntity> requirementEntities = buildRequirementEntities(
                safeRequirements,
                designationById);
        List<Role> interviewRoles = List.of();
        List<User> interviewAuthorities = List.of();

        if (targetStatus == InternalVacancyOpeningStatus.OPEN) {
            interviewRoles = resolveInterviewRoles(command.getInterviewAuthorityRoleIds());
            interviewAuthorities = resolveInterviewAuthorities(
                    command.getInterviewAuthorityUserIds(),
                    interviewRoles);
        } else {
            // For DRAFT or PENDING_HR_APPROVAL, resolve if provided but don't force mandatory checks
            List<Long> roleIds = normalizePositiveIds(command.getInterviewAuthorityRoleIds());
            if (!roleIds.isEmpty()) {
                interviewRoles = findAllowedInterviewAuthorityRoles(roleIds);
                List<Long> userIds = normalizePositiveIds(command.getInterviewAuthorityUserIds());
                if (!userIds.isEmpty() && !interviewRoles.isEmpty()) {
                    interviewAuthorities = userRepository.findAllWithRolesByIdIn(userIds);
                }
            }
        }

        List<EmployeeEntity> interviewEmployees = resolveInterviewEmployees(
                command.getInterviewAuthorityEmployeeIds(),
                targetStatus == InternalVacancyOpeningStatus.OPEN);

        List<InternalVacancyInterviewRoleEntity> interviewRoleEntities = buildInterviewRoleEntities(interviewRoles);
        List<InternalVacancyInterviewAuthorityEntity> interviewAuthorityEntities = buildInterviewAuthorityEntities(
                interviewAuthorities);
        List<InternalVacancyInterviewEmployeeEntity> interviewEmployeeEntities = buildInterviewEmployeeEntities(
                interviewEmployees);

        String previousApprovalFilePath = entity.getEOfficeApprovalFilePath();
        StoredInternalVacancyApprovalDocument uploadedApproval = null;
        if (hiringRequestType == InternalVacancyHiringRequestType.NEW_CANDIDATE
                && hasUploadedFile(command.getEOfficeApprovalDocument())) {
            uploadedApproval = approvalDocumentStorageService.store(command.getEOfficeApprovalDocument());
        }

        if (!isEdit) {
            entity.setRequestId(recruitmentRequestIdGenerator.generate(INTERNAL_REQUEST_TYPE));
            entity.setCreatedByEmail(actorEmail);
        }

        entity.setProjectMst(project);
        entity.setHiringRequestType(hiringRequestType);
        applyHiringRequestDetails(entity, hiringRequestType, uploadedApproval);
        entity.setStatus(targetStatus);
        entity.setRemarks(normalizeOptionalText(command.getRemarks()));
        entity.setUpdatedByEmail(actorEmail);
        replaceChildCollections(
                entity,
                requirementEntities,
                interviewRoleEntities,
                interviewAuthorityEntities,
                interviewEmployeeEntities,
                replacementEmployees,
                isEdit);

        InternalVacancyOpeningEntity saved;
        try {
            saved = internalVacancyOpeningRepository.saveAndFlush(entity);
            if (targetStatus == InternalVacancyOpeningStatus.OPEN) {
                recruitmentNotificationService.upsertFromInternalVacancyOpening(saved.getInternalVacancyOpeningId());
            }
        } catch (RuntimeException ex) {
            if (uploadedApproval != null) {
                approvalDocumentStorageService.removeManagedFileQuietly(uploadedApproval.getFullPath());
            }
            throw ex;
        }
        if (StringUtils.hasText(previousApprovalFilePath)
                && !previousApprovalFilePath.equals(saved.getEOfficeApprovalFilePath())) {
            approvalDocumentStorageService.removeManagedFileQuietly(previousApprovalFilePath);
        }
        long totalVacancies = saved.getRequirements().stream()
                .map(InternalVacancyOpeningRequirementEntity::getNumberOfVacancy)
                .filter(value -> value != null && value > 0)
                .mapToLong(Long::longValue)
                .sum();

        log.info(
                "Internal vacancy opening saved. operation={}, requestId={}, openingId={}, status={}, projectId={}, projectName={}, designationCount={}, totalVacancies={}, interviewRoleCount={}, interviewAuthorityCount={}, actor={}",
                isEdit ? "UPDATE" : "CREATE",
                saved.getRequestId(),
                saved.getInternalVacancyOpeningId(),
                saved.getStatus(),
                project.getProjectId(),
                project.getProjectName(),
                saved.getRequirements().size(),
                totalVacancies,
                saved.getInterviewRoles().size(),
                saved.getInterviewAuthorities().size(),
                saved.getInterviewEmployees().size(),
                actorEmail);

        return InternalVacancyOpeningResult.builder()
                .internalVacancyOpeningId(saved.getInternalVacancyOpeningId())
                .requestId(saved.getRequestId())
                .build();
    }

    @Override
    @Transactional
    public InternalVacancyOpeningResult changeOpeningStatus(
            Long internalVacancyOpeningId,
            String actorEmail,
            InternalVacancyOpeningStatus targetStatus) {
        String normalizedActorEmail = normalizeActorEmail(actorEmail);
        InternalVacancyOpeningEntity entity = findOpeningForStatusChange(internalVacancyOpeningId);
        validateStatusChangeTransition(entity, targetStatus);

        entity.setStatus(targetStatus);
        entity.setUpdatedByEmail(normalizedActorEmail);
        InternalVacancyOpeningEntity saved = internalVacancyOpeningRepository.saveAndFlush(entity);

        if (targetStatus == InternalVacancyOpeningStatus.OPEN) {
            recruitmentNotificationService.upsertFromInternalVacancyOpening(saved.getInternalVacancyOpeningId());
        } else if (targetStatus == InternalVacancyOpeningStatus.CLOSED) {
            recruitmentNotificationService.closeFromInternalVacancyOpening(saved.getInternalVacancyOpeningId());
        }

        log.info(
                "Internal vacancy opening status changed. requestId={}, openingId={}, status={}, actor={}",
                saved.getRequestId(),
                saved.getInternalVacancyOpeningId(),
                saved.getStatus(),
                normalizedActorEmail);

        return InternalVacancyOpeningResult.builder()
                .internalVacancyOpeningId(saved.getInternalVacancyOpeningId())
                .requestId(saved.getRequestId())
                .build();
    }

    @Override
    public Page<InternalVacancyOpeningSummaryView> getOpeningPage(
            String searchText,
            String actorEmail,
            List<InternalVacancyOpeningStatus> excludedStatuses,
            Pageable pageable) {
        List<InternalVacancyOpeningStatus> safeExcluded = (excludedStatuses == null || excludedStatuses.isEmpty()) ? null : excludedStatuses;
        return internalVacancyOpeningRepository.findPageBySearchWithFilters(
                buildSearchPattern(searchText),
                actorEmail,
                safeExcluded,
                pageable)
                .map(this::toSummaryView);
    }

    @Override
    public InternalVacancyOpeningListMetricsView getOpeningListMetrics(
            String searchText,
            String actorEmail,
            List<InternalVacancyOpeningStatus> excludedStatuses) {
        List<InternalVacancyOpeningStatus> safeExcluded = (excludedStatuses == null || excludedStatuses.isEmpty()) ? null : excludedStatuses;
        List<InternalVacancyOpeningStatusCountProjection> counts = internalVacancyOpeningRepository
                .summarizeStatusCountsWithFilters(buildSearchPattern(searchText), actorEmail, safeExcluded);

        Map<InternalVacancyOpeningStatus, Long> countByStatus = new LinkedHashMap<>();
        long totalOpenings = 0L;
        for (InternalVacancyOpeningStatusCountProjection count : counts) {
            if (count == null || count.getStatus() == null) {
                continue;
            }

            long safeCount = count.getTotalCount() == null ? 0L : count.getTotalCount();
            countByStatus.put(count.getStatus(), safeCount);
            totalOpenings += safeCount;
        }

        return InternalVacancyOpeningListMetricsView.builder()
                .totalOpenings(totalOpenings)
                .draftOpenings(countByStatus.getOrDefault(InternalVacancyOpeningStatus.DRAFT, 0L))
                .pendingRequests(countByStatus.getOrDefault(InternalVacancyOpeningStatus.PENDING_HR_APPROVAL, 0L))
                .activeOpenings(countByStatus.getOrDefault(InternalVacancyOpeningStatus.OPEN, 0L))
                .rejectedRequests(countByStatus.getOrDefault(InternalVacancyOpeningStatus.REJECTED_BY_HR, 0L))
                .closedOpenings(countByStatus.getOrDefault(InternalVacancyOpeningStatus.CLOSED, 0L))
                .build();
    }

    @Override
    public InternalVacancyOpeningForm getOpeningForEdit(Long internalVacancyOpeningId) {
        InternalVacancyOpeningEntity entity = findOpeningForFormEdit(internalVacancyOpeningId);

        InternalVacancyOpeningForm form = new InternalVacancyOpeningForm();
        form.setInternalVacancyOpeningId(entity.getInternalVacancyOpeningId());
        form.setCurrentStatus(entity.getStatus());
        form.setProjectId(entity.getProjectMst().getProjectId());
        form.setHiringRequestType(entity.getHiringRequestType());
        form.setReplacementEmployeeIds(entity.getReplacementEmployees().stream()
                .map(EmployeeEntity::getEmployeeId)
                .filter(java.util.Objects::nonNull)
                .toList());
        form.setExistingEOfficeApprovalFileName(entity.getEOfficeApprovalFileName());
        form.setRemarks(entity.getRemarks());
        form.setRequirements(entity.getRequirements().stream()
                .map(this::toRequirementForm)
                .toList());
        form.setInterviewAuthorityRoleIds(resolveEditInterviewAuthorityRoleIds(entity));
        form.setInterviewAuthorityUserIds(entity.getInterviewAuthorities().stream()
                .map(InternalVacancyInterviewAuthorityEntity::getUser)
                .filter(user -> user != null && user.getId() != null)
                .map(User::getId)
                .distinct()
                .toList());
        form.setInterviewAuthorityEmployeeIds(entity.getInterviewEmployees().stream()
                .map(InternalVacancyInterviewEmployeeEntity::getEmployee)
                .filter(employee -> employee != null && employee.getEmployeeId() != null)
                .map(EmployeeEntity::getEmployeeId)
                .distinct()
                .toList());
        return form;
    }

    @Override
    public InternalVacancyOpeningDetailsView getOpeningDetailsForOwner(
            Long internalVacancyOpeningId,
            String actorEmail) {
        String normalizedActorEmail = normalizeActorEmail(actorEmail);
        InternalVacancyOpeningEntity entity = internalVacancyOpeningRepository
                .findDetailedByInternalVacancyOpeningIdAndCreatedByEmailIgnoreCase(
                        internalVacancyOpeningId,
                        normalizedActorEmail)
                .orElseThrow(() -> new RecruitmentNotificationException(
                        "Internal vacancy application is unavailable."));

        List<InternalVacancyRequirementForm> requirements = entity.getRequirements().stream()
                .map(this::toRequirementForm)
                .toList();
        long totalVacancies = requirements.stream()
                .map(InternalVacancyRequirementForm::getNumberOfVacancy)
                .filter(value -> value != null && value > 0)
                .mapToLong(Long::longValue)
                .sum();

        return InternalVacancyOpeningDetailsView.builder()
                .internalVacancyOpeningId(entity.getInternalVacancyOpeningId())
                .requestId(entity.getRequestId())
                .projectName(entity.getProjectMst().getProjectName())
                .hiringRequestType(entity.getHiringRequestType())
                .replacementEmployeeLabels(entity.getReplacementEmployees().stream()
                        .map(this::buildReplacementEmployeeLabel)
                        .toList())
                .eOfficeApprovalFileName(entity.getEOfficeApprovalFileName())
                .remarks(entity.getRemarks())
                .requirements(requirements)
                .totalVacancies(totalVacancies)
                .status(entity.getStatus())
                .createdDateTime(entity.getCreatedDateTime())
                .updatedDateTime(entity.getUpdatedDateTime())
                .build();
    }

    @Override
    public InternalVacancyApprovalDocumentView getApprovalDocument(Long internalVacancyOpeningId) {
        return toApprovalDocumentView(findOpeningById(internalVacancyOpeningId));
    }

    @Override
    public InternalVacancyApprovalDocumentView getApprovalDocumentForOwner(
            Long internalVacancyOpeningId,
            String actorEmail) {
        String normalizedActorEmail = normalizeActorEmail(actorEmail);
        InternalVacancyOpeningEntity entity = findOpeningById(internalVacancyOpeningId);
        if (!normalizedActorEmail.equalsIgnoreCase(entity.getCreatedByEmail())) {
            throw new RecruitmentNotificationException("E-office approval document is unavailable.");
        }
        return toApprovalDocumentView(entity);
    }

    @Override
    public List<InternalProjectOptionView> getAvailableInternalProjects() {
        return projectRepository.findByProjectScopeTypeOrderByProjectNameAsc(ProjectScopeType.INTERNAL)
                .stream()
                .map(project -> InternalProjectOptionView.builder()
                        .projectId(project.getProjectId())
                        .projectName(project.getProjectName())
                        .build())
                .toList();
    }

    @Override
    public List<ManpowerDesignationMasterResponse> getAvailableDesignations() {
        return designationService.getAll(false, Pageable.unpaged()).getContent();
    }

    @Override
    public List<InternalVacancyOpeningLevelOptionView> getLevelsByDesignation(Long designationId) {
        if (designationId == null) {
            return List.of();
        }

        ManpowerDesignationMasterResponse designation = designationService.getById(designationId, false);
        if (designation.getLevels() == null || designation.getLevels().isEmpty()) {
            return List.of();
        }

        return designation.getLevels().stream()
                .sorted(Comparator.comparing(ResourceLevelRefResponse::getLevelName, String.CASE_INSENSITIVE_ORDER))
                .map(level -> InternalVacancyOpeningLevelOptionView.builder()
                        .levelCode(level.getLevelCode())
                        .levelName(level.getLevelName())
                        .build())
                .toList();
    }

    @Override
    public List<InternalVacancyInterviewAuthorityRoleOptionView> getAvailableInterviewAuthorityRoles() {
        List<InternalVacancyInterviewAuthorityRoleOptionView> roles = new ArrayList<>(getAllowedInterviewAuthorityRoles().stream()
                .map(role -> InternalVacancyInterviewAuthorityRoleOptionView.builder()
                        .roleId(role.getId())
                        .roleName(role.getName())
                        .roleLabel(toRoleLabel(role.getName()))
                        .build())
                .toList());

        roles.add(InternalVacancyInterviewAuthorityRoleOptionView.builder()
                .roleId(EMPLOYEE_ROLE_ID)
                .roleName("EMPLOYEE")
                .roleLabel("Employee")
                .build());

        return roles;
    }

    @Override
    public List<InternalVacancyInterviewAuthorityUserOptionView> getAvailableInterviewAuthorities(List<Long> roleIds) {
        return getAvailableInterviewAuthoritiesPage(roleIds, null, Pageable.unpaged()).getContent();
    }

    @Override
    public Page<InternalVacancyInterviewAuthorityUserOptionView> getAvailableInterviewAuthoritiesPage(
            List<Long> roleIds,
            String search,
            Pageable pageable) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Page.empty(pageable);
        }

        List<Long> actualRoleIds = roleIds.stream()
                .filter(id -> id != null && !id.equals(EMPLOYEE_ROLE_ID))
                .toList();

        boolean includeEmployees = roleIds.contains(EMPLOYEE_ROLE_ID);
        String searchPattern = StringUtils.hasText(search) ? "%" + search.trim().toLowerCase() + "%" : null;

        List<InternalVacancyInterviewAuthorityUserOptionView> combined = new ArrayList<>();
        long totalElements = 0;

        // Note: For true database pagination across two different entities/tables, 
        // a UNION query or a common database view would be ideal.
        // Here we handle them based on the selected roles.
        
        if (!actualRoleIds.isEmpty() && !includeEmployees) {
            // Only Users
            Page<User> userPage = userRepository.findByRolesAndSearch(actualRoleIds, searchPattern, pageable);
            return userPage.map(user -> InternalVacancyInterviewAuthorityUserOptionView.builder()
                    .userId(user.getId())
                    .name(user.getName())
                    .email(user.getEmail())
                    .mobileNo(user.getMobileNo())
                    .displayLabel(buildInterviewAuthorityLabel(user))
                    .type("USER")
                    .build());
        } else if (actualRoleIds.isEmpty() && includeEmployees) {
            // Only Employees
            Page<EmployeeEntity> employeePage = employeeRepository.findActiveWithSearch(searchPattern, pageable);
            return employeePage.map(employee -> InternalVacancyInterviewAuthorityUserOptionView.builder()
                    .userId(employee.getEmployeeId())
                    .name(employee.getFullName())
                    .email(employee.getEmail())
                    .mobileNo(employee.getMobile())
                    .displayLabel(buildInterviewEmployeeLabel(employee))
                    .type("EMPLOYEE")
                    .build());
        } else {
            // Both selected - Combine results. 
            // For simplicity in this mixed-role scenario, we fetch all users and then paginate.
            // If this becomes a performance bottleneck, we recommend restricting searches to one type at a time.
            List<InternalVacancyInterviewAuthorityUserOptionView> allResults = new ArrayList<>();
            
            allResults.addAll(userRepository.findByRolesAndSearch(actualRoleIds, searchPattern, Pageable.unpaged())
                    .map(user -> InternalVacancyInterviewAuthorityUserOptionView.builder()
                            .userId(user.getId())
                            .name(user.getName())
                            .email(user.getEmail())
                            .mobileNo(user.getMobileNo())
                            .displayLabel(buildInterviewAuthorityLabel(user))
                            .type("USER")
                            .build()).getContent());
            
            allResults.addAll(employeeRepository.findActiveWithSearch(searchPattern, Pageable.unpaged())
                    .map(employee -> InternalVacancyInterviewAuthorityUserOptionView.builder()
                            .userId(employee.getEmployeeId())
                            .name(employee.getFullName())
                            .email(employee.getEmail())
                            .mobileNo(employee.getMobile())
                            .displayLabel(buildInterviewEmployeeLabel(employee))
                            .type("EMPLOYEE")
                            .build()).getContent());

            if (pageable.isUnpaged()) {
                return new org.springframework.data.domain.PageImpl<>(allResults);
            }

            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), allResults.size());
            
            List<InternalVacancyInterviewAuthorityUserOptionView> pagedList = (start < allResults.size()) 
                    ? allResults.subList(start, end) 
                    : List.of();
                    
            return new org.springframework.data.domain.PageImpl<>(pagedList, pageable, allResults.size());
        }
    }

    @Override
    public List<InternalVacancyInterviewEmployeeOptionView> getAvailableInterviewEmployees() {
        return employeeRepository.findActiveEmployeeOptions("ACTIVE")
                .stream()
                .map(employee -> InternalVacancyInterviewEmployeeOptionView.builder()
                        .employeeId(employee.getEmployeeId())
                        .fullName(employee.getFullName())
                        .employeeCode(employee.getEmployeeCode())
                        .email(employee.getEmail())
                        .mobile(employee.getMobile())
                        .designationId(employee.getDesignation() != null
                                ? employee.getDesignation().getDesignationId()
                                : null)
                        .designationName(employee.getDesignation() != null ? employee.getDesignation().getDesignationName() : null)
                        .levelCode(normalizeOptionalText(employee.getLevelCode()))
                        .levelName(resolveEmployeeLevelName(employee))
                        .displayLabel(buildInterviewEmployeeLabel(employee))
                        .build())
                .toList();
    }

    @Override
    public List<InternalVacancyInterviewEmployeeOptionView> getAvailableReplacementEmployees() {
        return employeeRepository.findActiveEmployeeOptions("ACTIVE").stream()
                .filter(this::hasReplacementDesignationAndLevel)
                .map(employee -> InternalVacancyInterviewEmployeeOptionView.builder()
                        .employeeId(employee.getEmployeeId())
                        .fullName(employee.getFullName())
                        .email(employee.getEmail())
                        .mobile(employee.getMobile())
                        .designationId(employee.getDesignation().getDesignationId())
                        .designationName(employee.getDesignation().getDesignationName())
                        .levelCode(employee.getLevelCode().trim().toUpperCase(Locale.ROOT))
                        .levelName(resolveEmployeeLevelName(employee))
                        .displayLabel(buildReplacementEmployeeLabel(employee))
                        .build())
                .toList();
    }

    private boolean hasReplacementDesignationAndLevel(EmployeeEntity employee) {
        if (employee.getDesignation() == null
                || employee.getDesignation().getDesignationId() == null
                || !StringUtils.hasText(employee.getLevelCode())) {
            return false;
        }
        return employee.getDesignation().getLevels().stream()
                .anyMatch(level -> level.getLevelCode() != null
                        && level.getLevelCode().equalsIgnoreCase(employee.getLevelCode().trim()));
    }

    private String resolveEmployeeLevelName(EmployeeEntity employee) {
        if (employee.getDesignation() == null || !StringUtils.hasText(employee.getLevelCode())) {
            return null;
        }
        return employee.getDesignation().getLevels().stream()
                .filter(level -> level.getLevelCode() != null
                        && level.getLevelCode().equalsIgnoreCase(employee.getLevelCode().trim()))
                .map(ResourceLevelExperience::getLevelName)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(employee.getLevelCode().trim().toUpperCase(Locale.ROOT));
    }

    private BigDecimal getMonthlyRate(Long designationId, String levelCode) {
        if (designationId == null || !StringUtils.hasText(levelCode)) {
            throw new RecruitmentNotificationException("Designation and level are required.");
        }

        LocalDate today = LocalDate.now();
        List<ManpowerDesignationRateResponse> rates = designationRateService
                .getAll(designationId, false, Pageable.unpaged())
                .getContent();

        return rates.stream()
                .filter(rate -> rate.getLevelCode() != null
                        && rate.getLevelCode().equalsIgnoreCase(levelCode.trim()))
                .filter(rate -> rate.getEffectiveFrom() != null && !rate.getEffectiveFrom().isAfter(today))
                .filter(rate -> rate.getEffectiveTo() == null || !rate.getEffectiveTo().isBefore(today))
                .max(Comparator.comparing(ManpowerDesignationRateResponse::getEffectiveFrom))
                .map(ManpowerDesignationRateResponse::getGrossMonthlyCtc)
                .orElseThrow(() -> new RecruitmentNotificationException(
                        "No active rate found for the selected designation and level."));
    }

    private void validateCommand(InternalVacancyOpeningCommand command) {
        if (command == null) {
            throw new RecruitmentNotificationException("Internal vacancy opening request is required.");
        }
        if (command.getTargetStatus() == null) {
            throw new RecruitmentNotificationException("Vacancy opening action is required.");
        }
        if (command.getProjectId() == null) {
            throw new RecruitmentNotificationException("Project is required.");
        }
        if (!StringUtils.hasText(command.getActorEmail())) {
            throw new RecruitmentNotificationException("Authenticated user is required.");
        }
    }

    private InternalVacancyHiringRequestType resolveHiringRequestType(
            InternalVacancyHiringRequestType hiringRequestType) {
        if (hiringRequestType == null) {
            throw new RecruitmentNotificationException("Hiring request type is required.");
        }
        return hiringRequestType;
    }

    private void validateHiringRequestDetails(
            InternalVacancyOpeningCommand command,
            InternalVacancyOpeningEntity entity,
            InternalVacancyHiringRequestType hiringRequestType,
            InternalVacancyOpeningStatus targetStatus) {
        boolean submission = targetStatus == InternalVacancyOpeningStatus.PENDING_HR_APPROVAL
                || targetStatus == InternalVacancyOpeningStatus.OPEN;

        if (hiringRequestType == InternalVacancyHiringRequestType.NEW_CANDIDATE) {
            boolean approvalAvailable = hasUploadedFile(command.getEOfficeApprovalDocument())
                    || StringUtils.hasText(entity.getEOfficeApprovalFilePath());
            if (submission && !approvalAvailable) {
                throw new RecruitmentNotificationException(
                        "E-office approval document is required for a new candidate request.");
            }
        }
    }

    private List<EmployeeEntity> resolveReplacementEmployees(
            InternalVacancyOpeningCommand command,
            InternalVacancyHiringRequestType hiringRequestType,
            InternalVacancyOpeningStatus targetStatus) {
        if (hiringRequestType != InternalVacancyHiringRequestType.EMPLOYEE_REPLACEMENT) {
            return List.of();
        }

        List<Long> employeeIds = normalizePositiveIds(command.getReplacementEmployeeIds()).stream()
                .filter(id -> id > 0)
                .toList();
        boolean submission = targetStatus == InternalVacancyOpeningStatus.PENDING_HR_APPROVAL
                || targetStatus == InternalVacancyOpeningStatus.OPEN;
        if (employeeIds.isEmpty()) {
            if (submission) {
                throw new RecruitmentNotificationException(
                        "Select at least one employee to be replaced.");
            }
            return List.of();
        }

        List<EmployeeEntity> employees = employeeRepository.findReplacementEmployeesByEmployeeIdIn(employeeIds);
        Map<Long, EmployeeEntity> employeeById = employees.stream()
                .filter(employee -> employee.getEmployeeId() != null)
                .collect(Collectors.toMap(
                        EmployeeEntity::getEmployeeId,
                        employee -> employee,
                        (left, right) -> left,
                        LinkedHashMap::new));
        if (employeeById.size() != employeeIds.size()) {
            throw new RecruitmentNotificationException(
                    "One or more selected replacement employees do not exist.");
        }

        return employeeIds.stream()
                .map(employeeById::get)
                .peek(this::validateReplacementEmployee)
                .toList();
    }

    private void validateReplacementEmployee(EmployeeEntity employee) {
        if (!"ACTIVE".equalsIgnoreCase(employee.getStatus())) {
            throw new RecruitmentNotificationException(
                    "Selected replacement employees must be active.");
        }
        if (employee.getDesignation() == null || employee.getDesignation().getDesignationId() == null) {
            throw new RecruitmentNotificationException(
                    "Every selected replacement employee must have a designation.");
        }
        if (!StringUtils.hasText(employee.getLevelCode())) {
            throw new RecruitmentNotificationException(
                    "Every selected replacement employee must have a level.");
        }
    }

    private List<InternalVacancyRequirementCommand> buildReplacementRequirements(
            List<EmployeeEntity> replacementEmployees) {
        Map<String, InternalVacancyRequirementCommand> requirementByKey = new LinkedHashMap<>();
        for (EmployeeEntity employee : replacementEmployees) {
            Long designationId = employee.getDesignation().getDesignationId();
            String levelCode = employee.getLevelCode().trim().toUpperCase(Locale.ROOT);
            String key = designationId + "|" + levelCode;
            InternalVacancyRequirementCommand existing = requirementByKey.get(key);
            long vacancyCount = existing == null ? 1L : existing.getNumberOfVacancy() + 1L;
            requirementByKey.put(key, InternalVacancyRequirementCommand.builder()
                    .designationId(designationId)
                    .levelCode(levelCode)
                    .numberOfVacancy(vacancyCount)
                    .build());
        }
        return List.copyOf(requirementByKey.values());
    }

    private void applyHiringRequestDetails(
            InternalVacancyOpeningEntity entity,
            InternalVacancyHiringRequestType hiringRequestType,
            StoredInternalVacancyApprovalDocument uploadedApproval) {
        if (hiringRequestType == InternalVacancyHiringRequestType.EMPLOYEE_REPLACEMENT) {
            clearApprovalDocument(entity);
            return;
        }

        if (uploadedApproval != null) {
            entity.setEOfficeApprovalFileName(uploadedApproval.getOriginalFileName());
            entity.setEOfficeApprovalFilePath(uploadedApproval.getFullPath());
            entity.setEOfficeApprovalContentType(uploadedApproval.getContentType());
            entity.setEOfficeApprovalFileSize(uploadedApproval.getFileSize());
        }
    }

    private void clearApprovalDocument(InternalVacancyOpeningEntity entity) {
        entity.setEOfficeApprovalFileName(null);
        entity.setEOfficeApprovalFilePath(null);
        entity.setEOfficeApprovalContentType(null);
        entity.setEOfficeApprovalFileSize(null);
    }

    private boolean hasUploadedFile(MultipartFile file) {
        return file != null && !file.isEmpty();
    }

    private InternalVacancyApprovalDocumentView toApprovalDocumentView(InternalVacancyOpeningEntity entity) {
        if (!StringUtils.hasText(entity.getEOfficeApprovalFilePath())
                || !StringUtils.hasText(entity.getEOfficeApprovalFileName())) {
            throw new RecruitmentNotificationException("E-office approval document is unavailable.");
        }

        return InternalVacancyApprovalDocumentView.builder()
                .originalFileName(entity.getEOfficeApprovalFileName())
                .contentType(entity.getEOfficeApprovalContentType())
                .fileSize(entity.getEOfficeApprovalFileSize())
                .resource(approvalDocumentStorageService.loadAsResource(entity.getEOfficeApprovalFilePath()))
                .build();
    }

    private ProjectMst findInternalProject(Long projectId) {
        return projectRepository.findByProjectIdAndProjectScopeType(projectId, ProjectScopeType.INTERNAL)
                .orElseThrow(() -> new RecruitmentNotificationException(
                        "Only internal projects can be used for internal vacancy openings."));
    }

    private InternalVacancyOpeningStatus resolveTargetStatus(InternalVacancyOpeningStatus targetStatus) {
        if (targetStatus == null || targetStatus == InternalVacancyOpeningStatus.CLOSED) {
            throw new RecruitmentNotificationException("Invalid vacancy opening action.");
        }
        return targetStatus;
    }

    private InternalVacancyOpeningEntity findOpeningForFormEdit(Long internalVacancyOpeningId) {
        InternalVacancyOpeningEntity entity = findOpeningById(internalVacancyOpeningId);
        if (entity.getStatus() == InternalVacancyOpeningStatus.CLOSED) {
            throw new RecruitmentNotificationException(
                    "Deactivated internal vacancy openings must be activated before editing.");
        }
        return entity;
    }

    private InternalVacancyOpeningEntity findOpeningForUpdate(Long internalVacancyOpeningId) {
        InternalVacancyOpeningEntity entity = findOpeningById(internalVacancyOpeningId);
        if (entity.getStatus() == InternalVacancyOpeningStatus.CLOSED) {
            throw new RecruitmentNotificationException(
                    "Deactivated internal vacancy openings must be activated before updating.");
        }
        return entity;
    }

    private InternalVacancyOpeningEntity findOpeningForStatusChange(Long internalVacancyOpeningId) {
        InternalVacancyOpeningEntity entity = findOpeningById(internalVacancyOpeningId);
        if (entity.getStatus() == InternalVacancyOpeningStatus.DRAFT) {
            throw new RecruitmentNotificationException(
                    "Only submitted internal vacancy openings can be activated or deactivated.");
        }
        return entity;
    }

    private InternalVacancyOpeningEntity findOpeningById(Long internalVacancyOpeningId) {
        if (internalVacancyOpeningId == null || internalVacancyOpeningId < 1) {
            throw new RecruitmentNotificationException("Valid internal vacancy opening id is required.");
        }

        InternalVacancyOpeningEntity entity = internalVacancyOpeningRepository
                .findDetailedByInternalVacancyOpeningId(internalVacancyOpeningId)
                .orElseThrow(() -> new RecruitmentNotificationException(
                        "Internal vacancy opening not found for id: " + internalVacancyOpeningId));
        return entity;
    }

    private void validateSaveTransition(
            InternalVacancyOpeningEntity entity,
            InternalVacancyOpeningStatus targetStatus) {
        if (entity.getInternalVacancyOpeningId() == null) {
            return;
        }
        if (entity.getStatus() == InternalVacancyOpeningStatus.OPEN
                && targetStatus != InternalVacancyOpeningStatus.OPEN) {
            throw new RecruitmentNotificationException(
                    "Submitted internal vacancy openings can only be updated as active openings.");
        }
        if (entity.getStatus() == InternalVacancyOpeningStatus.DRAFT
                && targetStatus == InternalVacancyOpeningStatus.CLOSED) {
            throw new RecruitmentNotificationException(
                    "Draft internal vacancy openings cannot be deactivated.");
        }
        if (entity.getStatus() == InternalVacancyOpeningStatus.PENDING_HR_APPROVAL
                && targetStatus != InternalVacancyOpeningStatus.PENDING_HR_APPROVAL
                && targetStatus != InternalVacancyOpeningStatus.OPEN
                && targetStatus != InternalVacancyOpeningStatus.REJECTED_BY_HR) {
            throw new RecruitmentNotificationException(
                    "Requests pending HR approval can only be updated, approved, or rejected.");
        }
    }

    private void validateStatusChangeTransition(
            InternalVacancyOpeningEntity entity,
            InternalVacancyOpeningStatus targetStatus) {
        if (targetStatus == null) {
            throw new RecruitmentNotificationException("Internal vacancy opening status is required.");
        }
        if (targetStatus == InternalVacancyOpeningStatus.CLOSED
                && entity.getStatus() != InternalVacancyOpeningStatus.OPEN) {
            throw new RecruitmentNotificationException(
                    "Only active internal vacancy openings can be deactivated.");
        }
        if (targetStatus == InternalVacancyOpeningStatus.OPEN
                && entity.getStatus() != InternalVacancyOpeningStatus.CLOSED
                && entity.getStatus() != InternalVacancyOpeningStatus.PENDING_HR_APPROVAL) {
            throw new RecruitmentNotificationException(
                    "Only deactivated openings or pending requests can be moved to active status.");
        }
        if (targetStatus == InternalVacancyOpeningStatus.REJECTED_BY_HR
                && entity.getStatus() != InternalVacancyOpeningStatus.PENDING_HR_APPROVAL) {
            throw new RecruitmentNotificationException(
                    "Only requests pending HR approval can be rejected.");
        }
    }

    private Map<Long, ManpowerDesignationMaster> resolveDesignations(List<InternalVacancyRequirementCommand> requirements) {
        Set<Long> designationIds = requirements.stream()
                .map(InternalVacancyRequirementCommand::getDesignationId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (designationIds.isEmpty()) {
            throw new RecruitmentNotificationException("At least one valid designation is required.");
        }

        Map<Long, ManpowerDesignationMaster> designationById = designationRepository.findAllById(designationIds)
                .stream()
                .filter(designation -> "Y".equalsIgnoreCase(designation.getActiveFlag()))
                .collect(Collectors.toMap(
                        ManpowerDesignationMaster::getDesignationId,
                        designation -> designation,
                        (left, right) -> left,
                        LinkedHashMap::new));

        if (designationById.size() != designationIds.size()) {
            Set<Long> missingIds = new LinkedHashSet<>(designationIds);
            missingIds.removeAll(designationById.keySet());
            throw new RecruitmentNotificationException("Active designations not found for ids: " + missingIds);
        }

        return designationById;
    }

    private List<InternalVacancyOpeningRequirementEntity> buildRequirementEntities(
            List<InternalVacancyRequirementCommand> requirements,
            Map<Long, ManpowerDesignationMaster> designationById) {
        Set<String> uniqueKeys = new LinkedHashSet<>();
        List<InternalVacancyOpeningRequirementEntity> entities = new ArrayList<>();

        for (InternalVacancyRequirementCommand requirement : requirements) {
            validateRequirement(requirement);

            String normalizedLevelCode = requirement.getLevelCode().trim().toUpperCase(Locale.ROOT);
            String duplicateKey = requirement.getDesignationId() + "|" + normalizedLevelCode;
            if (!uniqueKeys.add(duplicateKey)) {
                throw new RecruitmentNotificationException(
                        "Duplicate designation and level combination detected in the vacancy opening form.");
            }

            ManpowerDesignationMaster designation = designationById.get(requirement.getDesignationId());
            if (designation == null) {
                throw new RecruitmentNotificationException(
                        "Active designation not found for id: " + requirement.getDesignationId());
            }

            boolean levelMapped = designation.getLevels().stream()
                    .anyMatch(level -> level.getLevelCode() != null
                            && level.getLevelCode().equalsIgnoreCase(normalizedLevelCode));
            if (!levelMapped) {
                throw new RecruitmentNotificationException(
                        "Selected level is not mapped to designation: " + designation.getDesignationName());
            }

            BigDecimal monthlyRate = getMonthlyRate(requirement.getDesignationId(), normalizedLevelCode);

            InternalVacancyOpeningRequirementEntity entity = new InternalVacancyOpeningRequirementEntity();
            entity.setDesignationMst(designation);
            entity.setLevelCode(normalizedLevelCode);
            entity.setMonthlyRate(monthlyRate.setScale(2, RoundingMode.HALF_UP));
            entity.setNumberOfVacancy(requirement.getNumberOfVacancy());
            entity.setFilledPositions(0L);
            entities.add(entity);
        }

        return entities;
    }

    private List<Role> resolveInterviewRoles(List<Long> roleIds) {
        List<Long> normalizedRoleIds = roleIds == null ? List.of() : roleIds.stream()
                .filter(id -> id != null && !id.equals(EMPLOYEE_ROLE_ID) && id > 0)
                .distinct()
                .toList();
        
        // We don't throw error here anymore if empty, 
        // we'll check overall panel validity later if needed.
        if (normalizedRoleIds.isEmpty()) {
            return List.of();
        }

        List<Role> roles = findAllowedInterviewAuthorityRoles(normalizedRoleIds);
        Set<Long> resolvedRoleIds = roles.stream()
                .map(Role::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (resolvedRoleIds.size() != normalizedRoleIds.size()) {
            Set<Long> invalidIds = new LinkedHashSet<>(normalizedRoleIds);
            invalidIds.removeAll(resolvedRoleIds);
            throw new RecruitmentNotificationException(
                    "Only HOD, PM, and STM roles can be assigned as interview authorities. Invalid role ids: "
                            + invalidIds);
        }

        return normalizedRoleIds.stream()
                .map(roleId -> roles.stream()
                        .filter(role -> roleId.equals(role.getId()))
                        .findFirst()
                        .orElse(null))
                .filter(role -> role != null)
                .toList();
    }

    private List<Role> findAllowedInterviewAuthorityRoles(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Role> rolesById = roleRepository.findAllById(roleIds).stream()
                .filter(role -> StringUtils.hasText(role.getName()))
                .filter(role -> ALLOWED_INTERVIEW_AUTHORITY_ROLE_NAMES.contains(
                        role.getName().trim().toUpperCase(Locale.ROOT)))
                .collect(Collectors.toMap(
                        Role::getId,
                        role -> role,
                        (left, right) -> left,
                        LinkedHashMap::new));

        return roleIds.stream()
                .map(rolesById::get)
                .filter(role -> role != null)
                .toList();
    }

    private List<Role> getAllowedInterviewAuthorityRoles() {
        return roleRepository.findAllByOrderByNameAsc().stream()
                .filter(role -> StringUtils.hasText(role.getName()))
                .filter(role -> ALLOWED_INTERVIEW_AUTHORITY_ROLE_NAMES.contains(
                        role.getName().trim().toUpperCase(Locale.ROOT)))
                .sorted(Comparator.comparingInt(role -> ALLOWED_INTERVIEW_AUTHORITY_ROLE_NAMES.indexOf(
                        role.getName().trim().toUpperCase(Locale.ROOT))))
                .toList();
    }

    private List<Long> resolveEditInterviewAuthorityRoleIds(InternalVacancyOpeningEntity entity) {
        List<Long> roleIds = new ArrayList<>(entity.getInterviewRoles().stream()
                .map(InternalVacancyInterviewRoleEntity::getRole)
                .filter(this::isAllowedInterviewAuthorityRole)
                .map(Role::getId)
                .distinct()
                .toList());

        if (entity.getInterviewEmployees() != null && !entity.getInterviewEmployees().isEmpty()) {
            roleIds.add(EMPLOYEE_ROLE_ID);
        }

        if (!roleIds.isEmpty()) {
            return roleIds;
        }

        List<Long> userRoleIds = entity.getInterviewAuthorities().stream()
                .map(InternalVacancyInterviewAuthorityEntity::getUser)
                .filter(user -> user != null && user.getRoles() != null)
                .flatMap(user -> user.getRoles().stream())
                .filter(this::isAllowedInterviewAuthorityRole)
                .map(Role::getId)
                .distinct()
                .collect(Collectors.toList());
        
        if (entity.getInterviewEmployees() != null && !entity.getInterviewEmployees().isEmpty()) {
            userRoleIds.add(EMPLOYEE_ROLE_ID);
        }
        
        return userRoleIds;
    }

    private boolean isAllowedInterviewAuthorityRole(Role role) {
        return role != null
                && role.getId() != null
                && StringUtils.hasText(role.getName())
                && ALLOWED_INTERVIEW_AUTHORITY_ROLE_NAMES.contains(role.getName().trim().toUpperCase(Locale.ROOT));
    }

    private List<User> resolveInterviewAuthorities(List<Long> userIds, List<Role> selectedRoles) {
        List<Long> normalizedUserIds = normalizePositiveIds(userIds);
        if (normalizedUserIds.isEmpty()) {
            throw new RecruitmentNotificationException("At least one interview authority is required.");
        }

        Map<Long, User> usersById = userRepository.findAllWithRolesByIdIn(normalizedUserIds).stream()
                .collect(Collectors.toMap(
                        User::getId,
                        user -> user,
                        (left, right) -> left,
                        LinkedHashMap::new));

        if (usersById.size() != normalizedUserIds.size()) {
            Set<Long> missingIds = new LinkedHashSet<>(normalizedUserIds);
            missingIds.removeAll(usersById.keySet());
            throw new RecruitmentNotificationException("Interview authorities not found for user ids: " + missingIds);
        }

        Set<Long> selectedRoleIds = selectedRoles.stream()
                .map(Role::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<User> users = normalizedUserIds.stream()
                .map(usersById::get)
                .toList();
        for (User user : users) {
            boolean matchesSelectedRoles = user.getRoles() != null && user.getRoles().stream()
                    .map(Role::getId)
                    .anyMatch(selectedRoleIds::contains);
            if (!matchesSelectedRoles) {
                throw new RecruitmentNotificationException(
                        "Selected interview authority does not belong to the chosen roles: "
                                + buildInterviewAuthorityLabel(user));
            }
        }

        return users;
    }

    private List<InternalVacancyInterviewRoleEntity> buildInterviewRoleEntities(List<Role> roles) {
        return roles.stream()
                .map(role -> {
                    InternalVacancyInterviewRoleEntity entity = new InternalVacancyInterviewRoleEntity();
                    entity.setRole(role);
                    return entity;
                })
                .toList();
    }

    private List<InternalVacancyInterviewAuthorityEntity> buildInterviewAuthorityEntities(List<User> users) {
        return users.stream()
                .map(user -> {
                    InternalVacancyInterviewAuthorityEntity entity = new InternalVacancyInterviewAuthorityEntity();
                    entity.setUser(user);
                    return entity;
                })
                .toList();
    }

    private void validateRequirement(InternalVacancyRequirementCommand requirement) {
        if (requirement == null) {
            throw new RecruitmentNotificationException("Invalid designation requirement row.");
        }
        if (requirement.getDesignationId() == null || requirement.getDesignationId() < 1) {
            throw new RecruitmentNotificationException("Designation is required for every vacancy row.");
        }
        if (!StringUtils.hasText(requirement.getLevelCode())) {
            throw new RecruitmentNotificationException("Level is required for every vacancy row.");
        }
        if (requirement.getNumberOfVacancy() == null || requirement.getNumberOfVacancy() < 1) {
            throw new RecruitmentNotificationException("Number of vacancies must be greater than zero.");
        }
    }

    private InternalVacancyOpeningSummaryView toSummaryView(InternalVacancyOpeningEntity entity) {
        long totalVacancies = entity.getRequirements().stream()
                .map(InternalVacancyOpeningRequirementEntity::getNumberOfVacancy)
                .filter(value -> value != null && value > 0)
                .mapToLong(Long::longValue)
                .sum();

        String createdByName = null;
        if (org.springframework.util.StringUtils.hasText(entity.getCreatedByEmail())) {
            createdByName = userRepository.findByEmailIgnoreCase(entity.getCreatedByEmail())
                    .map(com.maharecruitment.gov.in.auth.entity.User::getName)
                    .orElse(null);
        }

        return InternalVacancyOpeningSummaryView.builder()
                .internalVacancyOpeningId(entity.getInternalVacancyOpeningId())
                .requestId(entity.getRequestId())
                .projectId(entity.getProjectMst().getProjectId())
                .projectName(entity.getProjectMst().getProjectName())
                .designationCount(entity.getRequirements().size())
                .totalVacancies(totalVacancies)
                .status(entity.getStatus())
                .createdDateTime(entity.getCreatedDateTime())
                .createdByEmail(entity.getCreatedByEmail())
                .createdByName(createdByName)
                .build();
    }

    private InternalVacancyRequirementForm toRequirementForm(InternalVacancyOpeningRequirementEntity entity) {
        InternalVacancyRequirementForm form = new InternalVacancyRequirementForm();
        form.setDesignationId(entity.getDesignationMst().getDesignationId());
        form.setDesignationName(entity.getDesignationMst().getDesignationName());
        form.setLevelCode(entity.getLevelCode());
        form.setLevelName(resolveLevelName(entity));
        form.setNumberOfVacancy(entity.getNumberOfVacancy());
        return form;
    }

    private String resolveLevelName(InternalVacancyOpeningRequirementEntity entity) {
        if (!StringUtils.hasText(entity.getLevelCode())
                || entity.getDesignationMst() == null
                || entity.getDesignationMst().getLevels() == null) {
            return entity.getLevelCode();
        }

        return entity.getDesignationMst().getLevels().stream()
                .filter(level -> level.getLevelCode() != null
                        && level.getLevelCode().equalsIgnoreCase(entity.getLevelCode()))
                .map(ResourceLevelExperience::getLevelName)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(entity.getLevelCode());
    }

    private void replaceChildCollections(
            InternalVacancyOpeningEntity entity,
            List<InternalVacancyOpeningRequirementEntity> requirementEntities,
            List<InternalVacancyInterviewRoleEntity> interviewRoleEntities,
            List<InternalVacancyInterviewAuthorityEntity> interviewAuthorityEntities,
            List<InternalVacancyInterviewEmployeeEntity> interviewEmployeeEntities,
            List<EmployeeEntity> replacementEmployees,
            boolean isEdit) {
        if (isEdit) {
            entity.replaceRequirements(List.of());
            entity.replaceInterviewRoles(List.of());
            entity.replaceInterviewAuthorities(List.of());
            entity.replaceInterviewEmployees(List.of());
            entity.replaceReplacementEmployees(List.of());
            internalVacancyOpeningRepository.saveAndFlush(entity);
            entity.replaceRequirements(requirementEntities);
            entity.replaceInterviewRoles(interviewRoleEntities);
            entity.replaceInterviewAuthorities(interviewAuthorityEntities);
            entity.replaceInterviewEmployees(interviewEmployeeEntities);
            entity.replaceReplacementEmployees(replacementEmployees);
        } else {
            entity.replaceRequirements(requirementEntities);
            entity.replaceInterviewRoles(interviewRoleEntities);
            entity.replaceInterviewAuthorities(interviewAuthorityEntities);
            entity.replaceInterviewEmployees(interviewEmployeeEntities);
            entity.replaceReplacementEmployees(replacementEmployees);
        }
    }

    private List<EmployeeEntity> resolveInterviewEmployees(List<Long> employeeIds, boolean isMandatory) {
        List<Long> normalizedIds = normalizePositiveIds(employeeIds);
        if (normalizedIds.isEmpty()) {
            return List.of();
        }

        List<EmployeeEntity> employees = employeeRepository.findDetailedByEmployeeIdIn(normalizedIds);
        if (employees.size() != normalizedIds.size()) {
            Set<Long> missingIds = new LinkedHashSet<>(normalizedIds);
            missingIds.removeAll(employees.stream().map(EmployeeEntity::getEmployeeId).collect(Collectors.toSet()));
            throw new RecruitmentNotificationException("Interview employees not found for ids: " + missingIds);
        }

        return employees;
    }

    private List<InternalVacancyInterviewEmployeeEntity> buildInterviewEmployeeEntities(List<EmployeeEntity> employees) {
        return employees.stream()
                .map(employee -> {
                    InternalVacancyInterviewEmployeeEntity entity = new InternalVacancyInterviewEmployeeEntity();
                    entity.setEmployee(employee);
                    return entity;
                })
                .toList();
    }

    private String buildInterviewEmployeeLabel(EmployeeEntity employee) {
        StringBuilder label = new StringBuilder();
        label.append(employee.getFullName());
        if (StringUtils.hasText(employee.getEmployeeCode())) {
            label.append(" (").append(employee.getEmployeeCode()).append(")");
        }
        if (employee.getDesignation() != null && StringUtils.hasText(employee.getDesignation().getDesignationName())) {
            label.append(" - ").append(employee.getDesignation().getDesignationName());
        }
        return label.toString();
    }

    private String buildReplacementEmployeeLabel(EmployeeEntity employee) {
        StringBuilder label = new StringBuilder(employee.getFullName());
        if (employee.getDesignation() != null
                && StringUtils.hasText(employee.getDesignation().getDesignationName())) {
            label.append(" - ").append(employee.getDesignation().getDesignationName());
        }
        String levelName = resolveEmployeeLevelName(employee);
        if (StringUtils.hasText(levelName)) {
            label.append(" - ").append(levelName);
        }
        return label.toString();
    }

    private String normalizeActorEmail(String actorEmail) {
        if (!StringUtils.hasText(actorEmail)) {
            throw new RecruitmentNotificationException("Authenticated user is required.");
        }
        return actorEmail.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeOptionalText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String buildSearchPattern(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return "%" + value.trim().toUpperCase(Locale.ROOT) + "%";
    }

    private List<Long> normalizePositiveIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        return ids.stream()
                .filter(id -> id != null && (id > 0 || id.equals(EMPLOYEE_ROLE_ID)))
                .distinct()
                .toList();
    }

    private String buildInterviewAuthorityLabel(User user) {
        String name = normalizeOptionalText(user.getName());
        String email = normalizeOptionalText(user.getEmail());
        if (name == null && email == null) {
            return "User ID " + user.getId();
        }
        if (name == null) {
            return email;
        }
        if (email == null) {
            return name;
        }
        return name + " (" + email + ")";
    }

    private String toRoleLabel(String roleName) {
        if (!StringUtils.hasText(roleName)) {
            return "Role";
        }

        String normalizedRoleName = roleName.trim();
        String withoutPrefix = normalizedRoleName.startsWith("ROLE_")
                ? normalizedRoleName.substring(5)
                : normalizedRoleName;
        return withoutPrefix.replace('_', ' ');
    }

    private String normalizeSortText(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
