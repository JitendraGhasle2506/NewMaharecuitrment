package com.maharecruitment.gov.in.web.service.dashboard.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.master.entity.CellMaster;
import com.maharecruitment.gov.in.master.entity.ManpowerDesignationMaster;
import com.maharecruitment.gov.in.master.entity.ResourceLevelExperience;
import com.maharecruitment.gov.in.master.entity.WingMaster;
import com.maharecruitment.gov.in.master.repository.CellMasterRepository;
import com.maharecruitment.gov.in.master.repository.WingMasterRepository;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.entity.organization.OrganizationRecordStatus;
import com.maharecruitment.gov.in.recruitment.entity.organization.PositionMasterEntity;
import com.maharecruitment.gov.in.recruitment.entity.organization.PositionStatus;
import com.maharecruitment.gov.in.recruitment.repository.organization.PositionMasterRepository;
import com.maharecruitment.gov.in.web.service.dashboard.MdWorkforceReportService;
import com.maharecruitment.gov.in.web.service.dashboard.model.MdWorkforceCellView;
import com.maharecruitment.gov.in.web.service.dashboard.model.MdWorkforceEmployeeView;
import com.maharecruitment.gov.in.web.service.dashboard.model.MdWorkforceReportView;
import com.maharecruitment.gov.in.web.service.dashboard.model.MdWorkforceWingView;

@Service
@Transactional(readOnly = true)
public class MdWorkforceReportServiceImpl implements MdWorkforceReportService {

    private static final String ACTIVE_FLAG = "Y";
    private static final String ACTIVE_EMPLOYEE_STATUS = "ACTIVE";

    private final WingMasterRepository wingRepository;
    private final CellMasterRepository cellRepository;
    private final PositionMasterRepository positionRepository;

    public MdWorkforceReportServiceImpl(
            WingMasterRepository wingRepository,
            CellMasterRepository cellRepository,
            PositionMasterRepository positionRepository) {
        this.wingRepository = wingRepository;
        this.cellRepository = cellRepository;
        this.positionRepository = positionRepository;
    }

    @Override
    public MdWorkforceReportView getReport() {
        List<WingMaster> wings = wingRepository.findByActiveFlagIgnoreCaseOrderByWingNameAsc(ACTIVE_FLAG);
        List<CellMaster> cells = cellRepository
                .findByActiveFlagIgnoreCaseAndWing_ActiveFlagIgnoreCaseOrderByCellNameAsc(ACTIVE_FLAG, ACTIVE_FLAG);
        Map<Long, List<CellMaster>> cellsByWingId = groupCellsByWing(cells);
        Map<Long, List<MdWorkforceEmployeeView>> employeesByCellId = groupEmployeesByCell();

        List<MdWorkforceWingView> wingViews = wings.stream()
                .map(wing -> toWingView(wing, cellsByWingId, employeesByCellId))
                .toList();

        int totalCells = wingViews.stream().mapToInt(MdWorkforceWingView::cellCount).sum();
        int totalEmployees = wingViews.stream().mapToInt(MdWorkforceWingView::employeeCount).sum();
        return new MdWorkforceReportView(wingViews.size(), totalCells, totalEmployees, wingViews);
    }

    private Map<Long, List<CellMaster>> groupCellsByWing(List<CellMaster> cells) {
        return cells.stream()
                .filter(cell -> cell.getWing() != null && cell.getWing().getWingId() != null)
                .collect(Collectors.groupingBy(
                        cell -> cell.getWing().getWingId(),
                        LinkedHashMap::new,
                        Collectors.toList()));
    }

    private Map<Long, List<MdWorkforceEmployeeView>> groupEmployeesByCell() {
        List<PositionMasterEntity> positions = positionRepository
                .findByStatusOrderByCell_CellNameAscDisplayOrderAscPositionIdAsc(OrganizationRecordStatus.ACTIVE);
        Map<Long, List<PositionMasterEntity>> positionsByCellId = positions.stream()
                .filter(this::isFilledActiveEmployeePosition)
                .filter(position -> position.getCell() != null && position.getCell().getCellId() != null)
                .collect(Collectors.groupingBy(
                        position -> position.getCell().getCellId(),
                        LinkedHashMap::new,
                        Collectors.toList()));

        Map<Long, List<MdWorkforceEmployeeView>> employeesByCellId = new LinkedHashMap<>();
        positionsByCellId.forEach((cellId, cellPositions) ->
                employeesByCellId.put(cellId, toDistinctEmployeeViews(cellPositions)));
        return employeesByCellId;
    }

