package com.maharecruitment.gov.in.recruitment.service.organization.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.master.entity.CellMaster;
import com.maharecruitment.gov.in.master.entity.ProjectMst;
import com.maharecruitment.gov.in.master.entity.ResourceLevelExperience;
import com.maharecruitment.gov.in.master.exception.BusinessValidationException;
import com.maharecruitment.gov.in.master.exception.ResourceNotFoundException;
import com.maharecruitment.gov.in.master.repository.CellMasterRepository;
import com.maharecruitment.gov.in.master.repository.ProjectMstRepository;
import com.maharecruitment.gov.in.recruitment.dto.organization.OrganizationChartNodeResponse;
import com.maharecruitment.gov.in.recruitment.dto.organization.OrganizationDashboardResponse;
import com.maharecruitment.gov.in.recruitment.dto.organization.OrganizationSearchResult;
import com.maharecruitment.gov.in.recruitment.dto.organization.TeamStrengthResponse;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.entity.organization.OrganizationRecordStatus;
import com.maharecruitment.gov.in.recruitment.entity.organization.PositionMasterEntity;
import com.maharecruitment.gov.in.recruitment.entity.organization.PositionStatus;
import com.maharecruitment.gov.in.recruitment.entity.organization.TeamMasterEntity;
import com.maharecruitment.gov.in.recruitment.repository.organization.OrganizationTeamRepository;
import com.maharecruitment.gov.in.recruitment.repository.organization.PositionMasterRepository;
import com.maharecruitment.gov.in.recruitment.repository.organization.TeamStrengthProjection;
import com.maharecruitment.gov.in.recruitment.service.organization.OrganizationHierarchyService;

@Service
@Transactional(readOnly = true)
public class OrganizationHierarchyServiceImpl implements OrganizationHierarchyService {

    private static final String ACTIVE_FLAG = "Y";
    private static final String CELL_NODE = "CELL";
    private static final String PROJECT_NODE = "PROJECT";
    private static final String ROOT_NODE = "ROOT";
    private static final String TEAM_NODE = "TEAM";
    private static final String POSITION_NODE = "POSITION";

    private final ProjectMstRepository projectRepository;
    private final CellMasterRepository cellRepository;
    private final OrganizationTeamRepository teamRepository;
    private final PositionMasterRepository positionRepository;

    public OrganizationHierarchyServiceImpl(
            ProjectMstRepository projectRepository,
            CellMasterRepository cellRepository,
            OrganizationTeamRepository teamRepository,
            PositionMasterRepository positionRepository) {
        this.projectRepository = projectRepository;
        this.cellRepository = cellRepository;
        this.teamRepository = teamRepository;
        this.positionRepository = positionRepository;
    }

