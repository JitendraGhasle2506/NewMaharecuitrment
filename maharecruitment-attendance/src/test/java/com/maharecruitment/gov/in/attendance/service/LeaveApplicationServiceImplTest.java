package com.maharecruitment.gov.in.attendance.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.maharecruitment.gov.in.attendance.dto.LeaveApplicationHODDTO;
import com.maharecruitment.gov.in.attendance.entity.LeaveApplicationEntity;
import com.maharecruitment.gov.in.attendance.repository.AttendanceRegisterRepo;
import com.maharecruitment.gov.in.attendance.repository.DailyAttendanceInternalRepository;
import com.maharecruitment.gov.in.attendance.repository.LeaveApplicationRepository;
import com.maharecruitment.gov.in.attendance.repository.TourApplicationRepository;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.auth.entity.User;
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

    @Mock
    private UserRepository userRepository;

    @Mock
    private RecruitmentTypeApprovalRoutingService approvalRoutingService;

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
        when(leaveApplicationRepository.findPendingForApprover(
                7L, List.of(101L), "PENDING"))
                .thenReturn(List.of(leave));
        when(employeeRepository.findAllById(List.of(101L))).thenReturn(List.of(employee));

        List<LeaveApplicationHODDTO> result = service.getPendingLeavesForHOD(7L, null);

        assertEquals(1, result.size());
        assertEquals(101L, result.getFirst().getEmployeeId());
        assertEquals("Asha Patil", result.getFirst().getEmployeeName());
    }

    @Test
    void managerApprovalForwardsMahaitLeaveToHodWithoutFinalApproval() {
        LeaveApplicationEntity leave = new LeaveApplicationEntity();
        leave.setLeaveId(1L);
        leave.setEmployeeId(101L);
        leave.setStatus("PENDING");
        leave.setApprovalStage(RecruitmentTypeApprovalRoutingService.STAGE_MANAGER);
        leave.setManagerApproverUserId(7L);
        leave.setHodApproverUserId(9L);

        when(leaveApplicationRepository.findByLeaveIdForUpdate(1L)).thenReturn(java.util.Optional.of(leave));
        when(approvalRoutingService.isTwoLevelStage(RecruitmentTypeApprovalRoutingService.STAGE_MANAGER))
                .thenReturn(true);
        when(approvalRoutingService.canAct(
                7L, RecruitmentTypeApprovalRoutingService.STAGE_MANAGER, 7L, 9L)).thenReturn(true);

        service.updateLeaveStatus(1L, "APPROVED", "Recommended", 7L);

        assertEquals("PENDING", leave.getStatus());
        assertEquals(RecruitmentTypeApprovalRoutingService.STAGE_HOD, leave.getApprovalStage());
        assertEquals("Recommended", leave.getManagerRemarks());
        verify(leaveApplicationRepository).save(leave);
    }

    @Test
    void processedLeaveHistoryIsFilteredAndPaginatedInRepository() {
        LocalDate searchDate = LocalDate.of(2026, 8, 10);
        PageRequest pageable = PageRequest.of(1, 10);
        LeaveApplicationEntity leave = new LeaveApplicationEntity();
        leave.setLeaveId(22L);
        leave.setEmployeeId(101L);
        leave.setStatus("APPROVED");
        EmployeeEntity employee = new EmployeeEntity();
        employee.setEmployeeId(101L);
        employee.setFullName("Asha Patil");

        when(reportingManagerService.getEffectiveEmployeeIdsForAuthority(7L)).thenReturn(List.of(101L));
        when(leaveApplicationRepository.findProcessedForApproverPage(
                7L, List.of(101L), "%ASHA%", searchDate, pageable))
                .thenReturn(new PageImpl<>(List.of(leave), pageable, 21));
        when(employeeRepository.findAllById(List.of(101L))).thenReturn(List.of(employee));

        Page<LeaveApplicationHODDTO> result = service.getProcessedLeavesForHOD(
                7L, " asha ", searchDate, pageable);

        assertEquals(21, result.getTotalElements());
        assertEquals(3, result.getTotalPages());
        assertEquals("Asha Patil", result.getContent().getFirst().getEmployeeName());
    }

    @Test
    void leaveHistoryIncludesManagerAndHodNames() {
        LeaveApplicationEntity leave = new LeaveApplicationEntity();
        leave.setEmployeeId(101L);
        leave.setManagerApproverUserId(7L);
        leave.setHodApproverUserId(9L);
        User manager = new User();
        manager.setId(7L);
        manager.setName("Reporting Manager");
        User hod = new User();
        hod.setId(9L);
        hod.setName("Reporting HOD");

        when(leaveApplicationRepository.findByEmployeeIdOrderByApplicationDateDesc(101L))
                .thenReturn(List.of(leave));
        when(userRepository.findAllById(any())).thenReturn(List.of(manager, hod));

        List<LeaveApplicationEntity> result = service.getLeaveApplicationsByEmployee(101L);

        assertEquals("Reporting Manager", result.getFirst().getManagerApproverName());
        assertEquals("Reporting HOD", result.getFirst().getHodApproverName());
    }
}
