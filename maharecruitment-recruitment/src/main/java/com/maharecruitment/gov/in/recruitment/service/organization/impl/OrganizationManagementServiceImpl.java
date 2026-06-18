package com.maharecruitment.gov.in.recruitment.service.organization.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.master.entity.CellMaster;
import com.maharecruitment.gov.in.master.entity.ManpowerDesignationMaster;
import com.maharecruitment.gov.in.master.entity.ProjectMst;
import com.maharecruitment.gov.in.master.entity.ProjectScopeType;
import com.maharecruitment.gov.in.master.entity.ProjectType;
import com.maharecruitment.gov.in.master.entity.ResourceLevelExperience;
import com.maharecruitment.gov.in.master.exception.BusinessValidationException;
import com.maharecruitment.gov.in.master.exception.DuplicateResourceException;
import com.maharecruitment.gov.in.master.exception.ResourceNotFoundException;
import com.maharecruitment.gov.in.master.repository.CellMasterRepository;
import com.maharecruitment.gov.in.master.repository.ManpowerDesignationMasterRepository;
import com.maharecruitment.gov.in.master.repository.ProjectMstRepository;
import com.maharecruitment.gov.in.master.repository.ResourceLevelExperienceRepository;
import com.maharecruitment.gov.in.recruitment.dto.organization.EmployeeTeamMappingRequest;
import com.maharecruitment.gov.in.recruitment.dto.organization.EmployeeTeamMappingResponse;
import com.maharecruitment.gov.in.recruitment.dto.organization.OrganizationAuditResponse;
import com.maharecruitment.gov.in.recruitment.dto.organization.OrganizationLookupOption;
import com.maharecruitment.gov.in.recruitment.dto.organization.PositionRequest;
import com.maharecruitment.gov.in.recruitment.dto.organization.PositionResponse;
import com.maharecruitment.gov.in.recruitment.dto.organization.TeamByCellResponse;
import com.maharecruitment.gov.in.recruitment.dto.organization.TeamRequest;
import com.maharecruitment.gov.in.recruitment.dto.organization.TeamResponse;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.entity.organization.EmployeeTeamMappingEntity;
import com.maharecruitment.gov.in.recruitment.entity.organization.OrganizationAuditAction;
import com.maharecruitment.gov.in.recruitment.entity.organization.OrganizationAuditLogEntity;
import com.maharecruitment.gov.in.recruitment.entity.organization.OrganizationRecordStatus;
import com.maharecruitment.gov.in.recruitment.entity.organization.OrganizationTeamType;
import com.maharecruitment.gov.in.recruitment.entity.organization.PositionMasterEntity;
import com.maharecruitment.gov.in.recruitment.entity.organization.PositionStatus;
import com.maharecruitment.gov.in.recruitment.entity.organization.TeamMasterEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.recruitment.repository.organization.EmployeeTeamMappingRepository;
import com.maharecruitment.gov.in.recruitment.repository.organization.OrganizationAuditLogRepository;
import com.maharecruitment.gov.in.recruitment.repository.organization.OrganizationTeamRepository;
import com.maharecruitment.gov.in.recruitment.repository.organization.PositionMasterRepository;
import com.maharecruitment.gov.in.recruitment.service.organization.OrganizationManagementService;

@Service
@Transactional(readOnly = true)
public class OrganizationManagementServiceImpl implements OrganizationManagementService {

    private static final String ACTIVE_FLAG = "Y";
    private static final String ACTIVE_EMPLOYEE_STATUS = "ACTIVE";
    private static final String SAMPLE_CATEGORY = "MAHAIT Organization";

    private final OrganizationTeamRepository teamRepository;
    private final PositionMasterRepository positionRepository;
    private final EmployeeTeamMappingRepository mappingRepository;
    private final OrganizationAuditLogRepository auditLogRepository;
    private final ProjectMstRepository projectRepository;
    private final CellMasterRepository cellMasterRepository;
    private final ManpowerDesignationMasterRepository designationRepository;
    private final ResourceLevelExperienceRepository levelRepository;
    private final EmployeeRepository employeeRepository;

