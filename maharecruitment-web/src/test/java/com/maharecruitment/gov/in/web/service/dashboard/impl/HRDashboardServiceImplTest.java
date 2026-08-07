package com.maharecruitment.gov.in.web.service.dashboard.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;

import com.maharecruitment.gov.in.attendance.repository.AttendanceCellSummaryProjection;
import com.maharecruitment.gov.in.attendance.repository.AttendanceCheckInSummaryProjection;
import com.maharecruitment.gov.in.attendance.repository.AttendanceEmployeeDetailProjection;
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
import com.maharecruitment.gov.in.web.service.dashboard.model.HRDashboardView;

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

    @Mock
    private AttendanceCheckInSummaryProjection attendanceSummary;

    @Mock
    private AttendanceCellSummaryProjection cellAttendanceSummary;

    @Mock
    private AttendanceEmployeeDetailProjection attendanceEmployeeDetail;

    @Test
    void getDashboardBuildsSingleQueryCheckInBreakdown() {
        when(projectMstRepository.findByProjectScopeTypeOrderByProjectNameAsc(any())).thenReturn(List.of());
        when(wingMasterRepository.countByActiveFlagIgnoreCase("Y")).thenReturn(3L);
        when(cellMasterRepository.countByActiveFlagIgnoreCaseAndWing_ActiveFlagIgnoreCase("Y", "Y"))
                .thenReturn(12L);
        when(employeeRepository.summarizeEmployeeCountsByDepartment()).thenReturn(List.of());
        when(employeeRepository.count()).thenReturn(20L);
        when(dailyAttendanceInternalRepository.summarizeAttendanceByDate(
                any(LocalDate.class),
                eq(LocalTime.of(9, 45)),
                eq(LocalTime.of(10, 15)),
                eq(LocalTime.of(11, 0))))
                .thenReturn(attendanceSummary);
        when(attendanceSummary.getPresentCount()).thenReturn(9L);
        when(attendanceSummary.getCheckedInCount()).thenReturn(8L);
        when(attendanceSummary.getEarlyCount()).thenReturn(3L);
        when(attendanceSummary.getStandardCount()).thenReturn(3L);
        when(attendanceSummary.getLateCount()).thenReturn(1L);
        when(attendanceSummary.getAfterElevenCount()).thenReturn(1L);
        when(attendanceRegisterRepo.countExternalPresentByMonthYearDay(any(), any(), any())).thenReturn(2L);
        HRDashboardView result = service().getDashboard();

        assertThat(result.presentEmployees()).isEqualTo(11);
        assertThat(result.attendanceSummary().checkedInEmployees()).isEqualTo(8);
        assertThat(result.attendanceSummary().earlyCheckIns()).isEqualTo(3);
        assertThat(result.attendanceSummary().standardCheckIns()).isEqualTo(3);
        assertThat(result.attendanceSummary().lateCheckIns()).isEqualTo(1);
        assertThat(result.attendanceSummary().afterElevenCheckIns()).isEqualTo(1);
        assertThat(result.totalWings()).isEqualTo(3);
        assertThat(result.totalCells()).isEqualTo(12);
        verify(dailyAttendanceInternalRepository).summarizeAttendanceByDate(
                any(LocalDate.class),
                eq(LocalTime.of(9, 45)),
                eq(LocalTime.of(10, 15)),
                eq(LocalTime.of(11, 0)));
        verify(employeeRepository).summarizeEmployeeCountsByDepartment();
        verify(dailyAttendanceInternalRepository, never()).summarizeAttendanceByCell(
                any(LocalDate.class),
                anyInt(),
                anyInt(),
                anyInt(),
                eq("Y"),
                eq("ACTIVE"));
        verify(wingMasterRepository, never()).findByActiveFlagIgnoreCaseOrderByWingNameAsc("Y");
        verify(cellMasterRepository, never())
                .findByActiveFlagIgnoreCaseAndWing_ActiveFlagIgnoreCaseOrderByCellNameAsc("Y", "Y");
    }

    @Test
    void getTodayAttendanceLoadsCellDetailsOnlyForDedicatedPage() {
        when(employeeRepository.countActiveAttendanceEmployees()).thenReturn(20L);
        when(dailyAttendanceInternalRepository.summarizeAttendanceByDate(
                any(LocalDate.class),
                eq(LocalTime.of(9, 45)),
                eq(LocalTime.of(10, 15)),
                eq(LocalTime.of(11, 0))))
                .thenReturn(attendanceSummary);
        when(attendanceSummary.getPresentCount()).thenReturn(9L);
        when(attendanceSummary.getCheckedInCount()).thenReturn(8L);
        when(attendanceSummary.getEarlyCount()).thenReturn(3L);
        when(attendanceSummary.getStandardCount()).thenReturn(3L);
        when(attendanceSummary.getLateCount()).thenReturn(1L);
        when(attendanceSummary.getAfterElevenCount()).thenReturn(1L);
        when(attendanceRegisterRepo.countExternalPresentByMonthYearDay(any(), any(), any())).thenReturn(2L);
        when(dailyAttendanceInternalRepository.summarizeAttendanceByCell(
                any(LocalDate.class),
                anyInt(),
                anyInt(),
                anyInt(),
                eq("Y"),
                eq("ACTIVE")))
                .thenReturn(List.of(cellAttendanceSummary));
        when(cellAttendanceSummary.getCellId()).thenReturn(27L);
        when(cellAttendanceSummary.getCellName()).thenReturn("Network Infra Cell");
        when(cellAttendanceSummary.getWingName()).thenReturn("MAHAIT Project Cells");
        when(cellAttendanceSummary.getTotalEmployees()).thenReturn(12L);
        when(cellAttendanceSummary.getPresentEmployees()).thenReturn(9L);

        var result = service().getTodayAttendance();

        assertThat(result.totalEmployees()).isEqualTo(20);
        assertThat(result.presentEmployees()).isEqualTo(11);
        assertThat(result.absentEmployees()).isEqualTo(9);
        assertThat(result.checkIns().checkedInEmployees()).isEqualTo(8);
        assertThat(result.checkIns().lateCheckIns()).isEqualTo(1);
        assertThat(result.checkIns().afterElevenCheckIns()).isEqualTo(1);
        assertThat(result.cells()).singleElement().satisfies(cell -> {
            assertThat(cell.cellName()).isEqualTo("Network Infra Cell");
            assertThat(cell.presentEmployees()).isEqualTo(9);
            assertThat(cell.absentEmployees()).isEqualTo(3);
            assertThat(cell.presentPercent()).isEqualTo(75);
        });
    }

    @Test
    void getTodayAttendanceDetailsLoadsSelectedBucketOnDemand() {
        CellMaster cell = cell(27L, "Network Infra Cell", wing(2L, "Operations"));
        when(cellMasterRepository.findById(27L)).thenReturn(Optional.of(cell));
        when(attendanceEmployeeDetail.getEmployeeId()).thenReturn(101L);
        when(attendanceEmployeeDetail.getEmployeeCode()).thenReturn("EMP-101");
        when(attendanceEmployeeDetail.getFullName()).thenReturn("Asha Employee");
        when(attendanceEmployeeDetail.getRecruitmentType()).thenReturn("INTERNAL");
        when(attendanceEmployeeDetail.getAttendanceStatus()).thenReturn("PRESENT");
        when(attendanceEmployeeDetail.getCheckInTime()).thenReturn(LocalTime.of(10, 32));
        when(dailyAttendanceInternalRepository.findAttendanceEmployeeDetails(
                any(LocalDate.class),
                anyInt(),
                anyInt(),
                anyInt(),
                eq("LATE"),
                eq(27L),
                eq(LocalTime.of(9, 45)),
                eq(LocalTime.of(10, 15)),
                eq(LocalTime.of(11, 0)),
                any(Pageable.class)))
                .thenReturn(new SliceImpl<>(
                        List.of(attendanceEmployeeDetail),
                        PageRequest.of(0, 25),
                        false));

        var result = service().getTodayAttendanceDetails("late", 27L, 0, 25);

        assertThat(result.category().name()).isEqualTo("LATE");
        assertThat(result.cellName()).isEqualTo("Network Infra Cell");
        assertThat(result.employees()).singleElement().satisfies(employee -> {
            assertThat(employee.fullName()).isEqualTo("Asha Employee");
            assertThat(employee.checkInTime()).isEqualTo(LocalTime.of(10, 32));
        });
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    void getWingReportsLoadsDirectoryDataOnlyForDedicatedPage() {
        WingMaster wing = wing(2L, "MAHAIT Project Cells");
        CellMaster networkCell = cell(27L, "Network Infra Cell", wing);
        CellMaster fieldCell = cell(30L, "Field Operations Cell", wing);
        when(wingMasterRepository.findByActiveFlagIgnoreCaseOrderByWingNameAsc("Y")).thenReturn(List.of(wing));
        when(cellMasterRepository.findByActiveFlagIgnoreCaseAndWing_ActiveFlagIgnoreCaseOrderByCellNameAsc("Y", "Y"))
                .thenReturn(List.of(fieldCell, networkCell));
        when(projectMstRepository.summarizeProjectCountsByCell()).thenReturn(List.of());
        when(employeeCellMappingRepository.summarizeActiveEmployeesByCell("Y", "ACTIVE")).thenReturn(List.of());

        var result = service().getWingReports();

        assertThat(result.totalWings()).isEqualTo(1);
        assertThat(result.totalCells()).isEqualTo(2);
        assertThat(result.wings()).singleElement().satisfies(item -> {
            assertThat(item.wingName()).isEqualTo("MAHAIT Project Cells");
            assertThat(item.cellCount()).isEqualTo(2);
            assertThat(item.projectCount()).isZero();
            assertThat(item.employeeCount()).isZero();
        });
    }

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
