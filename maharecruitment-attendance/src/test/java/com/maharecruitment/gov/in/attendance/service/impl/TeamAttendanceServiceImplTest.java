package com.maharecruitment.gov.in.attendance.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maharecruitment.gov.in.attendance.entity.DailyAttendanceInternalEntity;
import com.maharecruitment.gov.in.attendance.entity.HolidayMasterEntity;
import com.maharecruitment.gov.in.attendance.entity.LeaveApplicationEntity;
import com.maharecruitment.gov.in.attendance.entity.TourApplicationEntity;
import com.maharecruitment.gov.in.attendance.entity.WeekOffWorkingDayEntity;
import com.maharecruitment.gov.in.attendance.repository.DailyAttendanceInternalRepository;
import com.maharecruitment.gov.in.attendance.repository.HolidayRepository;
import com.maharecruitment.gov.in.attendance.repository.LeaveApplicationRepository;
import com.maharecruitment.gov.in.attendance.repository.ManualAttendanceRequestRepository;
import com.maharecruitment.gov.in.attendance.repository.TourApplicationRepository;
import com.maharecruitment.gov.in.attendance.repository.WeekOffWorkingDayRepository;
import com.maharecruitment.gov.in.attendance.service.model.TeamAttendanceOverview;
import com.maharecruitment.gov.in.master.repository.ProjectMstRepository;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeReportingMappingRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeCellMappingRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.recruitment.service.ReportingManagerService;

@ExtendWith(MockitoExtension.class)
class TeamAttendanceServiceImplTest {

    @Mock private ReportingManagerService reportingManagerService;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private EmployeeCellMappingRepository employeeCellMappingRepository;
    @Mock private EmployeeReportingMappingRepository reportingMappingRepository;
    @Mock private ProjectMstRepository projectRepository;
    @Mock private DailyAttendanceInternalRepository dailyAttendanceRepository;
    @Mock private HolidayRepository holidayRepository;
    @Mock private WeekOffWorkingDayRepository workingDayRepository;
    @Mock private LeaveApplicationRepository leaveRepository;
    @Mock private TourApplicationRepository tourRepository;
    @Mock private ManualAttendanceRequestRepository manualAttendanceRepository;

    @InjectMocks
    private TeamAttendanceServiceImpl service;

    @Test
    void overviewUsesBatchDataAndBuildsMonthlyCounts() {
        YearMonth period = YearMonth.of(2026, 8);
        LocalDate startDate = period.atDay(1);
        LocalDate endDate = period.atEndOfMonth();
        EmployeeEntity employee = employee();
        DailyAttendanceInternalEntity present = new DailyAttendanceInternalEntity();
        present.setId(1L);
        present.setEmployeeId(101L);
        present.setAttendanceDate(LocalDate.of(2026, 8, 3));
        present.setStatus("PRESENT");
        present.setInTime("09:30");
        present.setOutTime("18:00");
        LeaveApplicationEntity leave = new LeaveApplicationEntity();
        leave.setEmployeeId(101L);
        leave.setStartDate(LocalDate.of(2026, 8, 4));
        leave.setEndDate(LocalDate.of(2026, 8, 4));
        leave.setLeaveType("CL");
        leave.setStatus("APPROVED");
        TourApplicationEntity tour = new TourApplicationEntity();
        tour.setEmployeeId(101L);
        tour.setStartDate(LocalDate.of(2026, 8, 5));
        tour.setEndDate(LocalDate.of(2026, 8, 5));
        tour.setStatus("APPROVED");
        HolidayMasterEntity holiday = new HolidayMasterEntity();
        holiday.setHolidayDate(LocalDate.of(2026, 8, 6));

        when(reportingManagerService.getEffectiveEmployeeIdsForAuthority(7L)).thenReturn(List.of(101L));
        when(employeeRepository.findByEmployeeIdInOrderByFullNameAscEmployeeIdAsc(List.of(101L)))
                .thenReturn(List.of(employee));
        when(dailyAttendanceRepository.findByEmployeeIdInAndAttendanceDateBetween(
                List.of(101L), startDate, endDate)).thenReturn(List.of(present));
        when(holidayRepository.findByHolidayDateBetween(startDate, endDate)).thenReturn(List.of(holiday));
        when(workingDayRepository.findByWorkingDateBetween(startDate, endDate)).thenReturn(List.of());
        when(leaveRepository.findApprovedOverlappingPeriod(List.of(101L), startDate, endDate))
                .thenReturn(List.of(leave));
        when(tourRepository.findApprovedOverlappingPeriod(List.of(101L), startDate, endDate))
                .thenReturn(List.of(tour));
        when(manualAttendanceRepository.findByUserIdInAndAttendanceDateBetween(
                List.of(101L), startDate, endDate)).thenReturn(List.of());
        when(reportingMappingRepository.findByEmployeeIdIn(List.of(101L))).thenReturn(List.of());
        when(employeeCellMappingRepository.findByEmployeeEmployeeIdInOrderByEmployeeEmployeeIdAsc(List.of(101L)))
                .thenReturn(List.of());

        TeamAttendanceOverview overview = service.getOverview(7L, period);

        assertThat(overview.teamSize()).isEqualTo(1);
        assertThat(overview.totalPresentDays()).isEqualTo(1);
        assertThat(overview.totalAbsentDays()).isEqualTo(17);
        assertThat(overview.totalLeaveDays()).isEqualTo(1);
        assertThat(overview.totalTourDays()).isEqualTo(1);
        assertThat(overview.attendanceRate()).isEqualTo(5);
        assertThat(overview.members().getFirst().latestStatus()).isEqualTo("ABSENT");
        assertThat(overview.members().getFirst().weekOffDays()).isEqualTo(10);
        assertThat(overview.members().getFirst().holidayDays()).isEqualTo(1);
    }

