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
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
import com.maharecruitment.gov.in.recruitment.entity.InternalVacancyOpeningEntity;
import com.maharecruitment.gov.in.recruitment.entity.InternalVacancyOpeningRequirementEntity;
import com.maharecruitment.gov.in.recruitment.entity.InternalVacancyOpeningStatus;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.recruitment.repository.InternalVacancyOpeningRepository;
import com.maharecruitment.gov.in.recruitment.service.InternalVacancyOpeningService;
import com.maharecruitment.gov.in.recruitment.service.RecruitmentNotificationService;
import com.maharecruitment.gov.in.recruitment.service.RecruitmentRequestIdGenerator;
import com.maharecruitment.gov.in.recruitment.service.model.InternalProjectOptionView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyOpeningCommand;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyOpeningLevelOptionView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyOpeningResult;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyOpeningSummaryView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyRequirementCommand;

@Service
@Transactional(readOnly = true)
public class InternalVacancyOpeningServiceImpl implements InternalVacancyOpeningService {

    private static final Logger log = LoggerFactory.getLogger(InternalVacancyOpeningServiceImpl.class);
    private static final String INTERNAL_REQUEST_TYPE = "I";

    private final InternalVacancyOpeningRepository internalVacancyOpeningRepository;
    private final ProjectMstRepository projectRepository;
    private final ManpowerDesignationMasterRepository designationRepository;
    private final ManpowerDesignationMasterService designationService;
    private final ManpowerDesignationRateService designationRateService;
    private final RecruitmentRequestIdGenerator recruitmentRequestIdGenerator;
    private final RecruitmentNotificationService recruitmentNotificationService;

    public InternalVacancyOpeningServiceImpl(
            InternalVacancyOpeningRepository internalVacancyOpeningRepository,
            ProjectMstRepository projectRepository,
            ManpowerDesignationMasterRepository designationRepository,
            ManpowerDesignationMasterService designationService,
            ManpowerDesignationRateService designationRateService,
            RecruitmentRequestIdGenerator recruitmentRequestIdGenerator,
            RecruitmentNotificationService recruitmentNotificationService) {
        this.internalVacancyOpeningRepository = internalVacancyOpeningRepository;
        this.projectRepository = projectRepository;
        this.designationRepository = designationRepository;
        this.designationService = designationService;
        this.designationRateService = designationRateService;
        this.recruitmentRequestIdGenerator = recruitmentRequestIdGenerator;
        this.recruitmentNotificationService = recruitmentNotificationService;
    }

    @Override
    @Transactional
    public InternalVacancyOpeningResult saveOpening(InternalVacancyOpeningCommand command) {
        validateCommand(command);

        String actorEmail = normalizeActorEmail(command.getActorEmail());
        ProjectMst project = findInternalProject(command.getProjectId());
        InternalVacancyOpeningStatus targetStatus = resolveTargetStatus(command.getTargetStatus());

        Map<Long, ManpowerDesignationMaster> designationById = resolveDesignations(command.getRequirements());
        List<InternalVacancyOpeningRequirementEntity> requirementEntities = buildRequirementEntities(
                command.getRequirements(),
                designationById);

        boolean isEdit = command.getInternalVacancyOpeningId() != null;
        InternalVacancyOpeningEntity entity = isEdit
                ? findEditableOpening(command.getInternalVacancyOpeningId())
                : new InternalVacancyOpeningEntity();

        if (!isEdit) {
            entity.setRequestId(recruitmentRequestIdGenerator.generate(INTERNAL_REQUEST_TYPE));
            entity.setCreatedByEmail(actorEmail);
        }

        entity.setProjectMst(project);
        entity.setStatus(targetStatus);
        entity.setRemarks(normalizeOptionalText(command.getRemarks()));
        entity.setUpdatedByEmail(actorEmail);
        replaceRequirements(entity, requirementEntities, isEdit);

        InternalVacancyOpeningEntity saved = targetStatus == InternalVacancyOpeningStatus.OPEN
                ? internalVacancyOpeningRepository.saveAndFlush(entity)
                : internalVacancyOpeningRepository.save(entity);
        if (targetStatus == InternalVacancyOpeningStatus.OPEN) {
            recruitmentNotificationService.upsertFromInternalVacancyOpening(saved.getInternalVacancyOpeningId());
        }
        long totalVacancies = saved.getRequirements().stream()
                .map(InternalVacancyOpeningRequirementEntity::getNumberOfVacancy)
                .filter(value -> value != null && value > 0)
                .mapToLong(Long::longValue)
                .sum();

        log.info(
                "Internal vacancy opening saved. operation={}, requestId={}, openingId={}, status={}, projectId={}, projectName={}, designationCount={}, totalVacancies={}, actor={}",
                isEdit ? "UPDATE" : "CREATE",
                saved.getRequestId(),
                saved.getInternalVacancyOpeningId(),
                saved.getStatus(),
                project.getProjectId(),
                project.getProjectName(),
                saved.getRequirements().size(),
                totalVacancies,
                actorEmail);

        return InternalVacancyOpeningResult.builder()
                .internalVacancyOpeningId(saved.getInternalVacancyOpeningId())
                .requestId(saved.getRequestId())
                .build();
    }

