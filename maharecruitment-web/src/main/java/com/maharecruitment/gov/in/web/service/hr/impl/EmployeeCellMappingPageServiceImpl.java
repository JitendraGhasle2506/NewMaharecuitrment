package com.maharecruitment.gov.in.web.service.hr.impl;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.master.entity.CellMaster;
import com.maharecruitment.gov.in.master.repository.CellMasterRepository;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeCellMappingAuditLogEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeCellMappingEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeCellMappingAuditLogRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeCellMappingRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.web.service.hr.EmployeeCellMappingPageService;
import com.maharecruitment.gov.in.web.service.hr.model.EmployeeCellBulkMappingResult;
import com.maharecruitment.gov.in.web.service.hr.model.EmployeeCellMappingAuditView;
import com.maharecruitment.gov.in.web.service.hr.model.EmployeeCellMappingEditView;
import com.maharecruitment.gov.in.web.service.hr.model.EmployeeCellMappingEmployeeView;
import com.maharecruitment.gov.in.web.service.hr.model.EmployeeCellOptionView;

@Service
@Transactional(readOnly = true)
public class EmployeeCellMappingPageServiceImpl implements EmployeeCellMappingPageService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeCellMappingPageServiceImpl.class);
    private static final String ACTIVE_STATUS = "ACTIVE";
    private static final String ACTIVE_FLAG = "Y";
    private static final String AUDIT_ACTION_ASSIGNED = "ASSIGNED";
    private static final String AUDIT_ACTION_UPDATED = "UPDATED";

    private final EmployeeRepository employeeRepository;
    private final CellMasterRepository cellMasterRepository;
    private final EmployeeCellMappingRepository employeeCellMappingRepository;
    private final EmployeeCellMappingAuditLogRepository auditLogRepository;

    public EmployeeCellMappingPageServiceImpl(
            EmployeeRepository employeeRepository,
            CellMasterRepository cellMasterRepository,
            EmployeeCellMappingRepository employeeCellMappingRepository,
            EmployeeCellMappingAuditLogRepository auditLogRepository) {
        this.employeeRepository = employeeRepository;
        this.cellMasterRepository = cellMasterRepository;
        this.employeeCellMappingRepository = employeeCellMappingRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public Page<EmployeeCellMappingEmployeeView> searchEmployees(
            String recruitmentType,
            String searchText,
            Pageable pageable) {
        Page<EmployeeEntity> employeePage = employeeRepository.findActiveOnboardedWithoutCellMapping(
                normalizeRecruitmentType(recruitmentType),
                buildSearchPattern(searchText),
                pageable);
        List<EmployeeCellMappingEmployeeView> content = employeePage.getContent().stream()
                .map(employee -> toEmployeeView(employee, null))
                .toList();
        return new PageImpl<>(content, pageable, employeePage.getTotalElements());
    }

    @Override
    public EmployeeCellMappingEditView loadMapping(Long employeeId) {
        EmployeeEntity employee = loadEligibleEmployee(employeeId);
        EmployeeCellOptionView selectedCell = employeeCellMappingRepository
                .findByEmployeeEmployeeId(employee.getEmployeeId())
                .map(mapping -> toCellOptionView(mapping.getCell()))
                .orElse(null);

        List<EmployeeCellOptionView> availableCells = loadAvailableCells(selectedCell);
        List<EmployeeCellMappingAuditView> auditLogs = auditLogRepository
                .findTop10ByEmployeeEmployeeIdOrderByOccurredAtDescAuditIdDesc(employee.getEmployeeId())
                .stream()
                .map(this::toAuditView)
                .toList();

        return new EmployeeCellMappingEditView(
                toEmployeeView(employee, selectedCell),
                availableCells,
                selectedCell,
                auditLogs);
    }

    @Override
    public List<EmployeeCellOptionView> availableActiveCells() {
        return loadActiveCellOptions();
    }

    @Override
    @Transactional
    public boolean updateMapping(Long employeeId, Long cellId, String actorLoginId) {
        EmployeeEntity employee = loadEligibleEmployee(employeeId);
        CellMaster selectedCell = resolveActiveCell(cellId);
        EmployeeCellMappingEntity mapping = employeeCellMappingRepository
                .findByEmployeeEmployeeId(employee.getEmployeeId())
                .orElse(null);
        CellMaster previousCell = mapping == null ? null : mapping.getCell();

        if (previousCell != null && Objects.equals(previousCell.getCellId(), selectedCell.getCellId())) {
            log.info(
                    "Employee cell mapping unchanged. employeeId={}, actorLoginId={}, cellId={}",
                    employee.getEmployeeId(),
                    actorLoginId,
                    selectedCell.getCellId());
            return false;
        }

        if (mapping == null) {
            mapping = new EmployeeCellMappingEntity();
            mapping.setEmployee(employee);
        }
        mapping.setCell(selectedCell);
        employeeCellMappingRepository.save(mapping);

        saveAuditLog(employee, actorLoginId, previousCell, selectedCell);
        log.info(
                "Employee cell mapping saved. employeeId={}, actorLoginId={}, previousCellId={}, newCellId={}",
                employee.getEmployeeId(),
                actorLoginId,
                previousCell == null ? null : previousCell.getCellId(),
                selectedCell.getCellId());
        return true;
    }

    @Override
    @Transactional
    public EmployeeCellBulkMappingResult updateMappings(
            Long cellId,
            List<Long> employeeIds,
            String actorLoginId) {
        CellMaster selectedCell = resolveActiveCell(cellId);
        List<Long> uniqueEmployeeIds = normalizeEmployeeIds(employeeIds);
        if (uniqueEmployeeIds.isEmpty()) {
            throw new RecruitmentNotificationException("Select at least one employee to assign.");
        }

        List<EmployeeEntity> employees = employeeRepository.findDetailedByEmployeeIdIn(uniqueEmployeeIds);
        if (employees.size() != uniqueEmployeeIds.size()) {
            throw new RecruitmentNotificationException("One or more selected employees are invalid.");
        }

        Map<Long, EmployeeEntity> employeeById = employees.stream()
                .collect(Collectors.toMap(EmployeeEntity::getEmployeeId, Function.identity()));
        List<EmployeeEntity> orderedEmployees = uniqueEmployeeIds.stream()
                .map(employeeById::get)
                .peek(this::validateEligibleEmployee)
                .toList();

        Map<Long, EmployeeCellMappingEntity> mappingByEmployeeId = employeeCellMappingRepository
                .findByEmployeeEmployeeIdInOrderByEmployeeEmployeeIdAsc(uniqueEmployeeIds)
                .stream()
                .collect(Collectors.toMap(
                        mapping -> mapping.getEmployee().getEmployeeId(),
                        Function.identity(),
                        (left, right) -> left));

        List<EmployeeCellMappingEntity> mappingsToSave = new ArrayList<>();
        List<EmployeeCellMappingAuditLogEntity> auditLogs = new ArrayList<>();
        int unchangedCount = 0;

        for (EmployeeEntity employee : orderedEmployees) {
            EmployeeCellMappingEntity mapping = mappingByEmployeeId.get(employee.getEmployeeId());
            CellMaster previousCell = mapping == null ? null : mapping.getCell();
            if (previousCell != null && Objects.equals(previousCell.getCellId(), selectedCell.getCellId())) {
                unchangedCount++;
                continue;
            }

            if (mapping == null) {
                mapping = new EmployeeCellMappingEntity();
                mapping.setEmployee(employee);
            }
            mapping.setCell(selectedCell);
            mappingsToSave.add(mapping);
            auditLogs.add(buildAuditLog(employee, actorLoginId, previousCell, selectedCell));
        }

        if (!mappingsToSave.isEmpty()) {
            employeeCellMappingRepository.saveAll(mappingsToSave);
            auditLogRepository.saveAll(auditLogs);
        }

        log.info(
                "Bulk employee cell mapping completed. actorLoginId={}, cellId={}, requested={}, changed={}, unchanged={}",
                actorLoginId,
                selectedCell.getCellId(),
                uniqueEmployeeIds.size(),
                mappingsToSave.size(),
                unchangedCount);
        return new EmployeeCellBulkMappingResult(
                uniqueEmployeeIds.size(),
                mappingsToSave.size(),
                unchangedCount);
    }

    private EmployeeEntity loadEligibleEmployee(Long employeeId) {
        if (employeeId == null || employeeId < 1) {
            throw new RecruitmentNotificationException("Valid employee id is required.");
        }
        EmployeeEntity employee = employeeRepository.findDetailedByEmployeeId(employeeId)
                .orElseThrow(() -> new RecruitmentNotificationException("Employee not found."));
        validateEligibleEmployee(employee);
        return employee;
    }

    private void validateEligibleEmployee(EmployeeEntity employee) {
        if (!ACTIVE_STATUS.equalsIgnoreCase(employee.getStatus())) {
            throw new RecruitmentNotificationException("Cell can be mapped only for active onboarded employees.");
        }
        if (!StringUtils.hasText(employee.getEmployeeCode())
                || "PENDING".equalsIgnoreCase(employee.getEmployeeCode().trim())
                || employee.getEmployeeCode().trim().toUpperCase().startsWith("TMP-")) {
            throw new RecruitmentNotificationException("Employee code is not finalized yet.");
        }
    }

    private CellMaster resolveActiveCell(Long cellId) {
        if (cellId == null || cellId < 1) {
            throw new RecruitmentNotificationException("Select a valid cell.");
        }
        CellMaster cell = cellMasterRepository.findByCellId(cellId)
                .orElseThrow(() -> new RecruitmentNotificationException("Selected cell was not found."));
        if (!ACTIVE_FLAG.equalsIgnoreCase(cell.getActiveFlag())
                || cell.getWing() == null
                || !ACTIVE_FLAG.equalsIgnoreCase(cell.getWing().getActiveFlag())) {
            throw new RecruitmentNotificationException("Inactive cells cannot be mapped to an employee.");
        }
        return cell;
    }

    private List<EmployeeCellOptionView> loadAvailableCells(EmployeeCellOptionView selectedCell) {
        List<EmployeeCellOptionView> cells = loadActiveCellOptions()
                .stream()
                .collect(Collectors.toCollection(ArrayList::new));
        if (selectedCell == null || selectedCell.active()) {
            return cells;
        }

        Set<Long> visibleCellIds = cells.stream()
                .map(EmployeeCellOptionView::cellId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (!visibleCellIds.contains(selectedCell.cellId())) {
            cells.add(selectedCell);
        }
        return cells;
    }

    private List<EmployeeCellOptionView> loadActiveCellOptions() {
        return cellMasterRepository
                .findByActiveFlagIgnoreCaseAndWing_ActiveFlagIgnoreCaseOrderByCellNameAsc(ACTIVE_FLAG, ACTIVE_FLAG)
                .stream()
                .map(this::toCellOptionView)
                .toList();
    }

    private void saveAuditLog(
            EmployeeEntity employee,
            String actorLoginId,
            CellMaster previousCell,
            CellMaster selectedCell) {
        auditLogRepository.save(buildAuditLog(employee, actorLoginId, previousCell, selectedCell));
    }

    private EmployeeCellMappingAuditLogEntity buildAuditLog(
            EmployeeEntity employee,
            String actorLoginId,
            CellMaster previousCell,
            CellMaster selectedCell) {
        EmployeeCellMappingAuditLogEntity auditLog = new EmployeeCellMappingAuditLogEntity();
        auditLog.setEmployee(employee);
        auditLog.setActorLoginId(StringUtils.hasText(actorLoginId) ? actorLoginId.trim() : "SYSTEM");
        auditLog.setActionType(previousCell == null ? AUDIT_ACTION_ASSIGNED : AUDIT_ACTION_UPDATED);
        auditLog.setPreviousCellId(previousCell == null ? null : previousCell.getCellId());
        auditLog.setNewCellId(selectedCell.getCellId());
        auditLog.setSummary(previousCell == null
                ? "Employee cell assigned"
                : "Employee cell mapping updated");
        auditLog.setDetails(buildAuditDetails(previousCell, selectedCell));
        return auditLog;
    }

    private String buildAuditDetails(CellMaster previousCell, CellMaster selectedCell) {
        String selectedCellName = buildCellDisplayName(selectedCell);
        if (previousCell == null) {
            return "Cell assigned to: " + selectedCellName;
        }
        return "Cell changed from: " + buildCellDisplayName(previousCell) + " to: " + selectedCellName;
    }

    private EmployeeCellMappingEmployeeView toEmployeeView(
            EmployeeEntity employee,
            EmployeeCellOptionView mappedCell) {
        String departmentName = employee.getDepartmentRegistration() == null
                ? "-"
                : defaultIfBlank(employee.getDepartmentRegistration().getDepartmentName(), "-");
        String designationName = employee.getDesignation() == null
                ? "-"
                : defaultIfBlank(employee.getDesignation().getDesignationName(), "-");
        String projectName = "-";
        if (employee.getPreOnboarding() != null
                && employee.getPreOnboarding().getInterviewDetail() != null
                && employee.getPreOnboarding().getInterviewDetail().getRecruitmentNotification() != null
                && employee.getPreOnboarding().getInterviewDetail().getRecruitmentNotification().getProjectMst() != null) {
            projectName = defaultIfBlank(
                    employee.getPreOnboarding().getInterviewDetail().getRecruitmentNotification()
                            .getProjectMst().getProjectName(),
                    "-");
        }

        return new EmployeeCellMappingEmployeeView(
                employee.getEmployeeId(),
                employee.getEmployeeCode(),
                employee.getFullName(),
                employee.getEmail(),
                employee.getMobile(),
                designationName,
                departmentName,
                projectName,
                employee.getRecruitmentType(),
                employee.getJoiningDate(),
                employee.getOnboardingDate(),
                mappedCell);
    }

    private EmployeeCellOptionView toCellOptionView(CellMaster cell) {
        String wingName = cell.getWing() == null ? "-" : defaultIfBlank(cell.getWing().getWingName(), "-");
        Long wingId = cell.getWing() == null ? null : cell.getWing().getWingId();
        boolean active = ACTIVE_FLAG.equalsIgnoreCase(cell.getActiveFlag())
                && cell.getWing() != null
                && ACTIVE_FLAG.equalsIgnoreCase(cell.getWing().getActiveFlag());
        return new EmployeeCellOptionView(
                cell.getCellId(),
                defaultIfBlank(cell.getCellName(), "-"),
                wingId,
                wingName,
                active,
                buildCellDisplayName(cell));
    }

    private EmployeeCellMappingAuditView toAuditView(EmployeeCellMappingAuditLogEntity entity) {
        return new EmployeeCellMappingAuditView(
                entity.getActionType(),
                entity.getActorLoginId(),
                entity.getPreviousCellId(),
                entity.getNewCellId(),
                entity.getSummary(),
                entity.getDetails(),
                entity.getOccurredAt());
    }

    private String buildCellDisplayName(CellMaster cell) {
        String cellName = defaultIfBlank(cell.getCellName(), "-");
        String wingName = cell.getWing() == null ? "" : defaultIfBlank(cell.getWing().getWingName(), "");
        return StringUtils.hasText(wingName) ? wingName + " - " + cellName : cellName;
    }

    private String normalizeRecruitmentType(String recruitmentType) {
        if (!StringUtils.hasText(recruitmentType) || "ALL".equalsIgnoreCase(recruitmentType)) {
            return null;
        }
        String normalized = recruitmentType.trim().toUpperCase();
        return "INTERNAL".equals(normalized) || "EXTERNAL".equals(normalized) ? normalized : null;
    }

    private String buildSearchPattern(String searchText) {
        if (!StringUtils.hasText(searchText)) {
            return null;
        }
        return "%" + searchText.trim().toUpperCase() + "%";
    }

    private List<Long> normalizeEmployeeIds(List<Long> employeeIds) {
        if (employeeIds == null) {
            return List.of();
        }
        return employeeIds.stream()
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .toList();
    }

    private String defaultIfBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }
}
