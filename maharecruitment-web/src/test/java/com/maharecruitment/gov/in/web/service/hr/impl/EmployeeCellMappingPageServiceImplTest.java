package com.maharecruitment.gov.in.web.service.hr.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.maharecruitment.gov.in.master.entity.CellMaster;
import com.maharecruitment.gov.in.master.entity.WingMaster;
import com.maharecruitment.gov.in.master.repository.CellMasterRepository;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeCellMappingAuditLogEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeCellMappingEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeCellMappingAuditLogRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeCellMappingRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.web.service.hr.model.EmployeeCellMappingEditView;

@ExtendWith(MockitoExtension.class)
class EmployeeCellMappingPageServiceImplTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private CellMasterRepository cellMasterRepository;

    @Mock
    private EmployeeCellMappingRepository employeeCellMappingRepository;

    @Mock
    private EmployeeCellMappingAuditLogRepository auditLogRepository;

    @Test
    void searchEmployeesLoadsOnlyEmployeesWithoutCellMappings() {
        EmployeeEntity employee = employee(101L);
        employee.setRecruitmentType("INTERNAL");
        var pageable = PageRequest.of(0, 10);

        when(employeeRepository.findActiveOnboardedWithoutCellMapping("INTERNAL", "%EMPLOYEE%", pageable))
                .thenReturn(new PageImpl<>(List.of(employee), pageable, 1));

        var result = service().searchEmployees("internal", " employee ", pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent())
                .extracting(view -> view.employeeId())
                .containsExactly(101L);
        assertThat(result.getContent().get(0).mappedCell()).isNull();
        verify(employeeRepository).findActiveOnboardedWithoutCellMapping("INTERNAL", "%EMPLOYEE%", pageable);
        verify(employeeCellMappingRepository, never())
                .findByEmployeeEmployeeIdInOrderByEmployeeEmployeeIdAsc(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void loadMappingReturnsSelectedCellAndActiveCellOptions() {
        EmployeeEntity employee = employee(101L);
        CellMaster selectedCell = cell(11L, "Recruitment Cell", "Operations", true);
        CellMaster availableCell = cell(12L, "Payroll Cell", "Finance", true);

        when(employeeRepository.findDetailedByEmployeeId(101L)).thenReturn(Optional.of(employee));
        when(employeeCellMappingRepository.findByEmployeeEmployeeId(101L))
                .thenReturn(Optional.of(mapping(employee, selectedCell)));
        when(cellMasterRepository.findByActiveFlagIgnoreCaseAndWing_ActiveFlagIgnoreCaseOrderByCellNameAsc("Y", "Y"))
                .thenReturn(List.of(selectedCell, availableCell));
        when(auditLogRepository.findTop10ByEmployeeEmployeeIdOrderByOccurredAtDescAuditIdDesc(101L))
                .thenReturn(List.of());

        EmployeeCellMappingEditView result = service().loadMapping(101L);

        assertThat(result.employee().employeeId()).isEqualTo(101L);
        assertThat(result.selectedCell()).isNotNull();
        assertThat(result.selectedCell().displayName()).isEqualTo("Operations - Recruitment Cell");
        assertThat(result.availableCells())
                .extracting(cell -> cell.cellId())
                .containsExactly(11L, 12L);
    }

    @Test
    void updateMappingReturnsFalseWhenCellIsAlreadyMapped() {
        EmployeeEntity employee = employee(101L);
        CellMaster selectedCell = cell(11L, "Recruitment Cell", "Operations", true);
        EmployeeCellMappingEntity mapping = mapping(employee, selectedCell);

        when(employeeRepository.findDetailedByEmployeeId(101L)).thenReturn(Optional.of(employee));
        when(cellMasterRepository.findByCellId(11L)).thenReturn(Optional.of(selectedCell));
        when(employeeCellMappingRepository.findByEmployeeEmployeeId(101L)).thenReturn(Optional.of(mapping));

        boolean changed = service().updateMapping(101L, 11L, "hr-user");

        assertThat(changed).isFalse();
        verify(employeeCellMappingRepository, never()).save(mapping);
        verify(auditLogRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void updateMappingCreatesNewMappingAndAuditLog() {
        EmployeeEntity employee = employee(101L);
        CellMaster selectedCell = cell(12L, "Payroll Cell", "Finance", true);

        when(employeeRepository.findDetailedByEmployeeId(101L)).thenReturn(Optional.of(employee));
        when(cellMasterRepository.findByCellId(12L)).thenReturn(Optional.of(selectedCell));
        when(employeeCellMappingRepository.findByEmployeeEmployeeId(101L)).thenReturn(Optional.empty());

        boolean changed = service().updateMapping(101L, 12L, "hr-user");

        assertThat(changed).isTrue();

        ArgumentCaptor<EmployeeCellMappingEntity> mappingCaptor =
                ArgumentCaptor.forClass(EmployeeCellMappingEntity.class);
        verify(employeeCellMappingRepository).save(mappingCaptor.capture());
        assertThat(mappingCaptor.getValue().getEmployee()).isSameAs(employee);
        assertThat(mappingCaptor.getValue().getCell()).isSameAs(selectedCell);

        ArgumentCaptor<EmployeeCellMappingAuditLogEntity> auditCaptor =
                ArgumentCaptor.forClass(EmployeeCellMappingAuditLogEntity.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getActionType()).isEqualTo("ASSIGNED");
        assertThat(auditCaptor.getValue().getPreviousCellId()).isNull();
        assertThat(auditCaptor.getValue().getNewCellId()).isEqualTo(12L);
        assertThat(auditCaptor.getValue().getDetails()).contains("Cell assigned to: Finance - Payroll Cell");
    }

    @Test
    void updateMappingRejectsInactiveCell() {
        EmployeeEntity employee = employee(101L);
        CellMaster inactiveCell = cell(12L, "Payroll Cell", "Finance", false);

        when(employeeRepository.findDetailedByEmployeeId(101L)).thenReturn(Optional.of(employee));
        when(cellMasterRepository.findByCellId(12L)).thenReturn(Optional.of(inactiveCell));

        assertThatThrownBy(() -> service().updateMapping(101L, 12L, "hr-user"))
                .isInstanceOf(RecruitmentNotificationException.class)
                .hasMessageContaining("Inactive cells");
    }

    @Test
    void updateMappingChangesExistingCellAndStoresAuditTrail() {
        EmployeeEntity employee = employee(101L);
        CellMaster previousCell = cell(11L, "Recruitment Cell", "Operations", true);
        CellMaster selectedCell = cell(12L, "Payroll Cell", "Finance", true);
        EmployeeCellMappingEntity existingMapping = mapping(employee, previousCell);

        when(employeeRepository.findDetailedByEmployeeId(101L)).thenReturn(Optional.of(employee));
        when(cellMasterRepository.findByCellId(12L)).thenReturn(Optional.of(selectedCell));
        when(employeeCellMappingRepository.findByEmployeeEmployeeId(101L)).thenReturn(Optional.of(existingMapping));

        boolean changed = service().updateMapping(101L, 12L, "hr-user");

        assertThat(changed).isTrue();
        assertThat(existingMapping.getCell()).isSameAs(selectedCell);
        verify(employeeCellMappingRepository).save(existingMapping);

        ArgumentCaptor<EmployeeCellMappingAuditLogEntity> auditCaptor =
                ArgumentCaptor.forClass(EmployeeCellMappingAuditLogEntity.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getActionType()).isEqualTo("UPDATED");
        assertThat(auditCaptor.getValue().getPreviousCellId()).isEqualTo(11L);
        assertThat(auditCaptor.getValue().getNewCellId()).isEqualTo(12L);
        assertThat(auditCaptor.getValue().getDetails())
                .contains("Cell changed from: Operations - Recruitment Cell to: Finance - Payroll Cell");
    }

    @Test
    void bulkUpdateMapsMultipleEmployeesToOneCellAndSkipsAlreadyMappedEmployee() {
        EmployeeEntity firstEmployee = employee(101L);
        EmployeeEntity secondEmployee = employee(102L);
        EmployeeEntity thirdEmployee = employee(103L);
        CellMaster previousCell = cell(11L, "Recruitment Cell", "Operations", true);
        CellMaster targetCell = cell(12L, "Payroll Cell", "Finance", true);
        EmployeeCellMappingEntity existingDifferentCell = mapping(secondEmployee, previousCell);
        EmployeeCellMappingEntity existingTargetCell = mapping(thirdEmployee, targetCell);

        when(cellMasterRepository.findByCellId(12L)).thenReturn(Optional.of(targetCell));
        when(employeeRepository.findDetailedByEmployeeIdIn(List.of(101L, 102L, 103L)))
                .thenReturn(List.of(firstEmployee, secondEmployee, thirdEmployee));
        when(employeeCellMappingRepository.findByEmployeeEmployeeIdInOrderByEmployeeEmployeeIdAsc(
                List.of(101L, 102L, 103L)))
                .thenReturn(List.of(existingDifferentCell, existingTargetCell));

        var result = service().updateMappings(12L, List.of(101L, 102L, 103L), "hr-user");

        assertThat(result.requestedCount()).isEqualTo(3);
        assertThat(result.changedCount()).isEqualTo(2);
        assertThat(result.unchangedCount()).isEqualTo(1);
        assertThat(existingDifferentCell.getCell()).isSameAs(targetCell);

        ArgumentCaptor<Iterable<EmployeeCellMappingEntity>> mappingCaptor =
                ArgumentCaptor.forClass(Iterable.class);
        verify(employeeCellMappingRepository).saveAll(mappingCaptor.capture());
        assertThat(mappingCaptor.getValue())
                .extracting(mapping -> mapping.getEmployee().getEmployeeId())
                .containsExactly(101L, 102L);
        assertThat(mappingCaptor.getValue())
                .allSatisfy(mapping -> assertThat(mapping.getCell()).isSameAs(targetCell));

        ArgumentCaptor<Iterable<EmployeeCellMappingAuditLogEntity>> auditCaptor =
                ArgumentCaptor.forClass(Iterable.class);
        verify(auditLogRepository).saveAll(auditCaptor.capture());
        assertThat(auditCaptor.getValue())
                .extracting(EmployeeCellMappingAuditLogEntity::getActionType)
                .containsExactly("ASSIGNED", "UPDATED");
    }

    @Test
    void bulkUpdateRejectsEmptyEmployeeSelection() {
        CellMaster targetCell = cell(12L, "Payroll Cell", "Finance", true);
        when(cellMasterRepository.findByCellId(12L)).thenReturn(Optional.of(targetCell));

        assertThatThrownBy(() -> service().updateMappings(12L, List.of(), "hr-user"))
                .isInstanceOf(RecruitmentNotificationException.class)
                .hasMessageContaining("Select at least one employee");
    }

    private EmployeeCellMappingPageServiceImpl service() {
        return new EmployeeCellMappingPageServiceImpl(
                employeeRepository,
                cellMasterRepository,
                employeeCellMappingRepository,
                auditLogRepository);
    }

    private EmployeeEntity employee(Long employeeId) {
        EmployeeEntity employee = new EmployeeEntity();
        employee.setEmployeeId(employeeId);
        employee.setEmployeeCode("EMP-" + employeeId);
        employee.setFullName("Employee " + employeeId);
        employee.setEmail("employee" + employeeId + "@mahait.org");
        employee.setMobile("9999999999");
        employee.setStatus("ACTIVE");
        employee.setOnboardingDate(LocalDate.of(2026, 7, 1));
        return employee;
    }

    private EmployeeCellMappingEntity mapping(EmployeeEntity employee, CellMaster cell) {
        EmployeeCellMappingEntity mapping = new EmployeeCellMappingEntity();
        mapping.setEmployee(employee);
        mapping.setCell(cell);
        return mapping;
    }

    private CellMaster cell(Long cellId, String cellName, String wingName, boolean active) {
        WingMaster wing = new WingMaster();
        wing.setWingId(cellId + 100L);
        wing.setWingName(wingName);
        wing.setActiveFlag(active ? "Y" : "N");

        CellMaster cell = new CellMaster();
        cell.setCellId(cellId);
        cell.setCellName(cellName);
        cell.setWing(wing);
        cell.setActiveFlag(active ? "Y" : "N");
        return cell;
    }
}