    @Override
    public OrganizationDashboardResponse getDashboard(Long projectId, Long cellId) {
        long totalPositions;
        long filledPositions;
        long vacantPositions;
        List<TeamStrengthResponse> teamStrength;

        if (cellId != null) {
            CellMaster cell = getCell(cellId);
            totalPositions = positionRepository.countByCell_CellIdAndStatus(
                    cell.getCellId(),
                    OrganizationRecordStatus.ACTIVE);
            filledPositions = positionRepository.countByCell_CellIdAndStatusAndPositionStatus(
                    cell.getCellId(),
                    OrganizationRecordStatus.ACTIVE,
                    PositionStatus.FILLED);
            vacantPositions = totalPositions - filledPositions;
            teamStrength = positionRepository
                    .getTeamStrengthByCell(
                            cell.getCellId(),
                            OrganizationRecordStatus.ACTIVE,
                            PositionStatus.FILLED,
                            PositionStatus.VACANT)
                    .stream()
                    .map(this::toTeamStrength)
                    .collect(Collectors.toList());
        } else if (projectId == null) {
            totalPositions = positionRepository.countByStatus(OrganizationRecordStatus.ACTIVE);
            filledPositions = positionRepository.countByStatusAndPositionStatus(
                    OrganizationRecordStatus.ACTIVE,
                    PositionStatus.FILLED);
            vacantPositions = totalPositions - filledPositions;
            teamStrength = positionRepository
                    .getTeamStrength(
                            null,
                            OrganizationRecordStatus.ACTIVE,
                            PositionStatus.FILLED,
                            PositionStatus.VACANT)
                    .stream()
                    .map(this::toTeamStrength)
                    .collect(Collectors.toList());
        } else {
            ProjectMst project = getProject(projectId);
            List<PositionMasterEntity> activePositions = positionRepository
                    .findByProjectScopeAndStatusOrderByDisplayOrderAscPositionIdAsc(
                            projectId,
                            project.getCell() == null ? null : project.getCell().getCellId(),
                            OrganizationRecordStatus.ACTIVE);
            totalPositions = activePositions.size();
            filledPositions = activePositions.stream()
                    .filter(position -> position.getPositionStatus() == PositionStatus.FILLED)
                    .count();
            vacantPositions = totalPositions - filledPositions;
            teamStrength = positionRepository
                    .getTeamStrength(
                            projectId,
                            OrganizationRecordStatus.ACTIVE,
                            PositionStatus.FILLED,
                            PositionStatus.VACANT)
                    .stream()
                    .map(this::toTeamStrength)
                    .collect(Collectors.toList());
        }

        return OrganizationDashboardResponse.builder()
                .totalPositions(totalPositions)
                .filledPositions(filledPositions)
                .vacantPositions(vacantPositions)
                .teamStrength(teamStrength)
                .build();
    }

    @Override
    public OrganizationChartNodeResponse getTree(Long projectId, Long cellId) {
        return buildHierarchy(projectId, cellId, false);
    }

    @Override
    public OrganizationChartNodeResponse getOrganizationChart(Long projectId, Long cellId) {
        return buildHierarchy(projectId, cellId, true);
    }

    @Override
    public List<OrganizationSearchResult> search(Long projectId, Long cellId, String search) {
        if (!StringUtils.hasText(search)) {
            throw new BusinessValidationException("Search text is required.");
        }
        String searchPattern = "%" + search.trim().toLowerCase() + "%";

        List<OrganizationSearchResult> results = new ArrayList<>();
        positionRepository.searchHierarchyPositions(
                projectId,
                cellId,
                OrganizationRecordStatus.ACTIVE,
                searchPattern,
                PageRequest.of(0, 50))
                .forEach(position -> results.add(toPositionSearchResult(position)));

        teamRepository.searchTeams(
                projectId,
                cellId,
                false,
                OrganizationRecordStatus.ACTIVE,
                searchPattern,
                PageRequest.of(0, 50))
                .forEach(team -> results.add(toTeamSearchResult(team)));

        return results.stream()
                .sorted(Comparator.comparing(OrganizationSearchResult::getCellName, nullSafeStringComparator())
                        .thenComparing(OrganizationSearchResult::getResultType, nullSafeStringComparator())
                        .thenComparing(OrganizationSearchResult::getTitle, nullSafeStringComparator()))
                .limit(50)
                .collect(Collectors.toList());
    }

    private OrganizationChartNodeResponse buildHierarchy(Long projectId, Long cellId, boolean reportingAware) {
        if (cellId != null) {
            return buildCellNode(getCell(cellId), reportingAware);
        }
        if (projectId != null) {
            ProjectMst project = getProject(projectId);
            return project.getCell() == null
                    ? buildProjectNode(project, reportingAware)
                    : buildCellNode(project.getCell(), reportingAware);
        }

        List<CellMaster> cells = cellRepository
                .findByActiveFlagIgnoreCaseAndWing_ActiveFlagIgnoreCaseOrderByCellNameAsc(ACTIVE_FLAG, ACTIVE_FLAG);
        if (cells.size() == 1) {
            return buildCellNode(cells.get(0), reportingAware);
        }

        List<OrganizationChartNodeResponse> cellNodes = cells.stream()
                .map(cell -> buildCellNode(cell, reportingAware))
                .collect(Collectors.toList());
        return OrganizationChartNodeResponse.builder()
                .id("organization-root")
                .nodeType(ROOT_NODE)
                .label("MAHAIT Organization")
                .subtitle(cellNodes.size() + " Cells")
                .expandable(!cellNodes.isEmpty())
                .children(cellNodes)
                .build();
    }