    public OrganizationManagementServiceImpl(
            OrganizationTeamRepository teamRepository,
            PositionMasterRepository positionRepository,
            EmployeeTeamMappingRepository mappingRepository,
            OrganizationAuditLogRepository auditLogRepository,
            ProjectMstRepository projectRepository,
            CellMasterRepository cellMasterRepository,
            ManpowerDesignationMasterRepository designationRepository,
            ResourceLevelExperienceRepository levelRepository,
            EmployeeRepository employeeRepository) {
        this.teamRepository = teamRepository;
        this.positionRepository = positionRepository;
        this.mappingRepository = mappingRepository;
        this.auditLogRepository = auditLogRepository;
        this.projectRepository = projectRepository;
        this.cellMasterRepository = cellMasterRepository;
        this.designationRepository = designationRepository;
        this.levelRepository = levelRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    @Transactional
    public TeamResponse createTeam(TeamRequest request) {
        CellMaster cell = resolveActiveCell(request.getCellId());
        ProjectMst project = resolveOptionalActiveProject(request.getProjectId(), cell);
        String teamName = normalizeRequired(request.getTeamName(), "Team name");
        ensureUniqueTeam(cell.getCellId(), teamName, null);

        TeamMasterEntity team = new TeamMasterEntity();
        mapTeamRequest(request, team, project, cell, teamName);
        TeamMasterEntity saved = teamRepository.save(team);
        log(OrganizationAuditAction.TEAM_CREATED, "TEAM", saved.getTeamId(), "Team created", saved.getTeamName());
        return toTeamResponse(saved);
    }

    @Override
    @Transactional
    public TeamResponse updateTeam(Long teamId, TeamRequest request) {
        TeamMasterEntity team = getTeamEntity(teamId);
        CellMaster cell = resolveActiveCell(request.getCellId());
        ProjectMst project = resolveOptionalActiveProject(request.getProjectId(), cell);
        String teamName = normalizeRequired(request.getTeamName(), "Team name");
        ensureUniqueTeam(cell.getCellId(), teamName, teamId);

        mapTeamRequest(request, team, project, cell, teamName);
        ensureNoTeamCycle(team);
        TeamMasterEntity saved = teamRepository.save(team);
        log(OrganizationAuditAction.TEAM_UPDATED, "TEAM", saved.getTeamId(), "Team updated", saved.getTeamName());
        return toTeamResponse(saved);
    }

    @Override
    public TeamResponse getTeam(Long teamId) {
        return toTeamResponse(getTeamEntity(teamId));
    }

    @Override
    public Page<TeamResponse> searchTeams(
            Long projectId,
            Long cellId,
            boolean includeInactive,
            String search,
            Pageable pageable) {
        return teamRepository.searchTeams(
                projectId,
                cellId,
                includeInactive,
                OrganizationRecordStatus.ACTIVE,
                toSearchPattern(search),
                pageable)
                .map(this::toTeamResponse);
    }

    @Override
    @Transactional
    public void changeTeamStatus(Long teamId, boolean active) {
        TeamMasterEntity team = getTeamEntity(teamId);
        if (!active) {
            if (teamRepository.existsByParentTeam_TeamIdAndStatus(teamId, OrganizationRecordStatus.ACTIVE)) {
                throw new BusinessValidationException("Deactivate child teams before deactivating this team.");
            }
            if (positionRepository.countByTeam_TeamId(teamId) > 0) {
                throw new BusinessValidationException("Team has mapped positions and cannot be deactivated.");
            }
        }

        team.setStatus(active ? OrganizationRecordStatus.ACTIVE : OrganizationRecordStatus.INACTIVE);
        teamRepository.save(team);
        log(OrganizationAuditAction.TEAM_STATUS_CHANGED, "TEAM", teamId,
                active ? "Team activated" : "Team deactivated", team.getTeamName());
    }

    @Override
    @Transactional
    public PositionResponse createPosition(PositionRequest request) {
        CellMaster cell = resolveActiveCell(request.getCellId());
        TeamMasterEntity team = resolveTeamRequired(request.getTeamId(), cell);
        PositionMasterEntity position = new PositionMasterEntity();
        mapPositionRequest(request, position, cell, team);
        ensureEmployeeAvailable(position.getEmployee(), null);

        PositionMasterEntity saved = positionRepository.save(position);
        syncActiveMappingForPosition(saved, request.getEmployeeId() != null);
        logPositionCreate(saved);
        return toPositionResponse(saved);
    }

    @Override
    @Transactional
    public PositionResponse updatePosition(Long positionId, PositionRequest request) {
        PositionMasterEntity position = getPositionEntity(positionId);
        Long previousEmployeeId = position.getEmployee() == null ? null : position.getEmployee().getEmployeeId();
        PositionStatus previousStatus = position.getPositionStatus();

        CellMaster cell = resolveActiveCell(request.getCellId());
        TeamMasterEntity team = resolveTeamRequired(request.getTeamId(), cell);
        mapPositionRequest(request, position, cell, team);
        ensureNoPositionCycle(position);
        ensureEmployeeAvailable(position.getEmployee(), positionId);

        PositionMasterEntity saved = positionRepository.save(position);
        syncActiveMappingForPosition(saved, !equalsLong(previousEmployeeId, request.getEmployeeId()));
        logPositionUpdate(saved, previousStatus, previousEmployeeId);
        return toPositionResponse(saved);
    }

    @Override
    public PositionResponse getPosition(Long positionId) {
        return toPositionResponse(getPositionEntity(positionId));
    }

    @Override
    public Page<PositionResponse> searchPositions(
            Long projectId,
            Long cellId,
            Long teamId,
            boolean includeInactive,
            String search,
            Pageable pageable) {
        return positionRepository.searchPositions(
                projectId,
                cellId,
                teamId,
                includeInactive,
                OrganizationRecordStatus.ACTIVE,
                toSearchPattern(search),
                pageable)
                .map(this::toPositionResponse);
    }

    @Override
    @Transactional
    public void changePositionStatus(Long positionId, boolean active) {
        PositionMasterEntity position = getPositionEntity(positionId);
        position.setStatus(active ? OrganizationRecordStatus.ACTIVE : OrganizationRecordStatus.INACTIVE);
        if (!active) {
            closeActiveMapping(position);
        }
        positionRepository.save(position);
        log(OrganizationAuditAction.POSITION_STATUS_CHANGED, "POSITION", positionId,
                active ? "Position activated" : "Position deactivated", position.getPositionName());
    }

    @Override
    @Transactional
    public EmployeeTeamMappingResponse createMapping(EmployeeTeamMappingRequest request) {
        EmployeeTeamMappingEntity mapping = new EmployeeTeamMappingEntity();
        mapMappingRequest(request, mapping);
        closeOtherActiveMapping(mapping);
        EmployeeTeamMappingEntity saved = mappingRepository.save(mapping);
        applyMappingToPosition(saved);
        log(OrganizationAuditAction.TEAM_ASSIGNMENT_CHANGED, "MAPPING", saved.getMappingId(),
                "Employee team assignment created", buildMappingDetails(saved));
        return toMappingResponse(saved);
    }

    @Override
    @Transactional
    public EmployeeTeamMappingResponse updateMapping(Long mappingId, EmployeeTeamMappingRequest request) {
        EmployeeTeamMappingEntity mapping = getMappingEntity(mappingId);
        mapMappingRequest(request, mapping);
        closeOtherActiveMapping(mapping);
        EmployeeTeamMappingEntity saved = mappingRepository.save(mapping);
        applyMappingToPosition(saved);
        log(OrganizationAuditAction.TEAM_ASSIGNMENT_CHANGED, "MAPPING", saved.getMappingId(),
                "Employee team assignment updated", buildMappingDetails(saved));
        return toMappingResponse(saved);
    }

    @Override
    public EmployeeTeamMappingResponse getMapping(Long mappingId) {
        return toMappingResponse(getMappingEntity(mappingId));
    }

    @Override
    public Page<EmployeeTeamMappingResponse> searchMappings(
            Long projectId,
            Long cellId,
            Long teamId,
            boolean includeInactive,
            String search,
            Pageable pageable) {
        return mappingRepository.searchMappings(
                projectId,
                cellId,
                teamId,
                includeInactive,
                OrganizationRecordStatus.ACTIVE,
                toSearchPattern(search),
                pageable)
                .map(this::toMappingResponse);
    }

    @Override
    @Transactional
    public void deactivateMapping(Long mappingId) {
        EmployeeTeamMappingEntity mapping = getMappingEntity(mappingId);
        mapping.setStatus(OrganizationRecordStatus.INACTIVE);
        mappingRepository.save(mapping);

        PositionMasterEntity position = mapping.getPosition();
        if (position != null && sameEmployee(position.getEmployee(), mapping.getEmployee())) {
            position.setEmployee(null);
            positionRepository.save(position);
            log(OrganizationAuditAction.VACANCY_CREATED, "POSITION", position.getPositionId(),
                    "Vacancy created", position.getPositionName());
        }

        log(OrganizationAuditAction.TEAM_ASSIGNMENT_CHANGED, "MAPPING", mappingId,
                "Employee team assignment deactivated", buildMappingDetails(mapping));
    }

    @Override
    public List<OrganizationLookupOption> getProjectOptions() {
        return projectRepository.findByActiveFlagIgnoreCaseOrderByProjectNameAsc(ACTIVE_FLAG).stream()
                .map(project -> OrganizationLookupOption.builder()
                        .id(project.getProjectId())
                        .label(project.getProjectName())
                        .code(project.getProjectCode())
                        .type(project.getProjectScopeType() == null ? null : project.getProjectScopeType().name())
                        .cellId(project.getCell() == null ? null : project.getCell().getCellId())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<OrganizationLookupOption> getCellOptions() {
        return cellMasterRepository
                .findByActiveFlagIgnoreCaseAndWing_ActiveFlagIgnoreCaseOrderByCellNameAsc(ACTIVE_FLAG, ACTIVE_FLAG)
                .stream()
                .map(cell -> OrganizationLookupOption.builder()
                        .id(cell.getCellId())
                        .label(cell.getCellName())
                        .code(cell.getWing() == null ? null : cell.getWing().getWingName())
                        .type("CELL")
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<OrganizationLookupOption> getTeamOptions(Long projectId, Long cellId) {
        List<TeamMasterEntity> teams;
        if (projectId != null) {
            ProjectMst project = resolveActiveProject(projectId);
            if (project.getCell() == null) {
                teams = teamRepository.findByProject_ProjectIdAndStatusOrderByDisplayOrderAscTeamNameAsc(
                        projectId,
                        OrganizationRecordStatus.ACTIVE);
            } else {
                teams = teamRepository.findByCell_CellIdAndStatusOrderByDisplayOrderAscTeamNameAsc(
                        project.getCell().getCellId(),
                        OrganizationRecordStatus.ACTIVE);
            }
        } else if (cellId != null) {
            teams = teamRepository.findByCell_CellIdAndStatusOrderByDisplayOrderAscTeamNameAsc(
                    cellId,
                    OrganizationRecordStatus.ACTIVE);
        } else {
            teams = teamRepository.findByStatusOrderByCell_CellNameAscDisplayOrderAscTeamNameAsc(
                    OrganizationRecordStatus.ACTIVE);
        }
        return teams.stream().map(this::toTeamOption).collect(Collectors.toList());
    }

    @Override
    public List<TeamByCellResponse> getActiveTeamsByCell(Long cellId) {
        CellMaster cell = resolveActiveCell(cellId);
        return teamRepository.findByCell_CellIdAndStatusOrderByDisplayOrderAscTeamNameAsc(
                cell.getCellId(),
                OrganizationRecordStatus.ACTIVE)
                .stream()
                .map(team -> new TeamByCellResponse(team.getTeamId(), team.getTeamName()))
                .collect(Collectors.toList());
    }

    @Override
    public List<OrganizationLookupOption> getPositionOptions(Long projectId, Long teamId) {
        return positionRepository.searchPositions(
                projectId,
                null,
                teamId,
                false,
                OrganizationRecordStatus.ACTIVE,
                null,
                Pageable.unpaged())
                .stream()
                .map(this::toPositionOption)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrganizationLookupOption> getDesignationOptions() {
        return designationRepository.findAll().stream()
                .filter(designation -> ACTIVE_FLAG.equalsIgnoreCase(designation.getActiveFlag()))
                .sorted((left, right) -> left.getDesignationName().compareToIgnoreCase(right.getDesignationName()))
                .map(designation -> OrganizationLookupOption.builder()
                        .id(designation.getDesignationId())
                        .label(designation.getDesignationName())
                        .code(designation.getCategory())
                        .type(designation.getRoleName())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<OrganizationLookupOption> getLevelOptions(Long designationId) {
        if (designationId != null) {
            ManpowerDesignationMaster designation = resolveActiveDesignation(designationId);
            return designation.getLevels().stream()
                    .filter(level -> ACTIVE_FLAG.equalsIgnoreCase(level.getActiveFlag()))
                    .sorted((left, right) -> left.getLevelCode().compareToIgnoreCase(right.getLevelCode()))
                    .map(this::toLevelOption)
                    .collect(Collectors.toList());
        }
        return levelRepository.findByActiveFlagIgnoreCase(ACTIVE_FLAG, Pageable.unpaged()).stream()
                .sorted((left, right) -> left.getLevelCode().compareToIgnoreCase(right.getLevelCode()))
                .map(this::toLevelOption)
                .collect(Collectors.toList());
    }

    @Override
    public Page<OrganizationLookupOption> getEmployeeOptions(
            Long positionId,
            Long designationId,
            String levelCode,
            String search,
            Pageable pageable) {
        Page<EmployeeEntity> employees;
        if (positionId != null) {
            PositionMasterEntity position = getPositionEntity(positionId);
            if (position.getDesignation() == null || position.getResourceLevel() == null) {
                return Page.empty(pageable);
            }
            designationId = position.getDesignation().getDesignationId();
            levelCode = position.getResourceLevel().getLevelCode();
        }
        if (designationId != null && StringUtils.hasText(levelCode)) {
            employees = employeeRepository.findActiveByDesignationAndLevelWithSearch(
                    designationId,
                    normalizeLevelCode(levelCode),
                    toEmployeeSearchPattern(search),
                    pageable);
        } else if (designationId == null && !StringUtils.hasText(levelCode)) {
            employees = employeeRepository.findActiveWithSearch(toEmployeeSearchPattern(search), pageable);
        } else {
            return Page.empty(pageable);
        }
        return employees.map(this::toEmployeeOption);
    }

    @Override
    public List<OrganizationAuditResponse> getAuditTimeline(String entityType, String entityId) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByOccurredAtDescAuditIdDesc(
                normalizeRequired(entityType, "Entity type").toUpperCase(Locale.ROOT),
                normalizeRequired(entityId, "Entity id"))
                .stream()
                .map(this::toAuditResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void seedSampleHierarchy(Long projectId) {
        ProjectMst project = resolveActiveProject(projectId);

        TeamMasterEntity development = upsertTeam(project, "Development Team", OrganizationTeamType.DEVELOPMENT, null, 10);
        TeamMasterEntity teamD15 = upsertTeam(project, "Team D-15", OrganizationTeamType.DEVELOPMENT, development, 15);
        TeamMasterEntity teamD16 = upsertTeam(project, "Team D-16", OrganizationTeamType.DEVELOPMENT, development, 16);
        TeamMasterEntity om = upsertTeam(project, "O&M Team", OrganizationTeamType.OM, null, 20);
        TeamMasterEntity teamO4 = upsertTeam(project, "Team O-4", OrganizationTeamType.OM, om, 24);
        TeamMasterEntity support = upsertTeam(project, "Support Team", OrganizationTeamType.SUPPORT, null, 30);
        TeamMasterEntity teamSqa = upsertTeam(project, "Team SQA", OrganizationTeamType.SUPPORT, support, 31);

        PositionMasterEntity stm = upsertPosition(project, null, "Senior Technical Manager", "Senior Technical Manager (STM)",
                null, null, 1);
        PositionMasterEntity pm = upsertPosition(project, null, "Project Manager", "Project Manager",
                null, stm, 2);
        PositionMasterEntity devLead = upsertPosition(project, development, "Development Project Lead", "Project Lead",
                null, pm, 10);
        PositionMasterEntity omLead = upsertPosition(project, om, "O&M Project Lead", "Project Lead",
                null, pm, 20);
        PositionMasterEntity d15Lead = upsertPosition(project, teamD15, "Team D-15 Lead", "SSD",
                "Gajanan Thakare", devLead, 151);
        upsertPosition(project, teamD15, "Team D-15 Developer 1", "SD", null, d15Lead, 152);
        upsertPosition(project, teamD15, "Team D-15 Developer 2", "SD", null, d15Lead, 153);
        PositionMasterEntity d16Lead = upsertPosition(project, teamD16, "Team D-16 Lead", "SSD", null, devLead, 161);
        upsertPosition(project, teamD16, "Team D-16 Developer", "SD", null, d16Lead, 162);
        PositionMasterEntity o4Member = upsertPosition(project, teamO4, "Team O-4 Developer", "SD",
                "Kiran Jadhav", omLead, 241);
        upsertPosition(project, teamO4, "Team O-4 Developer 2", "SD", null, o4Member, 242);
        PositionMasterEntity qaManager = upsertPosition(project, teamSqa, "Team SQA Manager", "QM",
                "Mallikarjun Kopuri", pm, 311);
        upsertPosition(project, teamSqa, "Team SQA Lead", "QAL", null, qaManager, 312);

        log(OrganizationAuditAction.POSITION_UPDATED, "PROJECT", project.getProjectId(),
                "Sample hierarchy generated", project.getProjectName());
    }

    private void mapTeamRequest(
            TeamRequest request,
            TeamMasterEntity team,
            ProjectMst project,
            CellMaster cell,
            String teamName) {
        team.setTeamName(teamName);
        team.setTeamType(request.getTeamType());
        team.setProject(project);
        team.setCell(cell);
        team.setDisplayOrder(request.getDisplayOrder() == null ? 0 : request.getDisplayOrder());
        team.setStatus(request.getStatus() == null ? OrganizationRecordStatus.ACTIVE : request.getStatus());
        team.setParentTeam(resolveParentTeam(request.getParentTeamId(), cell.getCellId(), team.getTeamId()));
    }

    private void mapPositionRequest(
            PositionRequest request,
            PositionMasterEntity position,
            CellMaster cell,
            TeamMasterEntity team) {
        ManpowerDesignationMaster designation = resolveActiveDesignation(request.getDesignationId());
        position.setPositionName(normalizeRequired(request.getPositionName(), "Position name"));
        position.setProject(team.getProject());
        position.setCell(cell);
        position.setTeam(team);
        position.setDesignation(designation);
        position.setResourceLevel(resolvePositionLevel(request.getLevelCode(), designation));
        position.setReportingPosition(resolveReportingPosition(
                request.getReportingPositionId(),
                cell,
                team,
                position.getPositionId()));
        position.setEmployee(resolveEmployee(request.getEmployeeId()));
        ensureEmployeeMatchesPosition(position.getEmployee(), position);
        position.setDisplayOrder(request.getDisplayOrder() == null ? 0 : request.getDisplayOrder());
        position.setStatus(request.getStatus() == null ? OrganizationRecordStatus.ACTIVE : request.getStatus());
        position.setPositionStatus(position.getEmployee() == null ? PositionStatus.VACANT : PositionStatus.FILLED);
    }

    private void mapMappingRequest(EmployeeTeamMappingRequest request, EmployeeTeamMappingEntity mapping) {
        TeamMasterEntity team = resolveTeamRequired(request.getTeamId());
        PositionMasterEntity position = getPositionEntity(request.getPositionId());
        CellMaster positionCell = resolvePositionCell(position);
        if (positionCell != null && team.getCell() != null
                && !team.getCell().getCellId().equals(positionCell.getCellId())) {
            throw new BusinessValidationException("Team cell must match the position cell.");
        }
        EmployeeEntity employee = resolveEmployee(request.getEmployeeId());
        ensureEmployeeMatchesPosition(employee, position);
        ensureEmployeeAvailable(employee, position.getPositionId());
        mapping.setEmployee(employee);
        mapping.setTeam(team);
        mapping.setPosition(position);
        mapping.setEffectiveDate(request.getEffectiveDate() == null ? LocalDate.now() : request.getEffectiveDate());
        mapping.setStatus(request.getStatus() == null ? OrganizationRecordStatus.ACTIVE : request.getStatus());
    }

    private ProjectMst resolveActiveProject(Long projectId) {
        if (projectId == null) {
            throw new BusinessValidationException("Project is required.");
        }
        ProjectMst project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found for id: " + projectId));
        if (!ACTIVE_FLAG.equalsIgnoreCase(project.getActiveFlag())) {
            throw new BusinessValidationException("Selected project is inactive.");
        }
        return project;
    }

    private ProjectMst resolveOptionalActiveProject(Long projectId) {
        if (projectId == null) {
            return null;
        }
        return resolveActiveProject(projectId);
    }

    private ProjectMst resolveOptionalActiveProject(Long projectId, CellMaster cell) {
        if (projectId == null) {
            return null;
        }
        ProjectMst project = resolveOptionalActiveProject(projectId);
        if (project.getCell() == null || !cell.getCellId().equals(project.getCell().getCellId())) {
            throw new BusinessValidationException("Selected project must belong to the selected cell.");
        }
        return project;
    }

    private CellMaster resolveActiveCell(Long cellId) {
        if (cellId == null) {
            throw new BusinessValidationException("Cell is required.");
        }
        CellMaster cell = cellMasterRepository.findByCellId(cellId)
                .orElseThrow(() -> new ResourceNotFoundException("Cell not found for id: " + cellId));
        if (!ACTIVE_FLAG.equalsIgnoreCase(cell.getActiveFlag())) {
            throw new BusinessValidationException("Selected cell is inactive.");
        }
        if (cell.getWing() == null || !ACTIVE_FLAG.equalsIgnoreCase(cell.getWing().getActiveFlag())) {
            throw new BusinessValidationException("Selected cell belongs to an inactive wing.");
        }
        return cell;
    }

    private TeamMasterEntity resolveParentTeam(Long parentTeamId, Long cellId, Long currentTeamId) {
        if (parentTeamId == null) {
            return null;
        }
        if (currentTeamId != null && currentTeamId.equals(parentTeamId)) {
            throw new BusinessValidationException("A team cannot be its own parent.");
        }
        TeamMasterEntity parent = getTeamEntity(parentTeamId);
        if (parent.getCell() == null || !parent.getCell().getCellId().equals(cellId)) {
            throw new BusinessValidationException("Parent team must belong to the same cell.");
        }
        if (parent.getStatus() != OrganizationRecordStatus.ACTIVE) {
            throw new BusinessValidationException("Parent team is inactive.");
        }
        return parent;
    }

    private TeamMasterEntity resolveTeamRequired(Long teamId) {
        if (teamId == null) {
            throw new BusinessValidationException("Team is required.");
        }
        TeamMasterEntity team = getTeamEntity(teamId);
        if (team.getStatus() != OrganizationRecordStatus.ACTIVE) {
            throw new BusinessValidationException("Selected team is inactive.");
        }
        return team;
    }

    private TeamMasterEntity resolveTeamRequired(Long teamId, CellMaster cell) {
        TeamMasterEntity team = resolveTeamRequired(teamId);
        if (team.getCell() == null || !cell.getCellId().equals(team.getCell().getCellId())) {
            throw new BusinessValidationException("Selected team must belong to the selected cell.");
        }
        return team;
    }

    private CellMaster resolvePositionCell(PositionMasterEntity position) {
        if (position == null) {
            return null;
        }
        if (position.getCell() != null) {
            return position.getCell();
        }
        if (position.getProject() != null && position.getProject().getCell() != null) {
            return position.getProject().getCell();
        }
        if (position.getTeam() != null) {
            return position.getTeam().getCell();
        }
        return null;
    }

    private PositionMasterEntity resolveReportingPosition(
            Long reportingPositionId,
            CellMaster cell,
            TeamMasterEntity team,
            Long currentPositionId) {
        if (reportingPositionId == null) {
            return null;
        }
        if (currentPositionId != null && currentPositionId.equals(reportingPositionId)) {
            throw new BusinessValidationException("A position cannot report to itself.");
        }
        PositionMasterEntity reporting = getPositionEntity(reportingPositionId);
        CellMaster reportingCell = resolvePositionCell(reporting);
        if (reportingCell == null || !cell.getCellId().equals(reportingCell.getCellId())) {
            throw new BusinessValidationException("Reporting position must belong to the same cell.");
        }
        if (reporting.getTeam() != null && team != null
                && !reporting.getTeam().getTeamId().equals(team.getTeamId())) {
            throw new BusinessValidationException("Reporting position must belong to the selected team.");
        }
        if (reporting.getStatus() != OrganizationRecordStatus.ACTIVE) {
            throw new BusinessValidationException("Reporting position is inactive.");
        }
        return reporting;
    }

    private ManpowerDesignationMaster resolveActiveDesignation(Long designationId) {
        if (designationId == null) {
            throw new BusinessValidationException("Designation is required.");
        }
        return designationRepository.findByDesignationIdAndActiveFlagIgnoreCase(designationId, ACTIVE_FLAG)
                .orElseThrow(() -> new ResourceNotFoundException("Active designation not found for id: " + designationId));
    }

    private ResourceLevelExperience resolvePositionLevel(String levelCode, ManpowerDesignationMaster designation) {
        if (!StringUtils.hasText(levelCode)) {
            return null;
        }
        String normalizedLevelCode = levelCode.trim().toUpperCase(Locale.ROOT);
        ResourceLevelExperience level = levelRepository
                .findByLevelCodeIgnoreCaseAndActiveFlagIgnoreCase(normalizedLevelCode, ACTIVE_FLAG)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Active resource level not found for code: " + normalizedLevelCode));

        if (designation.getLevels() != null && !designation.getLevels().isEmpty()) {
            boolean allowedForDesignation = designation.getLevels().stream()
                    .filter(mappedLevel -> ACTIVE_FLAG.equalsIgnoreCase(mappedLevel.getActiveFlag()))
                    .anyMatch(mappedLevel -> mappedLevel.getLevelCode().equalsIgnoreCase(level.getLevelCode()));
            if (!allowedForDesignation) {
                throw new BusinessValidationException("Selected level is not mapped to selected designation.");
            }
        }
        return level;
    }

    private EmployeeEntity resolveEmployee(Long employeeId) {
        if (employeeId == null) {
            return null;
        }
        EmployeeEntity employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found for id: " + employeeId));
        if (!ACTIVE_EMPLOYEE_STATUS.equalsIgnoreCase(employee.getStatus())) {
            throw new BusinessValidationException("Selected employee is not active.");
        }
        return employee;
    }

    private void ensureEmployeeAvailable(EmployeeEntity employee, Long currentPositionId) {
        if (employee == null || employee.getEmployeeId() == null) {
            return;
        }
        List<PositionMasterEntity> filledPositions = positionRepository.findByEmployee_EmployeeIdAndStatusAndPositionStatus(
                employee.getEmployeeId(),
                OrganizationRecordStatus.ACTIVE,
                PositionStatus.FILLED);
        boolean assignedElsewhere = filledPositions.stream()
                .anyMatch(position -> currentPositionId == null || !currentPositionId.equals(position.getPositionId()));
        if (assignedElsewhere) {
            throw new BusinessValidationException("Employee is already assigned to another active position.");
        }
    }

    private void ensureEmployeeMatchesPosition(EmployeeEntity employee, PositionMasterEntity position) {
        if (employee == null) {
            return;
        }
        Long employeeDesignationId = employee.getDesignation() == null
                ? null
                : employee.getDesignation().getDesignationId();
        Long positionDesignationId = position.getDesignation() == null
                ? null
                : position.getDesignation().getDesignationId();
        String employeeLevelCode = normalizeLevelCode(employee.getLevelCode());
        String positionLevelCode = position.getResourceLevel() == null
                ? null
                : normalizeLevelCode(position.getResourceLevel().getLevelCode());
        if (!equalsLong(employeeDesignationId, positionDesignationId)
                || !java.util.Objects.equals(employeeLevelCode, positionLevelCode)) {
            throw new BusinessValidationException(
                    "Employee designation and level must match the selected position.");
        }
    }

    private void ensureUniqueTeam(Long cellId, String teamName, Long excludeId) {
        if (teamRepository.existsDuplicateTeam(cellId, teamName, excludeId)) {
            throw new DuplicateResourceException("Team already exists in this cell: " + teamName);
        }
    }

    private void ensureNoTeamCycle(TeamMasterEntity team) {
        TeamMasterEntity cursor = team.getParentTeam();
        while (cursor != null) {
            if (team.getTeamId() != null && team.getTeamId().equals(cursor.getTeamId())) {
                throw new BusinessValidationException("Team hierarchy cannot contain a cycle.");
            }
            cursor = cursor.getParentTeam();
        }
    }

    private void ensureNoPositionCycle(PositionMasterEntity position) {
        PositionMasterEntity cursor = position.getReportingPosition();
        while (cursor != null) {
            if (position.getPositionId() != null && position.getPositionId().equals(cursor.getPositionId())) {
                throw new BusinessValidationException("Reporting hierarchy cannot contain a cycle.");
            }
            cursor = cursor.getReportingPosition();
        }
    }

    private void closeOtherActiveMapping(EmployeeTeamMappingEntity mapping) {
        if (mapping.getStatus() != OrganizationRecordStatus.ACTIVE || mapping.getPosition() == null) {
            return;
        }
        Optional<EmployeeTeamMappingEntity> current = mappingRepository
                .findFirstByPosition_PositionIdAndStatusOrderByEffectiveDateDescMappingIdDesc(
                        mapping.getPosition().getPositionId(),
                        OrganizationRecordStatus.ACTIVE);
        current.filter(existing -> !equalsLong(existing.getMappingId(), mapping.getMappingId()))
                .ifPresent(existing -> {
                    existing.setStatus(OrganizationRecordStatus.INACTIVE);
                    mappingRepository.save(existing);
                });
    }

    private void applyMappingToPosition(EmployeeTeamMappingEntity mapping) {
        if (mapping.getStatus() != OrganizationRecordStatus.ACTIVE) {
            return;
        }
        PositionMasterEntity position = mapping.getPosition();
        position.setCell(mapping.getTeam().getCell());
        position.setProject(mapping.getTeam().getProject());
        position.setTeam(mapping.getTeam());
        position.setEmployee(mapping.getEmployee());
        position.setPositionStatus(mapping.getEmployee() == null ? PositionStatus.VACANT : PositionStatus.FILLED);
        positionRepository.save(position);
        if (mapping.getEmployee() == null) {
            log(OrganizationAuditAction.VACANCY_CREATED, "POSITION", position.getPositionId(),
                    "Vacancy created", position.getPositionName());
        } else {
            log(OrganizationAuditAction.VACANCY_CLOSED, "POSITION", position.getPositionId(),
                    "Vacancy closed", employeeLabel(mapping.getEmployee()));
        }
    }

    private void syncActiveMappingForPosition(PositionMasterEntity position, boolean assignmentChanged) {
        if (position.getTeam() == null || position.getStatus() != OrganizationRecordStatus.ACTIVE) {
            closeActiveMapping(position);
            return;
        }

        Optional<EmployeeTeamMappingEntity> existing = mappingRepository
                .findFirstByPosition_PositionIdAndStatusOrderByEffectiveDateDescMappingIdDesc(
                        position.getPositionId(),
                        OrganizationRecordStatus.ACTIVE);
        if (position.getEmployee() == null) {
            existing.ifPresent(mapping -> {
                mapping.setStatus(OrganizationRecordStatus.INACTIVE);
                mappingRepository.save(mapping);
            });
            if (assignmentChanged) {
                log(OrganizationAuditAction.VACANCY_CREATED, "POSITION", position.getPositionId(),
                        "Vacancy created", position.getPositionName());
            }
            return;
        }

        EmployeeTeamMappingEntity mapping = existing.orElseGet(EmployeeTeamMappingEntity::new);
        mapping.setEmployee(position.getEmployee());
        mapping.setTeam(position.getTeam());
        mapping.setPosition(position);
        mapping.setEffectiveDate(mapping.getEffectiveDate() == null ? LocalDate.now() : mapping.getEffectiveDate());
        mapping.setStatus(OrganizationRecordStatus.ACTIVE);
        mappingRepository.save(mapping);
        if (assignmentChanged) {
            log(OrganizationAuditAction.VACANCY_CLOSED, "POSITION", position.getPositionId(),
                    "Vacancy closed", employeeLabel(position.getEmployee()));
        }
    }

    private void closeActiveMapping(PositionMasterEntity position) {
        if (position == null || position.getPositionId() == null) {
            return;
        }
        mappingRepository.findFirstByPosition_PositionIdAndStatusOrderByEffectiveDateDescMappingIdDesc(
                position.getPositionId(),
                OrganizationRecordStatus.ACTIVE)
                .ifPresent(mapping -> {
                    mapping.setStatus(OrganizationRecordStatus.INACTIVE);
                    mappingRepository.save(mapping);
                });
    }

    private TeamMasterEntity getTeamEntity(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found for id: " + teamId));
    }

    private PositionMasterEntity getPositionEntity(Long positionId) {
        return positionRepository.findById(positionId)
                .orElseThrow(() -> new ResourceNotFoundException("Position not found for id: " + positionId));
    }

    private EmployeeTeamMappingEntity getMappingEntity(Long mappingId) {
        return mappingRepository.findById(mappingId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee team mapping not found for id: " + mappingId));
    }

    private TeamResponse toTeamResponse(TeamMasterEntity team) {
        return TeamResponse.builder()
                .teamId(team.getTeamId())
                .teamName(team.getTeamName())
                .teamType(team.getTeamType())
                .parentTeamId(team.getParentTeam() == null ? null : team.getParentTeam().getTeamId())
                .parentTeamName(team.getParentTeam() == null ? null : team.getParentTeam().getTeamName())
                .projectId(team.getProject() == null ? null : team.getProject().getProjectId())
                .projectName(team.getProject() == null ? null : team.getProject().getProjectName())
                .projectCode(team.getProject() == null ? null : team.getProject().getProjectCode())
                .cellId(team.getCell() == null ? null : team.getCell().getCellId())
                .cellName(team.getCell() == null ? null : team.getCell().getCellName())
                .wingName(team.getCell() == null || team.getCell().getWing() == null
                        ? null
                        : team.getCell().getWing().getWingName())
                .displayOrder(team.getDisplayOrder())
                .status(team.getStatus())
                .positionCount(team.getTeamId() == null ? 0 : positionRepository.countByTeam_TeamId(team.getTeamId()))
                .createdAt(team.getCreatedDateTime())
                .updatedAt(team.getUpdatedDateTime())
                .build();
    }

    private PositionResponse toPositionResponse(PositionMasterEntity position) {
        String designationName = position.getDesignation() == null ? null : position.getDesignation().getDesignationName();
        ResourceLevelExperience level = position.getResourceLevel();
        String designationDisplay = designationWithLevel(designationName, level);
        CellMaster cell = resolvePositionCell(position);
        EmployeeEntity employee = position.getEmployee();
        return PositionResponse.builder()
                .positionId(position.getPositionId())
                .positionName(position.getPositionName())
                .projectId(position.getProject() == null ? null : position.getProject().getProjectId())
                .projectName(position.getProject() == null ? null : position.getProject().getProjectName())
                .projectCode(position.getProject() == null ? null : position.getProject().getProjectCode())
                .teamId(position.getTeam() == null ? null : position.getTeam().getTeamId())
                .teamName(position.getTeam() == null ? null : position.getTeam().getTeamName())
                .cellId(cell == null ? null : cell.getCellId())
                .cellName(cell == null ? null : cell.getCellName())
                .designationId(position.getDesignation() == null ? null : position.getDesignation().getDesignationId())
                .designationName(designationName)
                .levelCode(level == null ? null : level.getLevelCode())
                .levelName(level == null ? null : level.getLevelName())
                .reportingPositionId(position.getReportingPosition() == null
                        ? null
                        : position.getReportingPosition().getPositionId())
                .reportingPositionName(position.getReportingPosition() == null
                        ? null
                        : position.getReportingPosition().getPositionName())
                .employeeId(employee == null ? null : employee.getEmployeeId())
                .employeeCode(employee == null ? null : employee.getEmployeeCode())
                .employeeName(employee == null ? null : employee.getFullName())
                .displayName(employee == null
                        ? vacantLabel(designationDisplay)
                        : employee.getFullName() + " - " + designationDisplay)
                .displayOrder(position.getDisplayOrder())
                .positionStatus(position.getPositionStatus())
                .status(position.getStatus())
                .createdAt(position.getCreatedDateTime())
                .updatedAt(position.getUpdatedDateTime())
                .build();
    }

    private EmployeeTeamMappingResponse toMappingResponse(EmployeeTeamMappingEntity mapping) {
        EmployeeEntity employee = mapping.getEmployee();
        PositionMasterEntity position = mapping.getPosition();
        TeamMasterEntity team = mapping.getTeam();
        ProjectMst project = position != null && position.getProject() != null
                ? position.getProject()
                : team == null ? null : team.getProject();
        CellMaster cell = position == null
                ? team == null ? null : team.getCell()
                : resolvePositionCell(position);
        return EmployeeTeamMappingResponse.builder()
                .mappingId(mapping.getMappingId())
                .employeeId(employee == null ? null : employee.getEmployeeId())
                .employeeCode(employee == null ? null : employee.getEmployeeCode())
                .employeeName(employee == null ? null : employee.getFullName())
                .teamId(team == null ? null : team.getTeamId())
                .teamName(team == null ? null : team.getTeamName())
                .projectId(project == null ? null : project.getProjectId())
                .projectName(project == null ? null : project.getProjectName())
                .cellId(cell == null ? null : cell.getCellId())
                .cellName(cell == null ? null : cell.getCellName())
                .positionId(position == null ? null : position.getPositionId())
                .positionName(position == null ? null : position.getPositionName())
                .designationName(position == null || position.getDesignation() == null
                        ? null
                        : position.getDesignation().getDesignationName())
                .levelCode(position == null || position.getResourceLevel() == null
                        ? null
                        : position.getResourceLevel().getLevelCode())
                .levelName(position == null || position.getResourceLevel() == null
                        ? null
                        : position.getResourceLevel().getLevelName())
                .effectiveDate(mapping.getEffectiveDate())
                .status(mapping.getStatus())
                .createdAt(mapping.getCreatedDateTime())
                .updatedAt(mapping.getUpdatedDateTime())
                .build();
    }

    private OrganizationAuditResponse toAuditResponse(OrganizationAuditLogEntity entity) {
        return OrganizationAuditResponse.builder()
                .auditId(entity.getAuditId())
                .actionType(entity.getActionType())
                .entityType(entity.getEntityType())
                .entityId(entity.getEntityId())
                .actorLoginId(entity.getActorLoginId())
                .summary(entity.getSummary())
                .details(entity.getDetails())
                .occurredAt(entity.getOccurredAt())
                .build();
    }

    private OrganizationLookupOption toTeamOption(TeamMasterEntity team) {
        return OrganizationLookupOption.builder()
                .id(team.getTeamId())
                .label(team.getTeamName())
                .code(team.getCell() == null ? null : team.getCell().getCellName())
                .type(team.getTeamType() == null ? null : team.getTeamType().name())
                .build();
    }

    private OrganizationLookupOption toPositionOption(PositionMasterEntity position) {
        ResourceLevelExperience level = position.getResourceLevel();
        return OrganizationLookupOption.builder()
                .id(position.getPositionId())
                .label(position.getPositionName()
                        + (level == null ? "" : " - " + level.getLevelCode()))
                .code(position.getEmployee() == null ? "VACANT" : position.getEmployee().getEmployeeCode())
                .type(position.getDesignation() == null ? null : position.getDesignation().getDesignationName())
                .build();
    }

    private OrganizationLookupOption toLevelOption(ResourceLevelExperience level) {
        return OrganizationLookupOption.builder()
                .id(level.getLevelId())
                .label(level.getLevelName())
                .code(level.getLevelCode())
                .type("LEVEL")
                .build();
    }

    private TeamMasterEntity upsertTeam(
            ProjectMst project,
            String teamName,
            OrganizationTeamType teamType,
            TeamMasterEntity parent,
            int order) {
        TeamMasterEntity team = teamRepository
                .findFirstByCell_CellIdAndTeamNameIgnoreCase(project.getCell().getCellId(), teamName)
                .orElseGet(TeamMasterEntity::new);
        team.setProject(project);
        team.setCell(project.getCell());
        team.setTeamName(teamName);
        team.setTeamType(teamType);
        team.setParentTeam(parent);
        team.setDisplayOrder(order);
        team.setStatus(OrganizationRecordStatus.ACTIVE);
        return teamRepository.save(team);
    }

    private PositionMasterEntity upsertPosition(
            ProjectMst project,
            TeamMasterEntity team,
            String positionName,
            String designationName,
            String employeeName,
            PositionMasterEntity reportingPosition,
            int order) {
        PositionMasterEntity position = positionRepository
                .findFirstByProject_ProjectIdAndPositionNameIgnoreCase(project.getProjectId(), positionName)
                .orElseGet(PositionMasterEntity::new);
        position.setProject(project);
        position.setCell(project.getCell());
        position.setTeam(team);
        position.setPositionName(positionName);
        position.setDesignation(upsertDesignation(designationName));
        position.setEmployee(resolveSampleEmployee(employeeName));
        position.setReportingPosition(reportingPosition);
        position.setDisplayOrder(order);
        position.setStatus(OrganizationRecordStatus.ACTIVE);
        position.setPositionStatus(position.getEmployee() == null ? PositionStatus.VACANT : PositionStatus.FILLED);
        PositionMasterEntity saved = positionRepository.save(position);
        syncActiveMappingForPosition(saved, true);
        return saved;
    }

    private ManpowerDesignationMaster upsertDesignation(String designationName) {
        return designationRepository
                .findFirstByDesignationNameIgnoreCaseAndActiveFlagIgnoreCase(designationName, ACTIVE_FLAG)
                .orElseGet(() -> {
                    ManpowerDesignationMaster designation = new ManpowerDesignationMaster();
                    designation.setCategory(SAMPLE_CATEGORY);
                    designation.setDesignationName(designationName);
                    designation.setRoleName(designationName);
                    designation.setActiveFlag(ACTIVE_FLAG);
                    return designationRepository.save(designation);
                });
    }

    private EmployeeEntity resolveSampleEmployee(String employeeName) {
        if (!StringUtils.hasText(employeeName)) {
            return null;
        }
        List<EmployeeEntity> employees = employeeRepository.findByFullNameIgnoreCaseAndStatusIgnoreCase(
                employeeName.trim(),
                ACTIVE_EMPLOYEE_STATUS);
        return employees.isEmpty() ? null : employees.get(0);
    }

    private void logPositionCreate(PositionMasterEntity position) {
        log(OrganizationAuditAction.POSITION_CREATED, "POSITION", position.getPositionId(),
                "Position created", position.getPositionName());
        if (position.getEmployee() == null) {
            log(OrganizationAuditAction.VACANCY_CREATED, "POSITION", position.getPositionId(),
                    "Vacancy created", position.getPositionName());
        }
    }

    private void logPositionUpdate(PositionMasterEntity position, PositionStatus previousStatus, Long previousEmployeeId) {
        log(OrganizationAuditAction.POSITION_UPDATED, "POSITION", position.getPositionId(),
                "Position updated", position.getPositionName());
        Long currentEmployeeId = position.getEmployee() == null ? null : position.getEmployee().getEmployeeId();
        if (!equalsLong(previousEmployeeId, currentEmployeeId)) {
            log(OrganizationAuditAction.TEAM_ASSIGNMENT_CHANGED, "POSITION", position.getPositionId(),
                    "Position employee assignment changed", position.getPositionName());
        }
        if (previousStatus == PositionStatus.FILLED && position.getPositionStatus() == PositionStatus.VACANT) {
            log(OrganizationAuditAction.VACANCY_CREATED, "POSITION", position.getPositionId(),
                    "Vacancy created", position.getPositionName());
        }
        if (previousStatus == PositionStatus.VACANT && position.getPositionStatus() == PositionStatus.FILLED) {
            log(OrganizationAuditAction.VACANCY_CLOSED, "POSITION", position.getPositionId(),
                    "Vacancy closed", employeeLabel(position.getEmployee()));
        }
    }

    private void log(OrganizationAuditAction action, String entityType, Object entityId, String summary, String details) {
        OrganizationAuditLogEntity log = new OrganizationAuditLogEntity();
        log.setActionType(action);
        log.setEntityType(entityType == null ? "UNKNOWN" : entityType.toUpperCase(Locale.ROOT));
        log.setEntityId(String.valueOf(entityId));
        log.setActorLoginId(resolveActorLoginId());
        log.setSummary(summary);
        log.setDetails(details);
        auditLogRepository.save(log);
    }

    private String resolveActorLoginId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !StringUtils.hasText(authentication.getName())) {
            return "SYSTEM";
        }
        return authentication.getName();
    }

    private String normalizeRequired(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessValidationException(fieldName + " is required.");
        }
        return value.trim();
    }

    private String toSearchPattern(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return "%" + value.trim().toLowerCase(Locale.ROOT) + "%";
    }

    private String toEmployeeSearchPattern(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return "%" + value.trim().toLowerCase(Locale.ROOT) + "%";
    }

    private String normalizeLevelCode(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }

    private OrganizationLookupOption toEmployeeOption(EmployeeEntity employee) {
        return OrganizationLookupOption.builder()
                .id(employee.getEmployeeId())
                .label(employee.getFullName())
                .code(employee.getEmployeeCode())
                .type(employee.getDesignation() == null ? null : employee.getDesignation().getDesignationName())
                .build();
    }

    private String vacantLabel(String designationName) {
        return "Vacant - " + (StringUtils.hasText(designationName) ? designationName : "Designation");
    }

    private String designationWithLevel(String designationName, ResourceLevelExperience level) {
        String label = StringUtils.hasText(designationName) ? designationName : "Designation";
        if (level == null || !StringUtils.hasText(level.getLevelCode())) {
            return label;
        }
        return label + " (" + level.getLevelCode() + ")";
    }

    private String employeeLabel(EmployeeEntity employee) {
        if (employee == null) {
            return "Vacant";
        }
        if (StringUtils.hasText(employee.getEmployeeCode())) {
            return employee.getFullName() + " (" + employee.getEmployeeCode() + ")";
        }
        return employee.getFullName();
    }

    private String buildMappingDetails(EmployeeTeamMappingEntity mapping) {
        List<String> parts = new ArrayList<>();
        parts.add("Team: " + (mapping.getTeam() == null ? "-" : mapping.getTeam().getTeamName()));
        parts.add("Position: " + (mapping.getPosition() == null ? "-" : mapping.getPosition().getPositionName()));
        parts.add("Employee: " + employeeLabel(mapping.getEmployee()));
        parts.add("Effective Date: " + mapping.getEffectiveDate());
        return String.join(", ", parts);
    }

    private boolean sameEmployee(EmployeeEntity left, EmployeeEntity right) {
        Long leftId = left == null ? null : left.getEmployeeId();
        Long rightId = right == null ? null : right.getEmployeeId();
        return equalsLong(leftId, rightId);
    }

    private boolean equalsLong(Long left, Long right) {
        return left == null ? right == null : left.equals(right);
    }
}
