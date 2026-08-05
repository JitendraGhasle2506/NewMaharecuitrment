package com.maharecruitment.gov.in.web.service.dashboard.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maharecruitment.gov.in.attendance.repository.AttendanceRegisterRepo;
import com.maharecruitment.gov.in.attendance.repository.DailyAttendanceInternalRepository;
import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.department.repository.DepartmentProjectApplicationRepository;
import com.maharecruitment.gov.in.master.entity.CellMaster;
import com.maharecruitment.gov.in.master.entity.WingMaster;
import com.maharecruitment.gov.in.master.repository.CellMasterRepository;
import com.maharecruitment.gov.in.master.repository.ProjectMstRepository;
import com.maharecruitment.gov.in.master.repository.WingMasterRepository;
import com.maharecruitment.gov.in.recruitment.entity.CellReportingAuthorityMappingEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeCellMappingEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeReportingMappingEntity;
import com.maharecruitment.gov.in.recruitment.repository.CellReportingAuthorityMappingRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeCellMappingRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeReportingMappingRepository;
import com.maharecruitment.gov.in.web.service.dashboard.model.HRWingReportView;

@ExtendWith(MockitoExtension.class)
class HRDashboardServiceImplTest {

    @Mock
    private ProjectMstRepository projectMstRepository;

    @Mock
    private WingMasterRepository wingMasterRepository;

    @Mock
    private CellMasterRepository cellMasterRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private EmployeeCellMappingRepository employeeCellMappingRepository;

    @Mock
    private EmployeeReportingMappingRepository employeeReportingMappingRepository;

    @Mock
    private CellReportingAuthorityMappingRepository cellReportingAuthorityMappingRepository;

    @Mock
    private DepartmentProjectApplicationRepository departmentProjectApplicationRepository;

    @Mock
    private DailyAttendanceInternalRepository dailyAttendanceInternalRepository;

    @Mock
    private AttendanceRegisterRepo attendanceRegisterRepo;