    private MdWorkforceWingView toWingView(
            WingMaster wing,
            Map<Long, List<CellMaster>> cellsByWingId,
            Map<Long, List<MdWorkforceEmployeeView>> employeesByCellId) {
        List<CellMaster> wingCells = new ArrayList<>(cellsByWingId.getOrDefault(wing.getWingId(), List.of()));
        wingCells.sort(Comparator.comparing(CellMaster::getCellName, String.CASE_INSENSITIVE_ORDER));

        List<MdWorkforceCellView> cellViews = wingCells.stream()
                .map(cell -> toCellView(cell, employeesByCellId))
                .toList();
        int employeeCount = cellViews.stream().mapToInt(MdWorkforceCellView::employeeCount).sum();
        return new MdWorkforceWingView(
                wing.getWingId(),
                defaultText(wing.getWingName(), "Unassigned Wing"),
                cellViews.size(),
                employeeCount,
                cellViews);
    }

    private MdWorkforceCellView toCellView(
            CellMaster cell,
            Map<Long, List<MdWorkforceEmployeeView>> employeesByCellId) {
        List<MdWorkforceEmployeeView> employees = employeesByCellId.getOrDefault(cell.getCellId(), List.of());
        return new MdWorkforceCellView(
                cell.getCellId(),
                defaultText(cell.getCellName(), "Unnamed Cell"),
                employees.size(),
                employees);
    }

    private List<MdWorkforceEmployeeView> toDistinctEmployeeViews(List<PositionMasterEntity> positions) {
        Set<Long> seenEmployeeIds = new HashSet<>();
        return positions.stream()
                .sorted(positionComparator())
                .filter(position -> seenEmployeeIds.add(position.getEmployee().getEmployeeId()))
                .map(this::toEmployeeView)
                .toList();
    }

    private Comparator<PositionMasterEntity> positionComparator() {
        return Comparator
                .comparing(PositionMasterEntity::getDisplayOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(position -> employeeName(position.getEmployee()), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(PositionMasterEntity::getPositionId, Comparator.nullsLast(Long::compareTo));
    }

    private MdWorkforceEmployeeView toEmployeeView(PositionMasterEntity position) {
        EmployeeEntity employee = position.getEmployee();
        String employeeName = defaultText(employee.getFullName(), "Employee");
        return new MdWorkforceEmployeeView(
                employee.getEmployeeId(),
                defaultText(employee.getEmployeeCode(), "-"),
                employeeName,
                initials(employeeName),
                defaultText(employee.getPhotoPath(), ""),
                resolveDesignationName(position, employee),
                resolveLevelCode(position, employee),
                defaultText(position.getPositionName(), "-"),
                defaultText(employee.getRecruitmentType(), "-"));
    }

    private boolean isFilledActiveEmployeePosition(PositionMasterEntity position) {
        if (position == null || position.getEmployee() == null) {
            return false;
        }
        EmployeeEntity employee = position.getEmployee();
        return position.getPositionStatus() == PositionStatus.FILLED
                && ACTIVE_EMPLOYEE_STATUS.equalsIgnoreCase(employee.getStatus());
    }

    private String resolveDesignationName(PositionMasterEntity position, EmployeeEntity employee) {
        ManpowerDesignationMaster designation = position.getDesignation() != null
                ? position.getDesignation()
                : employee.getDesignation();
        return designation == null ? "-" : defaultText(designation.getDesignationName(), "-");
    }

    private String resolveLevelCode(PositionMasterEntity position, EmployeeEntity employee) {
        ResourceLevelExperience level = position.getResourceLevel();
        String levelCode = level == null ? employee.getLevelCode() : level.getLevelCode();
        return defaultText(levelCode, "-");
    }

    private String employeeName(EmployeeEntity employee) {
        return employee == null ? "" : defaultText(employee.getFullName(), "");
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String initials(String value) {
        String[] words = defaultText(value, "E").split("\\s+");
        if (words.length == 1) {
            return words[0].substring(0, Math.min(words[0].length(), 2)).toUpperCase();
        }
        return (String.valueOf(words[0].charAt(0)) + words[1].charAt(0)).toUpperCase();
    }
}
