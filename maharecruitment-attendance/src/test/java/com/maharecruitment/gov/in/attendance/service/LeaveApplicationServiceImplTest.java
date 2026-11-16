package com.maharecruitment.gov.in.attendance.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
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

    @Mock private AdditionalChargeApprovalService additionalChargeApprovals;

    @Mock private InternalEmployeeApprovalPolicy internalApprovalPolicy;

    @Mock private LeaveTourAuditService leaveTourAudit;

    @BeforeEach void noAdditionalChargesByDefault() {
        org.mockito.Mockito.lenient().when(additionalChargeApprovals.snapshot()).thenReturn(AdditionalChargeApprovalService.Snapshot.empty());
    }

    @InjectMocks
    private LeaveApplicationServiceImpl service;

    @Test
    void chargeHolderCanProcessTheCoveredManagerStageWithoutAcquiringTheHodStage() {
        var leave = new LeaveApplicationEntity();
        leave.setLeaveId(1L);
        leave.setEmployeeId(101L);
        leave.setStatus("PENDING");
        leave.setApprovalStage("MANAGER");
        leave.setManagerApproverUserId(7L);
        leave.setHodApproverUserId(9L);
        var scope = org.mockito.Mockito.mock(AdditionalChargeApprovalService.Snapshot.class);
        when(additionalChargeApprovals.snapshot()).thenReturn(scope);
        when(scope.replaces(101L, 7L)).thenReturn(true);
        when(scope.replaces(101L, 9L)).thenReturn(false);
        when(scope.canAct(11L, 101L, "MANAGER", 7L, 9L)).thenReturn(true);
        when(scope.canAct(7L, 101L, "MANAGER", 7L, 9L)).thenReturn(false);
        when(approvalRoutingService.isTwoLevelStage("MANAGER")).thenReturn(true);
        when(approvalRoutingService.isTwoLevelStage("HOD")).thenReturn(true);
        when(leaveApplicationRepository.findByLeaveIdForUpdate(1L)).thenReturn(java.util.Optional.of(leave));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.updateLeaveStatus(1L, "APPROVED", "Not current authority", 7L));
        service.updateLeaveStatus(1L, "APPROVED", "Acting manager recommendation", 11L);
        assertEquals("PENDING", leave.getStatus());
        assertEquals("HOD", leave.getApprovalStage());
        assertEquals(7L, leave.getManagerApproverUserId());
        assertEquals(9L, leave.getHodApproverUserId());
        assertEquals(11L, leave.getManagerDecisionByUserId());
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.updateLeaveStatus(1L, "APPROVED", "Wrong stage", 11L));
        verify(leaveApplicationRepository).save(leave);
    }

    @Test
    void delegatedPendingLeavesAppearForHolderAndDisappearForReplacedApprover() {
        var leave = new LeaveApplicationEntity();
        leave.setLeaveId(1L);
        leave.setEmployeeId(101L);
        leave.setStatus("PENDING");
        leave.setApprovalStage("MANAGER");
        leave.setManagerApproverUserId(7L);
        leave.setHodApproverUserId(9L);
        var scope = org.mockito.Mockito.mock(AdditionalChargeApprovalService.Snapshot.class);
        when(additionalChargeApprovals.snapshot()).thenReturn(scope);
        when(scope.affects(7L)).thenReturn(true);
        when(scope.affects(11L)).thenReturn(true);
        when(scope.incomingEmployeeIds(11L)).thenReturn(List.of(101L));
        when(scope.incomingEmployeeIds(7L)).thenReturn(List.of());
        when(scope.canAct(11L, 101L, "MANAGER", 7L, 9L)).thenReturn(true);
        when(scope.canAct(7L, 101L, "MANAGER", 7L, 9L)).thenReturn(false);
        when(approvalRoutingService.isTwoLevelStage("MANAGER")).thenReturn(true);
        when(reportingManagerService.getTeamEmployeeIdsForAuthority(7L)).thenReturn(List.of(101L));
        when(reportingManagerService.getTeamEmployeeIdsForAuthority(11L)).thenReturn(List.of());
        when(leaveApplicationRepository.findPendingForApprover(7L, List.of(101L), "PENDING")).thenReturn(List.of(leave));
        when(leaveApplicationRepository.findPendingForApprover(11L, List.of(-1L), "PENDING")).thenReturn(List.of());
        when(leaveApplicationRepository.findPendingAdditionalChargeCandidates(List.of(101L), null, null)).thenReturn(List.of(leave));
        var employee = new EmployeeEntity();
        employee.setEmployeeId(101L);
        employee.setFullName("Team member");
        when(employeeRepository.findAllById(List.of(101L))).thenReturn(List.of(employee));
        assertEquals(0, service.getPendingLeavesForHOD(7L, null).size());
        assertEquals(1, service.getPendingLeavesForHOD(11L, null).size());
    }

    @Test
    void legacyLeaveApprovalIsTransferredAndAuditsTheActualActor() {
        var leave = new LeaveApplicationEntity();
        leave.setLeaveId(1L);
        leave.setEmployeeId(101L);
        leave.setStatus("PENDING");
        var scope = org.mockito.Mockito.mock(AdditionalChargeApprovalService.Snapshot.class);
        when(additionalChargeApprovals.snapshot()).thenReturn(scope);
        when(scope.canActLegacy(11L, 101L, List.of())).thenReturn(true);
        when(scope.canActLegacy(7L, 101L, List.of(101L))).thenReturn(false);
        when(reportingManagerService.getTeamEmployeeIdsForAuthority(7L)).thenReturn(List.of(101L));
        when(reportingManagerService.getTeamEmployeeIdsForAuthority(11L)).thenReturn(List.of());
        when(leaveApplicationRepository.findByLeaveIdForUpdate(1L)).thenReturn(java.util.Optional.of(leave));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.updateLeaveStatus(1L, "APPROVED", "Not current authority", 7L));
        service.updateLeaveStatus(1L, "APPROVED", "Acting authority", 11L);
        assertEquals("APPROVED", leave.getStatus());
        assertEquals(11L, leave.getHodDecisionByUserId());
    }

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

        when(reportingManagerService.getTeamEmployeeIdsForAuthority(7L))
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
        verify(leaveTourAudit).leaveDecided(
                leave, "APPROVED", RecruitmentTypeApprovalRoutingService.STAGE_MANAGER, "Recommended", 7L);
    }

    @Test
    void internalEmployeeLeaveIsNotDecidedWhenApproverRoleCheckFails() {
        LeaveApplicationEntity leave = new LeaveApplicationEntity();
        leave.setLeaveId(1L);
        leave.setEmployeeId(101L);
        leave.setStatus("PENDING");

        when(leaveApplicationRepository.findByLeaveIdForUpdate(1L)).thenReturn(java.util.Optional.of(leave));
        org.mockito.Mockito.doThrow(new IllegalArgumentException("only senior roles"))
                .when(internalApprovalPolicy).requireCanDecide(7L, 101L, "leave");

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.updateLeaveStatus(1L, "APPROVED", "ok", 7L));

        assertEquals("PENDING", leave.getStatus());
        verify(leaveApplicationRepository, org.mockito.Mockito.never()).save(any());
        org.mockito.Mockito.verifyNoInteractions(leaveTourAudit);
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

        when(reportingManagerService.getTeamEmployeeIdsForAuthority(7L)).thenReturn(List.of(101L));
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

    @Test
    void leaveHistoryShowsNameAndDesignationOfWhoApproved() {
        LeaveApplicationEntity leave = new LeaveApplicationEntity();
        leave.setEmployeeId(101L);
        leave.setStatus("APPROVED");
        leave.setHodDecisionByUserId(9L);
        User hod = new User();
        hod.setId(9L);
        hod.setName("Ravi Deshmukh");
        var designation = new com.maharecruitment.gov.in.master.entity.ManpowerDesignationMaster();
        designation.setDesignationName("Head of Department");
        EmployeeEntity hodEmployee = new EmployeeEntity();
        hodEmployee.setUser(hod);
        hodEmployee.setDesignation(designation);

        when(leaveApplicationRepository.findByEmployeeIdOrderByApplicationDateDesc(101L))
                .thenReturn(List.of(leave));
        when(userRepository.findAllById(any())).thenReturn(List.of(hod));
        when(employeeRepository.findByUser_IdIn(any())).thenReturn(List.of(hodEmployee));

        LeaveApplicationEntity result = service.getLeaveApplicationsByEmployee(101L).getFirst();

        assertEquals("Ravi Deshmukh", result.getDecidedByName());
        assertEquals("Head of Department", result.getDecidedByDesignation());
    }

    @Test
    void leaveRejectedByManagerShowsManagerAndFallsBackToRoleWithoutEmployeeRecord() {
        LeaveApplicationEntity leave = new LeaveApplicationEntity();
        leave.setEmployeeId(101L);
        leave.setStatus("REJECTED");
        leave.setManagerDecisionByUserId(7L);
        var role = new com.maharecruitment.gov.in.auth.entity.Role();
        role.setName("ROLE_COO");
        User coo = new User();
        coo.setId(7L);
        coo.setName("Chief Officer");
        coo.setRoles(List.of(role));

        when(leaveApplicationRepository.findByEmployeeIdOrderByApplicationDateDesc(101L))
                .thenReturn(List.of(leave));
        when(userRepository.findAllById(any())).thenReturn(List.of(coo));

        LeaveApplicationEntity result = service.getLeaveApplicationsByEmployee(101L).getFirst();

        assertEquals("Chief Officer", result.getDecidedByName());
        assertEquals("COO", result.getDecidedByDesignation());
    }

    @Test
    void twoLevelLeaveApprovedBeforeDeciderWasRecordedFallsBackToStoredHod() {
        LeaveApplicationEntity leave = new LeaveApplicationEntity();
        leave.setEmployeeId(101L);
        leave.setStatus("APPROVED");
        leave.setApprovalStage("HOD");
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

        LeaveApplicationEntity result = service.getLeaveApplicationsByEmployee(101L).getFirst();

        assertEquals("Reporting HOD", result.getDecidedByName());
    }

    @Test
    void twoLevelLeaveRejectedAtManagerStageBeforeDeciderWasRecordedShowsManager() {
        LeaveApplicationEntity leave = new LeaveApplicationEntity();
        leave.setEmployeeId(101L);
        leave.setStatus("REJECTED");
        leave.setApprovalStage("MANAGER");
        leave.setManagerApproverUserId(7L);
        leave.setHodApproverUserId(9L);
        User manager = new User();
        manager.setId(7L);
        manager.setName("Reporting Manager");

        when(leaveApplicationRepository.findByEmployeeIdOrderByApplicationDateDesc(101L))
                .thenReturn(List.of(leave));
        when(userRepository.findAllById(any())).thenReturn(List.of(manager));

        LeaveApplicationEntity result = service.getLeaveApplicationsByEmployee(101L).getFirst();

        assertEquals("Reporting Manager", result.getDecidedByName());
    }

    @Test
    void legacyLeaveApprovedBeforeDeciderWasRecordedHasNoDecider() {
        LeaveApplicationEntity leave = new LeaveApplicationEntity();
        leave.setEmployeeId(101L);
        leave.setStatus("APPROVED");

        when(leaveApplicationRepository.findByEmployeeIdOrderByApplicationDateDesc(101L))
                .thenReturn(List.of(leave));

        LeaveApplicationEntity result = service.getLeaveApplicationsByEmployee(101L).getFirst();

        assertEquals(null, result.getDecidedByName());
    }

    @Test
    void indirectReportsLegacyLeaveIsVisibleAndDecidableByTheirSeniorManager() {
        // 101 reports to 55, who reports to user 7: the team lookup covers every level below user 7.
        LeaveApplicationEntity leave = new LeaveApplicationEntity();
        leave.setLeaveId(5L);
        leave.setEmployeeId(101L);
        leave.setStatus("PENDING");
        EmployeeEntity employee = new EmployeeEntity();
        employee.setEmployeeId(101L);
        employee.setFullName("Indirect Report");
        when(reportingManagerService.getTeamEmployeeIdsForAuthority(7L)).thenReturn(List.of(55L, 101L));
        when(leaveApplicationRepository.findPendingForApprover(7L, List.of(55L, 101L), "PENDING"))
                .thenReturn(List.of(leave));
        when(employeeRepository.findAllById(List.of(101L))).thenReturn(List.of(employee));
        when(internalApprovalPolicy.hasInternalApproverRole(7L)).thenReturn(true);
        when(leaveApplicationRepository.findByLeaveIdForUpdate(5L)).thenReturn(java.util.Optional.of(leave));

        LeaveApplicationHODDTO dto = service.getPendingLeavesForHOD(7L, null).getFirst();
        service.updateLeaveStatus(5L, "APPROVED", "ok", 7L);

        assertEquals(true, dto.isDecisionAllowed());
        assertEquals(null, dto.getDecisionNote());
        assertEquals("APPROVED", leave.getStatus());
        assertEquals(7L, leave.getHodDecisionByUserId());
    }

    @Test
    void internalLeaveIsViewOnlyWithReasonForTeamManagerWithoutApproverRole() {
        LeaveApplicationEntity leave = new LeaveApplicationEntity();
        leave.setLeaveId(6L);
        leave.setEmployeeId(101L);
        leave.setStatus("PENDING");
        EmployeeEntity employee = new EmployeeEntity();
        employee.setEmployeeId(101L);
        employee.setRecruitmentType("INTERNAL");
        when(reportingManagerService.getTeamEmployeeIdsForAuthority(7L)).thenReturn(List.of(101L));
        when(leaveApplicationRepository.findPendingForApprover(7L, List.of(101L), "PENDING"))
                .thenReturn(List.of(leave));
        when(employeeRepository.findAllById(List.of(101L))).thenReturn(List.of(employee));
        when(internalApprovalPolicy.isInternal(employee)).thenReturn(true);
        when(internalApprovalPolicy.viewOnlyReason()).thenReturn("Only HOD or PM can approve internal employee requests.");

        LeaveApplicationHODDTO dto = service.getPendingLeavesForHOD(7L, null).getFirst();

        assertEquals(false, dto.isDecisionAllowed());
        assertEquals("Only HOD or PM can approve internal employee requests.", dto.getDecisionNote());
    }

    @Test
    void stagedLeaveOfATeamMemberIsVisibleButStaysWithTheStoredApprover() {
        LeaveApplicationEntity leave = new LeaveApplicationEntity();
        leave.setLeaveId(7L);
        leave.setEmployeeId(101L);
        leave.setStatus("PENDING");
        leave.setApprovalStage("HOD");
        leave.setManagerApproverUserId(8L);
        leave.setHodApproverUserId(9L);
        when(reportingManagerService.getTeamEmployeeIdsForAuthority(7L)).thenReturn(List.of(101L));
        when(leaveApplicationRepository.findPendingForApprover(7L, List.of(101L), "PENDING"))
                .thenReturn(List.of(leave));
        EmployeeEntity employee = new EmployeeEntity();
        employee.setEmployeeId(101L);
        when(employeeRepository.findAllById(List.of(101L))).thenReturn(List.of(employee));
        when(approvalRoutingService.isTwoLevelStage("HOD")).thenReturn(true);
        when(approvalRoutingService.canAct(7L, "HOD", 8L, 9L)).thenReturn(false);

        LeaveApplicationHODDTO dto = service.getPendingLeavesForHOD(7L, null).getFirst();

        assertEquals(false, dto.isDecisionAllowed());
        org.junit.jupiter.api.Assertions.assertTrue(dto.getDecisionNote().contains("reporting HOD"));
    }

    @Test
    void pendingLeaveHasNoDecider() {
        LeaveApplicationEntity leave = new LeaveApplicationEntity();
        leave.setEmployeeId(101L);
        leave.setStatus("PENDING");
        leave.setManagerDecisionByUserId(7L);

        when(leaveApplicationRepository.findByEmployeeIdOrderByApplicationDateDesc(101L))
                .thenReturn(List.of(leave));

        LeaveApplicationEntity result = service.getLeaveApplicationsByEmployee(101L).getFirst();

        assertEquals(null, result.getDecidedByName());
    }
}