    @Test
    void getWingReportCountsEmployeesFromEmployeeCellMappings() {
        WingMaster wing = wing(2L, "MAHAIT Project Cells");
        CellMaster networkCell = cell(27L, "Network Infra Cell", wing);
        CellMaster fieldCell = cell(30L, "Field Operations Cell", wing);

        when(wingMasterRepository.findByWingIdAndActiveFlagIgnoreCase(2L, "Y"))
                .thenReturn(Optional.of(wing));
        when(cellMasterRepository.findByWing_WingIdAndActiveFlagIgnoreCaseAndWing_ActiveFlagIgnoreCaseOrderByCellNameAsc(
                2L,
                "Y",
                "Y"))
                .thenReturn(List.of(fieldCell, networkCell));
        when(projectMstRepository.summarizeProjectCountsByCellAndWingId(2L)).thenReturn(List.of());
        when(employeeCellMappingRepository.findActiveEmployeeMappingsByWingId(2L, "Y", "ACTIVE"))
                .thenReturn(List.of(
                        employeeCellMapping(employee(1L, "NET-1", "Network One", null), networkCell),
                        employeeCellMapping(employee(2L, "NET-2", "Network Two", null), networkCell),
                        employeeCellMapping(employee(3L, "NET-3", "Network Three", null), networkCell),
                        employeeCellMapping(employee(4L, "NET-4", "Network Four", null), networkCell),
                        employeeCellMapping(employee(5L, "FIELD-1", "Field One", null), fieldCell),
                        employeeCellMapping(employee(6L, "FIELD-2", "Field Two", null), fieldCell)));

        Optional<HRWingReportView> result = service().getWingReport(2L);

        assertThat(result).isPresent();
        HRWingReportView report = result.orElseThrow();
        assertThat(report.employeeCount()).isEqualTo(6);
        assertThat(report.cells())
                .extracting(cell -> cell.cellId(), cell -> cell.employeeCount())
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(30L, 2),
                        org.assertj.core.groups.Tuple.tuple(27L, 4));
        verify(employeeCellMappingRepository).findActiveEmployeeMappingsByWingId(2L, "Y", "ACTIVE");
    }

    @Test
    void getWingReportBuildsCellEmployeeAndSubordinateHierarchy() {
        WingMaster wing = wing(2L, "MAHAIT Project Cells");
        CellMaster fieldCell = cell(30L, "Field Operations Cell", wing);
        EmployeeEntity lead = employee(10L, "EMP-010", "Asha Lead", 100L);
        EmployeeEntity senior = employee(11L, "EMP-011", "Bharat Senior", 101L);
        EmployeeEntity junior = employee(12L, "EMP-012", "Chitra Junior", 102L);
        EmployeeEntity fallback = employee(13L, "EMP-013", "Deepa Analyst", 103L);

        when(wingMasterRepository.findByWingIdAndActiveFlagIgnoreCase(2L, "Y"))
                .thenReturn(Optional.of(wing));
        when(cellMasterRepository.findByWing_WingIdAndActiveFlagIgnoreCaseAndWing_ActiveFlagIgnoreCaseOrderByCellNameAsc(
                2L,
                "Y",
                "Y"))
                .thenReturn(List.of(fieldCell));
        when(projectMstRepository.summarizeProjectCountsByCellAndWingId(2L)).thenReturn(List.of());
        when(employeeCellMappingRepository.findActiveEmployeeMappingsByWingId(2L, "Y", "ACTIVE"))
                .thenReturn(List.of(
                        employeeCellMapping(lead, fieldCell),
                        employeeCellMapping(senior, fieldCell),
                        employeeCellMapping(junior, fieldCell),
                        employeeCellMapping(fallback, fieldCell)));
        when(employeeReportingMappingRepository.findByEmployeeIdIn(List.of(10L, 11L, 12L, 13L)))
                .thenReturn(List.of(
                        reportingMapping(1L, 11L, 10L, 100L),
                        reportingMapping(2L, 12L, 11L, 101L)));
        when(cellReportingAuthorityMappingRepository.findByCellCellIdIn(anyCollection()))
                .thenReturn(List.of(cellAuthorityMapping(1L, fieldCell, 100L)));

        HRWingReportView report = service().getWingReport(2L).orElseThrow();

        assertThat(report.cells().getFirst().employees())
                .extracting(
                        employee -> employee.employeeName(),
                        employee -> employee.depth(),
                        employee -> employee.directReportCount(),
                        employee -> employee.reportsToName(),
                        employee -> employee.reportingSource())
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("Asha Lead", 0, 2, "", "CELL"),
                        org.assertj.core.groups.Tuple.tuple("Bharat Senior", 1, 1, "Asha Lead", "DIRECT"),
                        org.assertj.core.groups.Tuple.tuple("Chitra Junior", 2, 0, "Bharat Senior", "DIRECT"),
                        org.assertj.core.groups.Tuple.tuple("Deepa Analyst", 1, 0, "Asha Lead", "CELL"));
    }

    private HRDashboardServiceImpl service() {
        return new HRDashboardServiceImpl(
                projectMstRepository,
                wingMasterRepository,
                cellMasterRepository,
                employeeRepository,
                employeeCellMappingRepository,
                employeeReportingMappingRepository,
                cellReportingAuthorityMappingRepository,
                departmentProjectApplicationRepository,
                dailyAttendanceInternalRepository,
                attendanceRegisterRepo);
    }

    private WingMaster wing(Long wingId, String wingName) {
        WingMaster wing = new WingMaster();
        wing.setWingId(wingId);
        wing.setWingName(wingName);
        wing.setActiveFlag("Y");
        return wing;
    }

    private CellMaster cell(Long cellId, String cellName, WingMaster wing) {
        CellMaster cell = new CellMaster();
        cell.setCellId(cellId);
        cell.setCellName(cellName);
        cell.setActiveFlag("Y");
        cell.setWing(wing);
        return cell;
    }

    private EmployeeEntity employee(Long employeeId, String employeeCode, String name, Long userId) {
        EmployeeEntity employee = new EmployeeEntity();
        employee.setEmployeeId(employeeId);
        employee.setEmployeeCode(employeeCode);
        employee.setFullName(name);
        employee.setStatus("ACTIVE");
        if (userId != null) {
            User user = new User();
            user.setId(userId);
            employee.setUser(user);
        }
        return employee;
    }

    private EmployeeCellMappingEntity employeeCellMapping(EmployeeEntity employee, CellMaster cell) {
        EmployeeCellMappingEntity mapping = new EmployeeCellMappingEntity();
        mapping.setEmployee(employee);
        mapping.setCell(cell);
        return mapping;
    }

    private EmployeeReportingMappingEntity reportingMapping(
            Long mappingId,
            Long employeeId,
            Long managerEmployeeId,
            Long authorityUserId) {
        EmployeeReportingMappingEntity mapping = new EmployeeReportingMappingEntity();
        mapping.setMappingId(mappingId);
        mapping.setEmployeeId(employeeId);
        mapping.setManagerEmployeeId(managerEmployeeId);
        mapping.setHodUserId(authorityUserId);
        mapping.setManagerType("OTHER");
        return mapping;
    }

    private CellReportingAuthorityMappingEntity cellAuthorityMapping(
            Long mappingId,
            CellMaster cell,
            Long authorityUserId) {
        CellReportingAuthorityMappingEntity mapping = new CellReportingAuthorityMappingEntity();
        mapping.setMappingId(mappingId);
        mapping.setCell(cell);
        mapping.setAuthorityUserId(authorityUserId);
        return mapping;
    }
}