    private OrganizationChartNodeResponse buildCellNode(CellMaster cell, boolean reportingAware) {
        List<TeamMasterEntity> teams = teamRepository
                .findByCell_CellIdAndStatusOrderByDisplayOrderAscTeamNameAsc(
                        cell.getCellId(),
                        OrganizationRecordStatus.ACTIVE);
        List<PositionMasterEntity> positions = positionRepository
                .findByCellScopeAndStatusOrderByDisplayOrderAscPositionIdAsc(
                        cell.getCellId(),
                        OrganizationRecordStatus.ACTIVE);

        Map<Long, List<TeamMasterEntity>> teamsByParentId = teams.stream()
                .collect(Collectors.groupingBy(
                        team -> team.getParentTeam() == null ? 0L : team.getParentTeam().getTeamId(),
                        LinkedHashMap::new,
                        Collectors.toList()));
        Map<Long, List<PositionMasterEntity>> positionsByTeamId = positions.stream()
                .filter(position -> position.getTeam() != null)
                .collect(Collectors.groupingBy(
                        position -> position.getTeam().getTeamId(),
                        LinkedHashMap::new,
                        Collectors.toList()));
        Map<Long, List<PositionMasterEntity>> positionsByReportingId = positions.stream()
                .filter(position -> position.getReportingPosition() != null)
                .collect(Collectors.groupingBy(
                        position -> position.getReportingPosition().getPositionId(),
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<OrganizationChartNodeResponse> children = new ArrayList<>();
        children.addAll(buildTeamNodes(
                teamsByParentId.getOrDefault(0L, new ArrayList<>()),
                teamsByParentId,
                positionsByTeamId,
                positionsByReportingId,
                reportingAware));
        children.addAll(projectRootPositions(positions, positionsByReportingId, reportingAware));

        return OrganizationChartNodeResponse.builder()
                .id("cell-" + cell.getCellId())
                .nodeType(CELL_NODE)
                .cellId(cell.getCellId())
                .label(cell.getCellName())
                .subtitle(cell.getWing() == null ? null : cell.getWing().getWingName())
                .expandable(!children.isEmpty())
                .children(children)
                .build();
    }

    private OrganizationChartNodeResponse buildProjectNode(ProjectMst project, boolean reportingAware) {
        List<TeamMasterEntity> teams = project.getCell() == null
                ? teamRepository.findByProject_ProjectIdAndStatusOrderByDisplayOrderAscTeamNameAsc(
                        project.getProjectId(),
                        OrganizationRecordStatus.ACTIVE)
                : teamRepository.findByCell_CellIdAndStatusOrderByDisplayOrderAscTeamNameAsc(
                        project.getCell().getCellId(),
                        OrganizationRecordStatus.ACTIVE);
        List<PositionMasterEntity> positions = positionRepository
                .findByProjectScopeAndStatusOrderByDisplayOrderAscPositionIdAsc(
                        project.getProjectId(),
                        project.getCell() == null ? null : project.getCell().getCellId(),
                        OrganizationRecordStatus.ACTIVE);

        Map<Long, List<TeamMasterEntity>> teamsByParentId = teams.stream()
                .collect(Collectors.groupingBy(
                        team -> team.getParentTeam() == null ? 0L : team.getParentTeam().getTeamId(),
                        LinkedHashMap::new,
                        Collectors.toList()));
        Map<Long, List<PositionMasterEntity>> positionsByTeamId = positions.stream()
                .filter(position -> position.getTeam() != null)
                .collect(Collectors.groupingBy(
                        position -> position.getTeam().getTeamId(),
                        LinkedHashMap::new,
                        Collectors.toList()));
        Map<Long, List<PositionMasterEntity>> positionsByReportingId = positions.stream()
                .filter(position -> position.getReportingPosition() != null)
                .collect(Collectors.groupingBy(
                        position -> position.getReportingPosition().getPositionId(),
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<OrganizationChartNodeResponse> children = new ArrayList<>();
        children.addAll(projectRootPositions(positions, positionsByReportingId, reportingAware));
        children.addAll(buildTeamNodes(
                teamsByParentId.getOrDefault(0L, new ArrayList<>()),
                teamsByParentId,
                positionsByTeamId,
                positionsByReportingId,
                reportingAware));

        return OrganizationChartNodeResponse.builder()
                .id("project-" + project.getProjectId())
                .nodeType(PROJECT_NODE)
                .projectId(project.getProjectId())
                .label(project.getProjectName())
                .subtitle(project.getProjectCode())
                .expandable(!children.isEmpty())
                .children(children)
                .build();
    }

    private List<OrganizationChartNodeResponse> projectRootPositions(
            List<PositionMasterEntity> positions,
            Map<Long, List<PositionMasterEntity>> positionsByReportingId,
            boolean reportingAware) {
        return positions.stream()
                .filter(position -> position.getTeam() == null)
                .filter(position -> !reportingAware || position.getReportingPosition() == null)
                .sorted(positionComparator())
                .map(position -> toPositionNode(position, positionsByReportingId, reportingAware, null))
                .collect(Collectors.toList());
    }

    private List<OrganizationChartNodeResponse> buildTeamNodes(
            List<TeamMasterEntity> teams,
            Map<Long, List<TeamMasterEntity>> teamsByParentId,
            Map<Long, List<PositionMasterEntity>> positionsByTeamId,
            Map<Long, List<PositionMasterEntity>> positionsByReportingId,
            boolean reportingAware) {
        return teams.stream()
                .sorted(teamComparator())
                .map(team -> buildTeamNode(team, teamsByParentId, positionsByTeamId, positionsByReportingId, reportingAware))
                .collect(Collectors.toList());
    }

    private OrganizationChartNodeResponse buildTeamNode(
            TeamMasterEntity team,
            Map<Long, List<TeamMasterEntity>> teamsByParentId,
            Map<Long, List<PositionMasterEntity>> positionsByTeamId,
            Map<Long, List<PositionMasterEntity>> positionsByReportingId,
            boolean reportingAware) {
        List<OrganizationChartNodeResponse> children = new ArrayList<>();
        children.addAll(buildTeamNodes(
                teamsByParentId.getOrDefault(team.getTeamId(), new ArrayList<>()),
                teamsByParentId,
                positionsByTeamId,
                positionsByReportingId,
                reportingAware));

        List<PositionMasterEntity> teamPositions = new ArrayList<>(
                positionsByTeamId.getOrDefault(team.getTeamId(), new ArrayList<>()));
        teamPositions.sort(positionComparator());
        if (reportingAware) {
            children.addAll(teamPositions.stream()
                    .filter(position -> isTeamRootPosition(position, team.getTeamId()))
                    .map(position -> toPositionNode(position, positionsByReportingId, true, team.getTeamId()))
                    .collect(Collectors.toList()));
        } else {
            children.addAll(teamPositions.stream()
                    .map(position -> toPositionNode(position, positionsByReportingId, false, team.getTeamId()))
                    .collect(Collectors.toList()));
        }

        return OrganizationChartNodeResponse.builder()
                .id("team-" + team.getTeamId())
                .nodeType(TEAM_NODE)
                .cellId(team.getCell() == null ? null : team.getCell().getCellId())
                .projectId(team.getProject() == null ? null : team.getProject().getProjectId())
                .teamId(team.getTeamId())
                .label(team.getTeamName())
                .subtitle(team.getTeamType() == null ? null : team.getTeamType().name())
                .expandable(!children.isEmpty())
                .children(children)
                .build();
    }

    private OrganizationChartNodeResponse toPositionNode(
            PositionMasterEntity position,
            Map<Long, List<PositionMasterEntity>> positionsByReportingId,
            boolean reportingAware,
            Long containingTeamId) {
        String designationName = position.getDesignation() == null
                ? null
                : position.getDesignation().getDesignationName();
        ResourceLevelExperience level = position.getResourceLevel();
        String designationDisplay = designationWithLevel(designationName, level);
        EmployeeEntity employee = position.getEmployee();
        boolean vacant = employee == null || position.getPositionStatus() == PositionStatus.VACANT;
        String label = vacant ? "Vacant - " + designationDisplay
                : employee.getFullName() + " - " + designationDisplay;

        List<OrganizationChartNodeResponse> children = new ArrayList<>();
        if (reportingAware) {
            children = positionsByReportingId.getOrDefault(position.getPositionId(), new ArrayList<>())
                    .stream()
                    .filter(child -> containingTeamId == null
                            ? child.getTeam() == null
                            : child.getTeam() != null && containingTeamId.equals(child.getTeam().getTeamId()))
                    .sorted(positionComparator())
                    .map(child -> toPositionNode(child, positionsByReportingId, true, containingTeamId))
                    .collect(Collectors.toList());
        }

        return OrganizationChartNodeResponse.builder()
                .id("position-" + position.getPositionId())
                .nodeType(POSITION_NODE)
                .cellId(resolvePositionCellId(position))
                .projectId(position.getProject() == null ? null : position.getProject().getProjectId())
                .teamId(position.getTeam() == null ? null : position.getTeam().getTeamId())
                .positionId(position.getPositionId())
                .employeeId(employee == null ? null : employee.getEmployeeId())
                .employeeName(employee == null ? null : employee.getFullName())
                .employeeCode(employee == null ? null : employee.getEmployeeCode())
                .designationName(designationName)
                .levelCode(level == null ? null : level.getLevelCode())
                .levelName(level == null ? null : level.getLevelName())
                .positionStatus(position.getPositionStatus())
                .label(label)
                .subtitle(position.getPositionName())
                .vacant(vacant)
                .expandable(!children.isEmpty())
                .children(children)
                .build();
    }

    private boolean isTeamRootPosition(PositionMasterEntity position, Long teamId) {
        if (position.getReportingPosition() == null) {
            return true;
        }
        if (position.getReportingPosition().getTeam() == null) {
            return true;
        }
        return !teamId.equals(position.getReportingPosition().getTeam().getTeamId());
    }

    private ProjectMst getProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found for id: " + projectId));
    }

    private CellMaster getCell(Long cellId) {
        CellMaster cell = cellRepository.findByCellId(cellId)
                .orElseThrow(() -> new ResourceNotFoundException("Cell not found for id: " + cellId));
        if (!ACTIVE_FLAG.equalsIgnoreCase(cell.getActiveFlag())) {
            throw new BusinessValidationException("Selected cell is inactive.");
        }
        return cell;
    }

    private TeamStrengthResponse toTeamStrength(TeamStrengthProjection projection) {
        return TeamStrengthResponse.builder()
                .teamId(projection.getTeamId())
                .teamName(projection.getTeamName())
                .teamType(projection.getTeamType())
                .totalPositions(defaultLong(projection.getTotalPositions()))
                .filledPositions(defaultLong(projection.getFilledPositions()))
                .vacantPositions(defaultLong(projection.getVacantPositions()))
                .build();
    }

    private OrganizationSearchResult toPositionSearchResult(PositionMasterEntity position) {
        EmployeeEntity employee = position.getEmployee();
        String designationName = position.getDesignation() == null ? null : position.getDesignation().getDesignationName();
        ResourceLevelExperience level = position.getResourceLevel();
        String designationDisplay = designationWithLevel(designationName, level);
        boolean vacant = employee == null || position.getPositionStatus() == PositionStatus.VACANT;
        return OrganizationSearchResult.builder()
                .resultType("POSITION")
                .id(position.getPositionId())
                .title(vacant ? "Vacant - " + designationDisplay : employee.getFullName())
                .subtitle(position.getPositionName())
                .projectId(position.getProject() == null ? null : position.getProject().getProjectId())
                .projectName(position.getProject() == null ? null : position.getProject().getProjectName())
                .cellId(resolvePositionCellId(position))
                .cellName(resolvePositionCellName(position))
                .teamId(position.getTeam() == null ? null : position.getTeam().getTeamId())
                .teamName(position.getTeam() == null ? null : position.getTeam().getTeamName())
                .designationName(designationName)
                .levelCode(level == null ? null : level.getLevelCode())
                .levelName(level == null ? null : level.getLevelName())
                .positionStatus(position.getPositionStatus())
                .vacant(vacant)
                .build();
    }

    private OrganizationSearchResult toTeamSearchResult(TeamMasterEntity team) {
        return OrganizationSearchResult.builder()
                .resultType("TEAM")
                .id(team.getTeamId())
                .title(team.getTeamName())
                .subtitle(team.getTeamType() == null ? null : team.getTeamType().name())
                .projectId(team.getProject() == null ? null : team.getProject().getProjectId())
                .projectName(team.getProject() == null ? null : team.getProject().getProjectName())
                .cellId(team.getCell() == null ? null : team.getCell().getCellId())
                .cellName(team.getCell() == null ? null : team.getCell().getCellName())
                .teamId(team.getTeamId())
                .teamName(team.getTeamName())
                .vacant(false)
                .build();
    }

    private Comparator<TeamMasterEntity> teamComparator() {
        return Comparator.comparing(
                TeamMasterEntity::getDisplayOrder,
                Comparator.nullsLast(Integer::compareTo))
                .thenComparing(TeamMasterEntity::getTeamName, nullSafeStringComparator());
    }

    private Comparator<PositionMasterEntity> positionComparator() {
        return Comparator.comparing(
                PositionMasterEntity::getDisplayOrder,
                Comparator.nullsLast(Integer::compareTo))
                .thenComparing(PositionMasterEntity::getPositionName, nullSafeStringComparator())
                .thenComparing(PositionMasterEntity::getPositionId, Comparator.nullsLast(Long::compareTo));
    }

    private Comparator<String> nullSafeStringComparator() {
        return Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER);
    }

    private String nullToLabel(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String designationWithLevel(String designationName, ResourceLevelExperience level) {
        String label = nullToLabel(designationName, "Designation");
        if (level == null || !StringUtils.hasText(level.getLevelCode())) {
            return label;
        }
        return label + " (" + level.getLevelCode() + ")";
    }

    private Long resolvePositionCellId(PositionMasterEntity position) {
        if (position.getCell() != null) {
            return position.getCell().getCellId();
        }
        if (position.getTeam() != null && position.getTeam().getCell() != null) {
            return position.getTeam().getCell().getCellId();
        }
        return position.getProject() == null || position.getProject().getCell() == null
                ? null
                : position.getProject().getCell().getCellId();
    }

    private String resolvePositionCellName(PositionMasterEntity position) {
        if (position.getCell() != null) {
            return position.getCell().getCellName();
        }
        if (position.getTeam() != null && position.getTeam().getCell() != null) {
            return position.getTeam().getCell().getCellName();
        }
        return position.getProject() == null || position.getProject().getCell() == null
                ? null
                : position.getProject().getCell().getCellName();
    }

    private long defaultLong(Long value) {
        return value == null ? 0L : value;
    }
}
