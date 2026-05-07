package com.maharecruitment.gov.in.attendance.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maharecruitment.gov.in.attendance.entity.DailyAttendanceInternalEntity;
import com.maharecruitment.gov.in.attendance.entity.HolidayMasterEntity;
import com.maharecruitment.gov.in.attendance.entity.LeaveApplicationEntity;
import com.maharecruitment.gov.in.attendance.entity.TourApplicationEntity;
import com.maharecruitment.gov.in.attendance.repository.DailyAttendanceInternalRepository;
import com.maharecruitment.gov.in.attendance.repository.HolidayRepository;
import com.maharecruitment.gov.in.attendance.repository.LeaveApplicationRepository;
import com.maharecruitment.gov.in.attendance.repository.TourApplicationRepository;
import com.maharecruitment.gov.in.attendance.service.model.InternalAttendanceReportFilter;
import com.maharecruitment.gov.in.attendance.service.model.InternalAttendanceReportRow;
import com.maharecruitment.gov.in.attendance.service.model.InternalAttendanceReportView;
import com.maharecruitment.gov.in.auth.entity.DepartmentRegistrationEntity;
import com.maharecruitment.gov.in.master.entity.AgencyMaster;
import com.maharecruitment.gov.in.master.entity.ManpowerDesignationMaster;
import com.maharecruitment.gov.in.master.entity.ProjectMst;
import com.maharecruitment.gov.in.master.entity.SubDepartment;
import com.maharecruitment.gov.in.master.repository.ProjectMstRepository;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeReportingMappingEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeReportingMappingRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;