    @Override
    public List<InternalVacancyOpeningSummaryView> getAllOpenings() {
        return internalVacancyOpeningRepository.findAllByOrderByInternalVacancyOpeningIdDesc()
                .stream()
                .map(this::toSummaryView)
                .toList();
    }

    @Override
    public InternalVacancyOpeningForm getOpeningForEdit(Long internalVacancyOpeningId) {
        InternalVacancyOpeningEntity entity = findEditableOpening(internalVacancyOpeningId);

        InternalVacancyOpeningForm form = new InternalVacancyOpeningForm();
        form.setInternalVacancyOpeningId(entity.getInternalVacancyOpeningId());
        form.setProjectId(entity.getProjectMst().getProjectId());
        form.setRemarks(entity.getRemarks());
        form.setRequirements(entity.getRequirements().stream()
                .map(this::toRequirementForm)
                .toList());
        return form;
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
        if (command.getRequirements() == null || command.getRequirements().isEmpty()) {
            throw new RecruitmentNotificationException("At least one designation requirement is required.");
        }
        if (!StringUtils.hasText(command.getActorEmail())) {
            throw new RecruitmentNotificationException("Authenticated user is required.");
        }
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

    private InternalVacancyOpeningEntity findEditableOpening(Long internalVacancyOpeningId) {
        if (internalVacancyOpeningId == null || internalVacancyOpeningId < 1) {
            throw new RecruitmentNotificationException("Valid internal vacancy opening id is required.");
        }

        InternalVacancyOpeningEntity entity = internalVacancyOpeningRepository
                .findDetailedByInternalVacancyOpeningId(internalVacancyOpeningId)
                .orElseThrow(() -> new RecruitmentNotificationException(
                        "Internal vacancy opening not found for id: " + internalVacancyOpeningId));

        if (entity.getStatus() != InternalVacancyOpeningStatus.DRAFT) {
            throw new RecruitmentNotificationException("Only draft internal vacancy openings can be edited.");
        }

        return entity;
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

        return InternalVacancyOpeningSummaryView.builder()
                .internalVacancyOpeningId(entity.getInternalVacancyOpeningId())
                .requestId(entity.getRequestId())
                .projectId(entity.getProjectMst().getProjectId())
                .projectName(entity.getProjectMst().getProjectName())
                .designationCount(entity.getRequirements().size())
                .totalVacancies(totalVacancies)
                .status(entity.getStatus())
                .createdDateTime(entity.getCreatedDateTime())
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

    private void replaceRequirements(
            InternalVacancyOpeningEntity entity,
            List<InternalVacancyOpeningRequirementEntity> requirementEntities,
            boolean isEdit) {
        if (isEdit) {
            entity.replaceRequirements(List.of());
            internalVacancyOpeningRepository.saveAndFlush(entity);
        }
        entity.replaceRequirements(requirementEntities);
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
}
