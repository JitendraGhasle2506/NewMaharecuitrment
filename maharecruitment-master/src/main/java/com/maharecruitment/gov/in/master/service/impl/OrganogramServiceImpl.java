package com.maharecruitment.gov.in.master.service.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.master.dto.OrganogramNodeView;
import com.maharecruitment.gov.in.master.dto.OrganogramView;
import com.maharecruitment.gov.in.master.entity.CellMaster;
import com.maharecruitment.gov.in.master.entity.ProjectMst;
import com.maharecruitment.gov.in.master.entity.WingMaster;
import com.maharecruitment.gov.in.master.repository.CellMasterRepository;
import com.maharecruitment.gov.in.master.repository.ProjectMstRepository;
import com.maharecruitment.gov.in.master.repository.WingMasterRepository;
import com.maharecruitment.gov.in.master.service.OrganogramService;

@Service
@Transactional(readOnly = true)
public class OrganogramServiceImpl implements OrganogramService {

    private static final String ACTIVE = "Y";
    private static final String ROOT_NODE_TYPE = "ROOT";
    private static final String WING_NODE_TYPE = "WING";
    private static final String CELL_NODE_TYPE = "CELL";
    private static final String PROJECT_NODE_TYPE = "PROJECT";

    private final WingMasterRepository wingRepository;
    private final CellMasterRepository cellRepository;
    private final ProjectMstRepository projectRepository;

    public OrganogramServiceImpl(
            WingMasterRepository wingRepository,
            CellMasterRepository cellRepository,
            ProjectMstRepository projectRepository) {
        this.wingRepository = wingRepository;
        this.cellRepository = cellRepository;
        this.projectRepository = projectRepository;
    }

    @Override
    public OrganogramView getActiveOrganogram() {
        List<WingMaster> wings = wingRepository.findByActiveFlagIgnoreCaseOrderByWingNameAsc(ACTIVE);
        List<CellMaster> cells = cellRepository
                .findByActiveFlagIgnoreCaseAndWing_ActiveFlagIgnoreCaseOrderByCellNameAsc(ACTIVE, ACTIVE);
        List<ProjectMst> projects = projectRepository.findByActiveFlagIgnoreCaseOrderByProjectNameAsc(ACTIVE);

        Map<Long, List<CellMaster>> cellsByWingId = cells.stream()
                .filter(cell -> cell.getWing() != null && cell.getWing().getWingId() != null)
                .collect(Collectors.groupingBy(
                        cell -> cell.getWing().getWingId(),
                        LinkedHashMap::new,
                        Collectors.toList()));

        Map<Long, List<ProjectMst>> projectsByCellId = projects.stream()
                .filter(this::hasActiveCellAndWing)
                .collect(Collectors.groupingBy(
                        project -> project.getCell().getCellId(),
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<OrganogramNodeView> wingNodes = wings.stream()
                .map(wing -> toWingNode(wing, cellsByWingId, projectsByCellId))
                .toList();

        int projectCount = wingNodes.stream().mapToInt(OrganogramNodeView::totalProjects).sum();
        OrganogramNodeView root = new OrganogramNodeView(
                "root-md",
                ROOT_NODE_TYPE,
                "Managing Director",
                "Organisation Overview",
                "Active",
                "fa-solid fa-user-tie",
                wingNodes.size(),
                projectCount,
                wingNodes);

        return new OrganogramView(root, wingNodes.size(), cells.size(), projectCount, calculateDepth(root));
    }

    private OrganogramNodeView toWingNode(
            WingMaster wing,
            Map<Long, List<CellMaster>> cellsByWingId,
            Map<Long, List<ProjectMst>> projectsByCellId) {
        List<CellMaster> wingCells = new ArrayList<>(cellsByWingId.getOrDefault(wing.getWingId(), List.of()));
        wingCells.sort(Comparator.comparing(CellMaster::getCellName, String.CASE_INSENSITIVE_ORDER));

        List<OrganogramNodeView> cellNodes = wingCells.stream()
                .map(cell -> toCellNode(cell, projectsByCellId))
                .toList();
        int projectCount = cellNodes.stream().mapToInt(OrganogramNodeView::totalProjects).sum();

        return new OrganogramNodeView(
                "wing-" + wing.getWingId(),
                WING_NODE_TYPE,
                wing.getWingName(),
                cellNodes.size() + " Cells",
                resolveStatus(wing.getActiveFlag()),
                "fa-solid fa-sitemap",
                cellNodes.size(),
                projectCount,
                cellNodes);
    }

    private OrganogramNodeView toCellNode(CellMaster cell, Map<Long, List<ProjectMst>> projectsByCellId) {
        List<ProjectMst> cellProjects = new ArrayList<>(projectsByCellId.getOrDefault(cell.getCellId(), List.of()));
        cellProjects.sort(Comparator.comparing(ProjectMst::getProjectName, String.CASE_INSENSITIVE_ORDER));

        List<OrganogramNodeView> projectNodes = cellProjects.stream()
                .map(this::toProjectNode)
                .toList();

        return new OrganogramNodeView(
                "cell-" + cell.getCellId(),
                CELL_NODE_TYPE,
                cell.getCellName(),
                projectNodes.size() + " Projects",
                resolveStatus(cell.getActiveFlag()),
                "fa-solid fa-table-cells",
                projectNodes.size(),
                projectNodes.size(),
                projectNodes);
    }

    private OrganogramNodeView toProjectNode(ProjectMst project) {
        String caption = project.getProjectScopeType() == null
                ? "Project"
                : titleCase(project.getProjectScopeType().name());
        return new OrganogramNodeView(
                "project-" + project.getProjectId(),
                PROJECT_NODE_TYPE,
                project.getProjectName(),
                caption,
                resolveStatus(project.getActiveFlag()),
                "fa-solid fa-diagram-project",
                0,
                1,
                List.of());
    }

    private boolean hasActiveCellAndWing(ProjectMst project) {
        if (project == null || project.getCell() == null || project.getCell().getWing() == null) {
            return false;
        }
        return ACTIVE.equalsIgnoreCase(project.getCell().getActiveFlag())
                && ACTIVE.equalsIgnoreCase(project.getCell().getWing().getActiveFlag());
    }

    private int calculateDepth(OrganogramNodeView node) {
        if (node.children().isEmpty()) {
            return 1;
        }
        return 1 + node.children().stream()
                .filter(Objects::nonNull)
                .mapToInt(this::calculateDepth)
                .max()
                .orElse(0);
    }

    private String resolveStatus(String activeFlag) {
        return ACTIVE.equalsIgnoreCase(activeFlag) ? "Active" : "Inactive";
    }

    private String titleCase(String value) {
        if (value == null || value.isBlank()) {
            return "Project";
        }
        String normalized = value.trim().replace('_', ' ').toLowerCase();
        String[] words = normalized.split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return builder.toString();
    }
}
