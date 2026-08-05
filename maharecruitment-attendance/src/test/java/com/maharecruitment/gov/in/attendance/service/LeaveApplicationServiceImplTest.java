package com.maharecruitment.gov.in.attendance.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maharecruitment.gov.in.attendance.dto.LeaveApplicationHODDTO;
import com.maharecruitment.gov.in.attendance.entity.LeaveApplicationEntity;
import com.maharecruitment.gov.in.attendance.repository.AttendanceRegisterRepo;
import com.maharecruitment.gov.in.attendance.repository.DailyAttendanceInternalRepository;
import com.maharecruitment.gov.in.attendance.repository.LeaveApplicationRepository;
import com.maharecruitment.gov.in.attendance.repository.TourApplicationRepository;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.recruitment.service.ReportingManagerService;

@ExtendWith(MockitoExtension.class)
class LeaveApplicationServiceImplTest {

    @Mock
    private LeaveApplicationRepository leaveApplicationRepository;

    @Mock
    private DailyAttendanceInternalRepository dailyAttendanceInternalRepository;

    @Mock
    private AttendanceRegisterRepo attendanceRegisterRepo;

    @Mock
    private TourApplicationRepository tourApplicationRepository;

    @Mock
    private ReportingManagerService reportingManagerService;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private LeaveApplicationServiceImpl service;

    @Test
    void pendingLeaveUsesEffectiveAuthorityEmployeesIncludingCellFallback() {
        EmployeeEntity employee = new EmployeeEntity();
        employee.setEmployeeId(101L);
        employee.setEmployeeCode("EMP101");
        employee.setFullName("Asha Patil");

        LeaveApplicationEntity leave = new LeaveApplicationEntity();
        leave.setLeaveId(1L);
        leave.setEmployeeId(101L);
        leave.setLeaveType("CL");
        leave.setLeaveCategory("FULL_DAY");
        leave.setStartDate(LocalDate.of(2026, 8, 10));
        leave.setEndDate(LocalDate.of(2026, 8, 10));
        leave.setApplicationDate(LocalDateTime.of(2026, 8, 5, 9, 30));
        leave.setStatus("PENDING");

        when(reportingManagerService.getEffectiveEmployeeIdsForAuthority(7L))
                .thenReturn(List.of(101L));
        when(leaveApplicationRepository.findByEmployeeIdInAndStatusOrderByApplicationDateDesc(
                List.of(101L), "PENDING"))
                .thenReturn(List.of(leave));
        when(employeeRepository.findAllById(List.of(101L))).thenReturn(List.of(employee));

        List<LeaveApplicationHODDTO> result = service.getPendingLeavesForHOD(7L, null);

        assertEquals(1, result.size());
        assertEquals(101L, result.getFirst().getEmployeeId());
        assertEquals("Asha Patil", result.getFirst().getEmployeeName());
    }
}
