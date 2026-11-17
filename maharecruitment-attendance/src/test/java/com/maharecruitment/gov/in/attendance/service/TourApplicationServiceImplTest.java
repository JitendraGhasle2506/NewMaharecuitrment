package com.maharecruitment.gov.in.attendance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.maharecruitment.gov.in.attendance.dto.TourApplicationHODDTO;
import com.maharecruitment.gov.in.attendance.entity.TourApplicationEntity;
import com.maharecruitment.gov.in.attendance.repository.TourApplicationRepository;
import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.master.entity.ManpowerDesignationMaster;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.recruitment.service.ReportingManagerService;

@ExtendWith(MockitoExtension.class)
class TourApplicationServiceImplTest {

    @Mock
    private TourApplicationRepository tourApplicationRepository;

    @Mock
    private ReportingManagerService reportingManagerService;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private RecruitmentTypeApprovalRoutingService approvalRoutingService;

    @Mock
    private UserRepository userRepository;

    @Mock private AdditionalChargeApprovalService additionalChargeApprovals;

    @Mock private LeaveTourAuditService leaveTourAudit;

    @Mock private com.maharecruitment.gov.in.attendance.repository.InternalApproverRoleRepository approverRoleRepository;

    private TourApplicationServiceImpl service;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(additionalChargeApprovals.snapshot()).thenReturn(AdditionalChargeApprovalService.Snapshot.empty());
        // The roles seeded by V160, as maintained on Admin > Internal Approver Roles.
        org.mockito.Mockito.lenient().when(approverRoleRepository.findApproverRoleNames()).thenReturn(List.of(
                "ROLE_CFO", "ROLE_COO", "ROLE_CTO", "ROLE_HOD", "ROLE_MD", "ROLE_PM", "ROLE_STM"));
        service = new TourApplicationServiceImpl(
                tourApplicationRepository,
                reportingManagerService,
                employeeRepository,
                approvalRoutingService,
                userRepository, additionalChargeApprovals,
                new InternalEmployeeApprovalPolicy(employeeRepository, userRepository, approverRoleRepository),
                leaveTourAudit);
    }

    @Test
    void internalEmployeeTourCannotBeDecidedWithoutSeniorApproverRole() {
        var tour = tour(601L, 101L, "PENDING");
        when(tourApplicationRepository.findByTourIdForUpdate(601L)).thenReturn(java.util.Optional.of(tour));
        when(employeeRepository.findById(101L)).thenReturn(java.util.Optional.of(internalEmployee(101L)));
        when(userRepository.findById(7L)).thenReturn(java.util.Optional.of(userWithRole(7L, "ROLE_EMPLOYEE")));

        var error = org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.updateTourStatus(601L, "APPROVED", "ok", 7L));

        assertThat(error.getMessage()).contains("cannot approve or reject",
                "Only CFO, COO, CTO, HOD, MD, PM or STM can approve internal employee requests.");
        assertThat(tour.getStatus()).isEqualTo("PENDING");
        org.mockito.Mockito.verify(tourApplicationRepository, org.mockito.Mockito.never()).save(any());
        org.mockito.Mockito.verifyNoInteractions(leaveTourAudit);
    }

    @Test
    void internalEmployeeTourCanBeDecidedByHodInReportingAuthority() {
        var tour = tour(602L, 101L, "PENDING");
        when(tourApplicationRepository.findByTourIdForUpdate(602L)).thenReturn(java.util.Optional.of(tour));
        when(employeeRepository.findById(101L)).thenReturn(java.util.Optional.of(internalEmployee(101L)));
        when(userRepository.findById(7L)).thenReturn(java.util.Optional.of(userWithRole(7L, "ROLE_HOD")));
        when(reportingManagerService.getTeamEmployeeIdsForAuthority(7L)).thenReturn(List.of(101L));

        service.updateTourStatus(602L, "APPROVED", "ok", 7L);

        assertThat(tour.getStatus()).isEqualTo("APPROVED");
        assertThat(tour.getHodDecisionByUserId()).isEqualTo(7L);
        verify(leaveTourAudit).tourDecided(tour, "APPROVED", null, "ok", 7L);
    }

    @Test
    void pendingInternalTourIsViewOnlyForApproverWithoutSeniorRole() {
        List<Long> employeeIds = List.of(101L);
        EmployeeEntity employee = employee(101L, "EMP101", "Asha Patil", "Developer");
        employee.setRecruitmentType("INTERNAL");
        when(reportingManagerService.getTeamEmployeeIdsForAuthority(7L)).thenReturn(employeeIds);
        when(tourApplicationRepository.findPendingForApprover(7L, employeeIds, "PENDING"))
                .thenReturn(List.of(tour(603L, 101L, "PENDING")));
        when(employeeRepository.findByEmployeeIdInOrderByFullNameAscEmployeeIdAsc(employeeIds))
                .thenReturn(List.of(employee));
        when(userRepository.findById(7L)).thenReturn(java.util.Optional.of(userWithRole(7L, "ROLE_EMPLOYEE")));

        assertThat(service.getPendingToursForHOD(7L, null)).singleElement().satisfies(dto -> {
            assertThat(dto.isDecisionAllowed()).isFalse();
            assertThat(dto.getDecisionNote())
                    .isEqualTo("Only CFO, COO, CTO, HOD, MD, PM or STM can approve internal employee requests.");
        });
    }

    @Test
    void projectManagerCanDecideInternalTourOfAnIndirectReport() {
        // 101 reports to 55, who reports to the PM (user 7): the team lookup covers every level.
        var tour = tour(604L, 101L, "PENDING");
        EmployeeEntity employee = internalEmployee(101L);
        when(reportingManagerService.getTeamEmployeeIdsForAuthority(7L)).thenReturn(List.of(55L, 101L));
        when(tourApplicationRepository.findPendingForApprover(7L, List.of(55L, 101L), "PENDING"))
                .thenReturn(List.of(tour));
        when(employeeRepository.findByEmployeeIdInOrderByFullNameAscEmployeeIdAsc(List.of(101L)))
                .thenReturn(List.of(employee));
        when(userRepository.findById(7L)).thenReturn(java.util.Optional.of(userWithRole(7L, "ROLE_PM")));
        when(tourApplicationRepository.findByTourIdForUpdate(604L)).thenReturn(java.util.Optional.of(tour));
        when(employeeRepository.findById(101L)).thenReturn(java.util.Optional.of(employee));

        assertThat(service.getPendingToursForHOD(7L, null)).singleElement().satisfies(dto -> {
            assertThat(dto.isDecisionAllowed()).isTrue();
            assertThat(dto.getDecisionNote()).isNull();
        });
        service.updateTourStatus(604L, "APPROVED", "ok", 7L);

        assertThat(tour.getStatus()).isEqualTo("APPROVED");
        assertThat(tour.getHodDecisionByUserId()).isEqualTo(7L);
    }

    @Test
    void stagedTourOfATeamMemberIsVisibleButStaysWithTheStoredApprover() {
        var tour = tour(605L, 101L, "PENDING");
        tour.setApprovalStage("MANAGER");
        tour.setManagerApproverUserId(8L);
        tour.setHodApproverUserId(9L);
        when(reportingManagerService.getTeamEmployeeIdsForAuthority(7L)).thenReturn(List.of(101L));
        when(tourApplicationRepository.findPendingForApprover(7L, List.of(101L), "PENDING"))
                .thenReturn(List.of(tour));
        when(employeeRepository.findByEmployeeIdInOrderByFullNameAscEmployeeIdAsc(List.of(101L)))
                .thenReturn(List.of(employee(101L, "EMP101", "Asha Patil", "Developer")));
        when(approvalRoutingService.isTwoLevelStage("MANAGER")).thenReturn(true);
        when(approvalRoutingService.canAct(7L, "MANAGER", 8L, 9L)).thenReturn(false);
        when(tourApplicationRepository.findByTourIdForUpdate(605L)).thenReturn(java.util.Optional.of(tour));

        assertThat(service.getPendingToursForHOD(7L, null)).singleElement().satisfies(dto -> {
            assertThat(dto.isDecisionAllowed()).isFalse();
            assertThat(dto.getDecisionNote()).contains("reporting manager");
        });
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.updateTourStatus(605L, "APPROVED", "not my stage", 7L));
        assertThat(tour.getStatus()).isEqualTo("PENDING");
    }

    @Test
    void delegatedHodCanApproveTourAndOriginalOwnerCannotActDuringCoverage() {
        var tour = tour(501L, 101L, "PENDING");
        tour.setApprovalStage("HOD");
        tour.setManagerApproverUserId(7L);
        tour.setHodApproverUserId(9L);
        var scope = org.mockito.Mockito.mock(AdditionalChargeApprovalService.Snapshot.class);
        when(additionalChargeApprovals.snapshot()).thenReturn(scope);
        when(scope.replaces(101L, 9L)).thenReturn(true);
        when(scope.canAct(11L, 101L, "HOD", 7L, 9L)).thenReturn(true);
        when(scope.canAct(9L, 101L, "HOD", 7L, 9L)).thenReturn(false);
        when(approvalRoutingService.isTwoLevelStage("HOD")).thenReturn(true);
        when(tourApplicationRepository.findByTourIdForUpdate(501L)).thenReturn(java.util.Optional.of(tour));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.updateTourStatus(501L, "APPROVED", "Not current approver", 9L));
        service.updateTourStatus(501L, "APPROVED", "Acting HOD", 11L);
        assertThat(tour.getStatus()).isEqualTo("APPROVED");
        assertThat(tour.getHodApproverUserId()).isEqualTo(9L);
        assertThat(tour.getHodDecisionByUserId()).isEqualTo(11L);
        verify(tourApplicationRepository).save(tour);
    }

    @Test
    void expiredCoverageUsesTheOriginalStoredApproverWithoutRewritingTheRequest() {
        var tour = tour(501L, 101L, "PENDING");
        tour.setApprovalStage("HOD");
        tour.setManagerApproverUserId(7L);
        tour.setHodApproverUserId(9L);
        when(approvalRoutingService.isTwoLevelStage("HOD")).thenReturn(true);
        when(approvalRoutingService.canAct(9L, "HOD", 7L, 9L)).thenReturn(true);
        when(approvalRoutingService.canAct(11L, "HOD", 7L, 9L)).thenReturn(false);
        when(tourApplicationRepository.findByTourIdForUpdate(501L)).thenReturn(java.util.Optional.of(tour));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.updateTourStatus(501L, "APPROVED", "Charge expired", 11L));
        service.updateTourStatus(501L, "APPROVED", "Original approver", 9L);
        assertThat(tour.getHodApproverUserId()).isEqualTo(9L);
        assertThat(tour.getHodDecisionByUserId()).isEqualTo(9L);
    }

    @Test
    void pendingTourCoverageUsesTheSameSearchAndDateFilters() {
        var tour = tour(501L, 101L, "PENDING");
        tour.setApprovalStage("HOD");
        tour.setHodApproverUserId(9L);
        var scope = org.mockito.Mockito.mock(AdditionalChargeApprovalService.Snapshot.class);
        when(additionalChargeApprovals.snapshot()).thenReturn(scope);
        when(scope.affects(11L)).thenReturn(true);
        when(scope.incomingEmployeeIds(11L)).thenReturn(List.of(101L));
        when(scope.canAct(11L, 101L, "HOD", null, 9L)).thenReturn(true);
        when(approvalRoutingService.isTwoLevelStage("HOD")).thenReturn(true);
        var date = LocalDate.of(2026, 10, 15);
        when(tourApplicationRepository.findPendingAdditionalChargeCandidates(List.of(101L), "%ASHA%", date)).thenReturn(List.of(tour));
        when(employeeRepository.findByEmployeeIdInOrderByFullNameAscEmployeeIdAsc(List.of(101L)))
                .thenReturn(List.of(employee(101L, "EMP101", "Asha Patil", "Developer")));
        assertThat(service.getPendingToursForHOD(11L, "Asha", date)).hasSize(1);
    }

    @Test
    void pendingToursUseEffectiveReportingAuthorityEmployees() {
        List<Long> employeeIds = List.of(101L);
        TourApplicationEntity tour = tour(501L, 101L, "PENDING");
        EmployeeEntity employee = employee(101L, "EMP101", "Asha Patil", "Developer");

        when(reportingManagerService.getTeamEmployeeIdsForAuthority(7L)).thenReturn(employeeIds);
        when(tourApplicationRepository.findPendingForApprover(
                7L,
                employeeIds,
                "PENDING")).thenReturn(List.of(tour));
        when(employeeRepository.findByEmployeeIdInOrderByFullNameAscEmployeeIdAsc(employeeIds))
                .thenReturn(List.of(employee));

        List<TourApplicationHODDTO> result = service.getPendingToursForHOD(7L, null);

        assertThat(result).singleElement().satisfies(dto -> {
            assertThat(dto.getTourId()).isEqualTo(501L);
            assertThat(dto.getEmployeeId()).isEqualTo(101L);
            assertThat(dto.getEmployeeName()).isEqualTo("Asha Patil");
            assertThat(dto.getDesignation()).isEqualTo("Developer");
        });
        verify(reportingManagerService).getTeamEmployeeIdsForAuthority(7L);
    }

    @Test
    void processedToursUseEffectiveReportingAuthorityEmployees() {
        List<Long> employeeIds = List.of(101L);
        List<String> statuses = List.of("APPROVED", "REJECTED");
        TourApplicationEntity tour = tour(502L, 101L, "APPROVED");
        EmployeeEntity employee = employee(101L, "EMP101", "Asha Patil", "Developer");

        when(reportingManagerService.getTeamEmployeeIdsForAuthority(7L)).thenReturn(employeeIds);
        when(tourApplicationRepository.findProcessedForApprover(
                7L,
                employeeIds,
                statuses)).thenReturn(List.of(tour));
        when(employeeRepository.findByEmployeeIdInOrderByFullNameAscEmployeeIdAsc(employeeIds))
                .thenReturn(List.of(employee));

        List<TourApplicationHODDTO> result = service.getProcessedToursForHOD(7L, null);

        assertThat(result).singleElement().satisfies(dto -> {
            assertThat(dto.getTourId()).isEqualTo(502L);
            assertThat(dto.getStatus()).isEqualTo("APPROVED");
        });
        verify(reportingManagerService).getTeamEmployeeIdsForAuthority(7L);
    }

    @Test
    void processedTourHistoryIsFilteredAndPaginatedInRepository() {
        List<Long> employeeIds = List.of(101L);
        LocalDate searchDate = LocalDate.of(2026, 9, 5);
        PageRequest pageable = PageRequest.of(0, 10);
        TourApplicationEntity tour = tour(504L, 101L, "APPROVED");
        EmployeeEntity employee = employee(101L, "EMP101", "Asha Patil", "Developer");

        when(reportingManagerService.getTeamEmployeeIdsForAuthority(7L)).thenReturn(employeeIds);
        when(tourApplicationRepository.findProcessedForApproverPage(
                7L, employeeIds, "%EMP101%", searchDate, pageable))
                .thenReturn(new PageImpl<>(List.of(tour), pageable, 12));
        when(employeeRepository.findByEmployeeIdInOrderByFullNameAscEmployeeIdAsc(employeeIds))
                .thenReturn(List.of(employee));

        Page<TourApplicationHODDTO> result = service.getProcessedToursForHOD(
                7L, "emp101", searchDate, pageable);

        assertThat(result.getTotalElements()).isEqualTo(12);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.getContent()).singleElement()
                .extracting(TourApplicationHODDTO::getEmployeeName)
                .isEqualTo("Asha Patil");
    }

    @Test
    void tourHistoryIncludesManagerAndHodNames() {
        TourApplicationEntity tour = tour(503L, 101L, "PENDING");
        tour.setManagerApproverUserId(7L);
        tour.setHodApproverUserId(9L);
        User manager = new User();
        manager.setId(7L);
        manager.setName("Reporting Manager");
        User hod = new User();
        hod.setId(9L);
        hod.setName("Reporting HOD");

        when(tourApplicationRepository.findByEmployeeIdOrderByApplicationDateDesc(101L))
                .thenReturn(List.of(tour));
        when(userRepository.findAllById(any())).thenReturn(List.of(manager, hod));

        List<TourApplicationEntity> result = service.getTourApplicationsByEmployee(101L);

        assertThat(result.getFirst().getManagerApproverName()).isEqualTo("Reporting Manager");
        assertThat(result.getFirst().getHodApproverName()).isEqualTo("Reporting HOD");
    }

    private TourApplicationEntity tour(Long tourId, Long employeeId, String status) {
        TourApplicationEntity tour = new TourApplicationEntity();
        tour.setTourId(tourId);
        tour.setEmployeeId(employeeId);
        tour.setTourCategory("Full Day");
        tour.setStartDate(LocalDate.of(2026, 9, 5));
        tour.setEndDate(LocalDate.of(2026, 9, 6));
        tour.setApplicationDate(LocalDateTime.of(2026, 9, 2, 10, 30));
        tour.setStatus(status);
        return tour;
    }

    private EmployeeEntity employee(Long employeeId, String code, String name, String designationName) {
        ManpowerDesignationMaster designation = new ManpowerDesignationMaster();
        designation.setDesignationName(designationName);

        EmployeeEntity employee = new EmployeeEntity();
        employee.setEmployeeId(employeeId);
        employee.setEmployeeCode(code);
        employee.setFullName(name);
        employee.setDesignation(designation);
        return employee;
    }

    private EmployeeEntity internalEmployee(Long employeeId) {
        EmployeeEntity employee = employee(employeeId, "EMP" + employeeId, "Internal Employee", "Developer");
        employee.setRecruitmentType("INTERNAL");
        return employee;
    }

    private User userWithRole(Long userId, String roleName) {
        com.maharecruitment.gov.in.auth.entity.Role role = new com.maharecruitment.gov.in.auth.entity.Role();
        role.setName(roleName);
        User user = new User();
        user.setId(userId);
        user.setRoles(List.of(role));
        return user;
    }
}