@ExtendWith(MockitoExtension.class)
class InternalEmployeeAttendanceReportServiceImplTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private EmployeeReportingMappingRepository employeeReportingMappingRepository;

    @Mock
    private DailyAttendanceInternalRepository dailyAttendanceInternalRepository;

    @Mock
    private HolidayRepository holidayRepository;

    @Mock
    private LeaveApplicationRepository leaveApplicationRepository;

    @Mock
    private TourApplicationRepository tourApplicationRepository;

    @Mock
    private ProjectMstRepository projectRepository;

    private InternalEmployeeAttendanceReportServiceImpl service;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(
                Instant.parse("2026-05-10T06:00:00Z"),
                ZoneId.of("Asia/Kolkata"));
        service = new InternalEmployeeAttendanceReportServiceImpl(
                employeeRepository,
                employeeReportingMappingRepository,
                dailyAttendanceInternalRepository,
                holidayRepository,
                leaveApplicationRepository,
                tourApplicationRepository,
                projectRepository,
                fixedClock);
    }

    @Test
    void buildReportCombinesSyncedAttendanceAndDerivedStatuses() {
        EmployeeEntity employee = buildEmployee(101L, "EMP000101", "Aarav Sharma", "ACTIVE");
        EmployeeReportingMappingEntity mapping = buildMapping(employee.getEmployeeId(), 44L);
        ProjectMst project = buildProject(44L, "Cloud Mission");
        LocalDate startDate = YearMonth.of(2026, 5).atDay(1);
        LocalDate endDate = YearMonth.of(2026, 5).atEndOfMonth();

        when(employeeRepository.findDetailedInternalEmployeesForAttendanceReport(null, null, null, "ACTIVE"))
                .thenReturn(List.of(employee));
        when(employeeReportingMappingRepository.findByEmployeeIdIn(List.of(employee.getEmployeeId())))
                .thenReturn(List.of(mapping));
        when(projectRepository.findAllById(anyCollection()))
                .thenReturn(List.of(project));
        when(dailyAttendanceInternalRepository.findByEmployeeIdInAndAttendanceDateBetween(
                List.of(employee.getEmployeeId()),
                startDate,
                endDate))
                .thenReturn(List.of(buildAttendance(employee.getEmployeeId(), LocalDate.of(2026, 5, 1), "PRESENT")));
        when(leaveApplicationRepository.findByEmployeeIdInAndStatusOrderByApplicationDateDesc(
                List.of(employee.getEmployeeId()),
                "APPROVED"))
                .thenReturn(List.of(buildLeave(employee.getEmployeeId(), LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 4))));
        when(tourApplicationRepository.findByEmployeeIdInAndStatusOrderByApplicationDateDesc(
                List.of(employee.getEmployeeId()),
                "APPROVED"))
                .thenReturn(List.of(buildTour(employee.getEmployeeId(), LocalDate.of(2026, 5, 5), LocalDate.of(2026, 5, 5))));
        when(holidayRepository.findByHolidayDateBetween(startDate, endDate))
                .thenReturn(List.of(buildHoliday(LocalDate.of(2026, 5, 6), "Foundation Day")));

        InternalAttendanceReportFilter filter = new InternalAttendanceReportFilter();
        filter.setMonth(5);
        filter.setYear(2026);
        filter.setEmployeeStatus("ACTIVE");

        InternalAttendanceReportView report = service.buildReport(filter);

        assertEquals(1, report.getRows().size());
        InternalAttendanceReportRow row = report.getRows().get(0);
        assertEquals("Talent Hive", row.getAgencyName());
        assertEquals("Cloud Mission", row.getProjectName());
        assertEquals("P", row.getDailyStatus().get(1));
        assertEquals("W", row.getDailyStatus().get(2));
        assertEquals("W", row.getDailyStatus().get(3));
        assertEquals("L", row.getDailyStatus().get(4));
        assertEquals("T", row.getDailyStatus().get(5));
        assertEquals("H", row.getDailyStatus().get(6));
        assertEquals("A", row.getDailyStatus().get(7));
        assertEquals("A", row.getDailyStatus().get(8));
        assertEquals("", row.getDailyStatus().get(11));

        assertEquals(1, row.getPresentCount());
        assertEquals(2, row.getAbsentCount());
        assertEquals(1, row.getLeaveCount());
        assertEquals(1, row.getTourCount());
        assertEquals(1, row.getHolidayCount());
        assertEquals(4, row.getWeekOffCount());

        assertEquals(1, report.getSummary().getEmployeeCount());
        assertEquals(31, report.getSummary().getTotalDaysInMonth());
        assertEquals(5, report.getSummary().getOfficeDayCount());
        assertEquals(1, report.getSummary().getTotalHolidayCount());
        assertEquals(4, report.getSummary().getTotalWeekOffCount());
    }

    @Test
    void buildReportFiltersRowsUsingMappedProjectAndSearchText() {
        EmployeeEntity alphaEmployee = buildEmployee(101L, "EMP000101", "Aarav Sharma", "ACTIVE");
        EmployeeEntity betaEmployee = buildEmployee(202L, "EMP000202", "Bhavesh Kale", "ACTIVE");
        EmployeeReportingMappingEntity alphaMapping = buildMapping(alphaEmployee.getEmployeeId(), 44L);
        EmployeeReportingMappingEntity betaMapping = buildMapping(betaEmployee.getEmployeeId(), 55L);
        ProjectMst alphaProject = buildProject(44L, "Cloud Mission");
        ProjectMst betaProject = buildProject(55L, "Data Modernization");
        LocalDate startDate = YearMonth.of(2026, 5).atDay(1);
        LocalDate endDate = YearMonth.of(2026, 5).atEndOfMonth();

        when(employeeRepository.findDetailedInternalEmployeesForAttendanceReport(77L, null, null, "ACTIVE"))
                .thenReturn(List.of(alphaEmployee, betaEmployee));
        when(employeeReportingMappingRepository.findByEmployeeIdIn(List.of(alphaEmployee.getEmployeeId(), betaEmployee.getEmployeeId())))
                .thenReturn(List.of(alphaMapping, betaMapping));
        when(projectRepository.findAllById(anyCollection()))
                .thenReturn(List.of(alphaProject, betaProject));
        when(dailyAttendanceInternalRepository.findByEmployeeIdInAndAttendanceDateBetween(
                eq(List.of(alphaEmployee.getEmployeeId())),
                eq(startDate),
                eq(endDate)))
                .thenReturn(List.of());
        when(leaveApplicationRepository.findByEmployeeIdInAndStatusOrderByApplicationDateDesc(
                List.of(alphaEmployee.getEmployeeId()),
                "APPROVED"))
                .thenReturn(List.of());
        when(tourApplicationRepository.findByEmployeeIdInAndStatusOrderByApplicationDateDesc(
                List.of(alphaEmployee.getEmployeeId()),
                "APPROVED"))
                .thenReturn(List.of());
        when(holidayRepository.findByHolidayDateBetween(startDate, endDate))
                .thenReturn(List.of());

        InternalAttendanceReportFilter filter = new InternalAttendanceReportFilter();
        filter.setAgencyId(77L);
        filter.setMonth(5);
        filter.setYear(2026);
        filter.setEmployeeStatus("ACTIVE");
        filter.setProjectId(44L);
        filter.setSearchText("cloud");

        InternalAttendanceReportView report = service.buildReport(filter);

        assertEquals(1, report.getRows().size());
        assertEquals(alphaEmployee.getEmployeeId(), report.getRows().get(0).getEmployeeId());
        assertTrue(report.getRows().stream().allMatch(row -> row.getProjectId().equals(44L)));
    }

    @Test
    void buildReportTreatsWeekOffWithPunchTimesAsPresent() {
        EmployeeEntity employee = buildEmployee(101L, "EMP000101", "Aarav Sharma", "ACTIVE");
        EmployeeReportingMappingEntity mapping = buildMapping(employee.getEmployeeId(), 44L);
        ProjectMst project = buildProject(44L, "Cloud Mission");
        LocalDate startDate = YearMonth.of(2026, 5).atDay(1);
        LocalDate endDate = YearMonth.of(2026, 5).atEndOfMonth();

        DailyAttendanceInternalEntity attendance = buildAttendance(
                employee.getEmployeeId(),
                LocalDate.of(2026, 5, 2),
                "WEEK_OFF");

        when(employeeRepository.findDetailedInternalEmployeesForAttendanceReport(null, null, null, "ACTIVE"))
                .thenReturn(List.of(employee));
        when(employeeReportingMappingRepository.findByEmployeeIdIn(List.of(employee.getEmployeeId())))
                .thenReturn(List.of(mapping));
        when(projectRepository.findAllById(anyCollection()))
                .thenReturn(List.of(project));
        when(dailyAttendanceInternalRepository.findByEmployeeIdInAndAttendanceDateBetween(
                List.of(employee.getEmployeeId()),
                startDate,
                endDate))
                .thenReturn(List.of(attendance));
        when(leaveApplicationRepository.findByEmployeeIdInAndStatusOrderByApplicationDateDesc(
                List.of(employee.getEmployeeId()),
                "APPROVED"))
                .thenReturn(List.of());
        when(tourApplicationRepository.findByEmployeeIdInAndStatusOrderByApplicationDateDesc(
                List.of(employee.getEmployeeId()),
                "APPROVED"))
                .thenReturn(List.of());
        when(holidayRepository.findByHolidayDateBetween(startDate, endDate))
                .thenReturn(List.of());

        InternalAttendanceReportFilter filter = new InternalAttendanceReportFilter();
        filter.setMonth(5);
        filter.setYear(2026);
        filter.setEmployeeStatus("ACTIVE");

        InternalAttendanceReportView report = service.buildReport(filter);

        InternalAttendanceReportRow row = report.getRows().get(0);
        assertEquals("P", row.getDailyStatus().get(2));
        assertEquals(1, row.getPresentCount());
        assertEquals(6, row.getAbsentCount());
        assertEquals(3, row.getWeekOffCount());
    }

    @Test
    void buildReportPrefersConfiguredHolidayOverSyncedAttendance() {
        EmployeeEntity employee = buildEmployee(101L, "EMP000101", "Aarav Sharma", "ACTIVE");
        EmployeeReportingMappingEntity mapping = buildMapping(employee.getEmployeeId(), 44L);
        ProjectMst project = buildProject(44L, "Cloud Mission");
        LocalDate startDate = YearMonth.of(2026, 5).atDay(1);
        LocalDate endDate = YearMonth.of(2026, 5).atEndOfMonth();

        when(employeeRepository.findDetailedInternalEmployeesForAttendanceReport(null, null, null, "ACTIVE"))
                .thenReturn(List.of(employee));
        when(employeeReportingMappingRepository.findByEmployeeIdIn(List.of(employee.getEmployeeId())))
                .thenReturn(List.of(mapping));
        when(projectRepository.findAllById(anyCollection()))
                .thenReturn(List.of(project));
        when(dailyAttendanceInternalRepository.findByEmployeeIdInAndAttendanceDateBetween(
                List.of(employee.getEmployeeId()),
                startDate,
                endDate))
                .thenReturn(List.of(buildAttendance(employee.getEmployeeId(), LocalDate.of(2026, 5, 6), "PRESENT")));
        when(leaveApplicationRepository.findByEmployeeIdInAndStatusOrderByApplicationDateDesc(
                List.of(employee.getEmployeeId()),
                "APPROVED"))
                .thenReturn(List.of());
        when(tourApplicationRepository.findByEmployeeIdInAndStatusOrderByApplicationDateDesc(
                List.of(employee.getEmployeeId()),
                "APPROVED"))
                .thenReturn(List.of());
        when(holidayRepository.findByHolidayDateBetween(startDate, endDate))
                .thenReturn(List.of(buildHoliday(LocalDate.of(2026, 5, 6), "Foundation Day")));

        InternalAttendanceReportFilter filter = new InternalAttendanceReportFilter();
        filter.setMonth(5);
        filter.setYear(2026);
        filter.setEmployeeStatus("ACTIVE");

        InternalAttendanceReportView report = service.buildReport(filter);

        InternalAttendanceReportRow row = report.getRows().get(0);
        assertEquals("H", row.getDailyStatus().get(6));
        assertEquals(1, row.getHolidayCount());
    }

    private EmployeeEntity buildEmployee(Long employeeId, String employeeCode, String fullName, String status) {
        DepartmentRegistrationEntity department = new DepartmentRegistrationEntity();
        department.setDepartmentRegistrationId(10L);
        department.setDepartmentName("Human Resource");

        SubDepartment subDepartment = new SubDepartment();
        subDepartment.setSubDeptId(22L);
        subDepartment.setSubDeptName("Operations");

        ManpowerDesignationMaster designation = ManpowerDesignationMaster.builder()
                .designationName("Software Engineer")
                .category("Technology")
                .build();

        EmployeeEntity employee = new EmployeeEntity();
        employee.setEmployeeId(employeeId);
        employee.setEmployeeCode(employeeCode);
        employee.setFullName(fullName);
        employee.setStatus(status);
        employee.setRecruitmentType("INTERNAL");
        employee.setJoiningDate(LocalDate.of(2026, 5, 1));
        employee.setAgency(buildAgency(77L, "Talent Hive"));
        employee.setDepartmentRegistration(department);
        employee.setSubDepartment(subDepartment);
        employee.setDesignation(designation);
        employee.setRequestId("REQ-" + employeeId);
        employee.setLevelCode("L1");
        return employee;
    }

    private EmployeeReportingMappingEntity buildMapping(Long employeeId, Long projectId) {
        EmployeeReportingMappingEntity mapping = new EmployeeReportingMappingEntity();
        mapping.setMappingId(employeeId);
        mapping.setEmployeeId(employeeId);
        mapping.setProjectId(projectId);
        mapping.setManagerType("PM");
        mapping.setManagerEmployeeId(501L);
        mapping.setHodUserId(601L);
        return mapping;
    }

    private ProjectMst buildProject(Long projectId, String projectName) {
        ProjectMst project = new ProjectMst();
        project.setProjectId(projectId);
        project.setProjectName(projectName);
        return project;
    }

    private AgencyMaster buildAgency(Long agencyId, String agencyName) {
        AgencyMaster agency = new AgencyMaster();
        agency.setAgencyId(agencyId);
        agency.setAgencyName(agencyName);
        return agency;
    }

    private DailyAttendanceInternalEntity buildAttendance(Long employeeId, LocalDate date, String status) {
        DailyAttendanceInternalEntity attendance = new DailyAttendanceInternalEntity();
        attendance.setId(employeeId + date.getDayOfMonth());
        attendance.setEmployeeId(employeeId);
        attendance.setAttendanceDate(date);
        attendance.setStatus(status);
        attendance.setInTime("09:00");
        attendance.setOutTime("18:00");
        return attendance;
    }

    private LeaveApplicationEntity buildLeave(Long employeeId, LocalDate startDate, LocalDate endDate) {
        LeaveApplicationEntity leave = new LeaveApplicationEntity();
        leave.setEmployeeId(employeeId);
        leave.setStartDate(startDate);
        leave.setEndDate(endDate);
        leave.setStatus("APPROVED");
        return leave;
    }

    private TourApplicationEntity buildTour(Long employeeId, LocalDate startDate, LocalDate endDate) {
        TourApplicationEntity tour = new TourApplicationEntity();
        tour.setEmployeeId(employeeId);
        tour.setStartDate(startDate);
        tour.setEndDate(endDate);
        tour.setStatus("APPROVED");
        return tour;
    }

    private HolidayMasterEntity buildHoliday(LocalDate holidayDate, String holidayName) {
        HolidayMasterEntity holiday = new HolidayMasterEntity();
        holiday.setHolidayDate(holidayDate);
        holiday.setHolidayName(holidayName);
        return holiday;
    }
}