    @Test
    void overviewCountsPresentAbsentAndLeaveEmployeesForToday() {
        LocalDate today = LocalDate.now();
        YearMonth period = YearMonth.from(today);
        LocalDate startDate = period.atDay(1);
        LocalDate endDate = period.atEndOfMonth();
        List<Long> employeeIds = List.of(101L, 102L, 103L);
        List<EmployeeEntity> employees = List.of(
                employee(101L, "Asha Patil", today.minusMonths(1)),
                employee(102L, "Ravi Joshi", today.minusMonths(1)),
                employee(103L, "Neha Shah", today.minusMonths(1)));

        DailyAttendanceInternalEntity present = new DailyAttendanceInternalEntity();
        present.setId(1L);
        present.setEmployeeId(101L);
        present.setAttendanceDate(today);
        present.setStatus("PRESENT");

        LeaveApplicationEntity leave = new LeaveApplicationEntity();
        leave.setEmployeeId(102L);
        leave.setStartDate(today);
        leave.setEndDate(today);
        leave.setLeaveType("CL");
        leave.setStatus("APPROVED");

        WeekOffWorkingDayEntity workingDay = new WeekOffWorkingDayEntity();
        workingDay.setWorkingDate(today);

        when(reportingManagerService.getEffectiveEmployeeIdsForAuthority(7L)).thenReturn(employeeIds);
        when(employeeRepository.findByEmployeeIdInOrderByFullNameAscEmployeeIdAsc(employeeIds))
                .thenReturn(employees);
        when(dailyAttendanceRepository.findByEmployeeIdInAndAttendanceDateBetween(
                employeeIds,
                startDate,
                endDate)).thenReturn(List.of(present));
        when(holidayRepository.findByHolidayDateBetween(startDate, endDate)).thenReturn(List.of());
        when(workingDayRepository.findByWorkingDateBetween(startDate, endDate)).thenReturn(List.of(workingDay));
        when(leaveRepository.findApprovedOverlappingPeriod(employeeIds, startDate, endDate))
                .thenReturn(List.of(leave));
        when(tourRepository.findApprovedOverlappingPeriod(employeeIds, startDate, endDate))
                .thenReturn(List.of());
        when(manualAttendanceRepository.findByUserIdInAndAttendanceDateBetween(
                employeeIds,
                startDate,
                endDate)).thenReturn(List.of());
        when(reportingMappingRepository.findByEmployeeIdIn(employeeIds)).thenReturn(List.of());
        when(employeeCellMappingRepository.findByEmployeeEmployeeIdInOrderByEmployeeEmployeeIdAsc(employeeIds))
                .thenReturn(List.of());

        TeamAttendanceOverview overview = service.getOverview(7L, period);

        assertThat(overview.todayPresentCount()).isEqualTo(1);
        assertThat(overview.todayAbsentCount()).isEqualTo(1);
        assertThat(overview.todayLeaveCount()).isEqualTo(1);
    }

    @Test
    void unauthorizedEmployeeIsRejectedBeforeEmployeeOrAttendanceQueries() {
        when(reportingManagerService.getEffectiveEmployeeIdsForAuthority(7L)).thenReturn(List.of(101L));

        assertThat(service.getAuthorizedMember(7L, 999L, YearMonth.of(2026, 8))).isEmpty();

        verify(employeeRepository, never()).findByEmployeeIdInOrderByFullNameAscEmployeeIdAsc(List.of(999L));
        verify(dailyAttendanceRepository, never()).findByEmployeeIdInAndAttendanceDateBetween(
                List.of(999L), LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));
    }

    private EmployeeEntity employee() {
        return employee(101L, "Asha Patil", LocalDate.of(2026, 8, 1));
    }

    private EmployeeEntity employee(Long employeeId, String fullName, LocalDate joiningDate) {
        EmployeeEntity employee = new EmployeeEntity();
        employee.setEmployeeId(employeeId);
        employee.setEmployeeCode("EMP" + employeeId);
        employee.setFullName(fullName);
        employee.setRecruitmentType("INTERNAL");
        employee.setStatus("ACTIVE");
        employee.setJoiningDate(joiningDate);
        return employee;
    }
}
