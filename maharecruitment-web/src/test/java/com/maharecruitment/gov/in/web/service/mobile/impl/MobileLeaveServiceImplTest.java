package com.maharecruitment.gov.in.web.service.mobile.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.maharecruitment.gov.in.attendance.entity.LeaveApplicationEntity;
import com.maharecruitment.gov.in.attendance.repository.LeaveApplicationRepository;
import com.maharecruitment.gov.in.attendance.service.LeaveApplicationService;
import com.maharecruitment.gov.in.auth.entity.Role;
import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.master.entity.LeaveEntity;
import com.maharecruitment.gov.in.master.repository.LeaveRepository;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.service.ReportingManagerService;
import com.maharecruitment.gov.in.web.dto.mobile.MobileLeaveApplyRequest;
import com.maharecruitment.gov.in.web.service.mobile.MobileApiException;
import com.maharecruitment.gov.in.web.service.mobile.MobileEmployeeAccessContext;
import com.maharecruitment.gov.in.web.service.mobile.MobileEmployeeAccessService;

@ExtendWith(MockitoExtension.class)
class MobileLeaveServiceImplTest {

    @Mock
    private MobileEmployeeAccessService mobileEmployeeAccessService;

    @Mock
    private LeaveApplicationService leaveApplicationService;

    @Mock
    private LeaveApplicationRepository leaveApplicationRepository;

    @Mock
    private LeaveRepository leaveRepository;

    @Mock
    private ReportingManagerService reportingManagerService;

    private MobileLeaveServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MobileLeaveServiceImpl(
                mobileEmployeeAccessService,
                leaveApplicationService,
                leaveApplicationRepository,
                leaveRepository,
                reportingManagerService);
    }

    @Test
    void optionsIncludeMasterTypesCategoriesAndCompOffFallback() {
        EmployeeEntity employee = employee(101L);
        LeaveEntity casualLeave = leaveType(1L, "CL", "Casual Leave");
        when(mobileEmployeeAccessService.requireCurrentActiveEmployee(101L)).thenReturn(employee);
        when(leaveRepository.findAll()).thenReturn(List.of(casualLeave));

        var response = service.getOptions(101L);

        assertThat(response.leaveTypes())
                .extracting(type -> type.code())
                .containsExactly("CL", "CO");
        assertThat(response.leaveCategories())
                .extracting(category -> category.code())
                .containsExactly("FULL_DAY", "FIRST_HALF", "SECOND_HALF");
    }

    @Test
    void applyUsesAuthenticatedEmployeeAndNormalizesPortalCategory() {
        EmployeeEntity employee = employee(101L);
        LeaveEntity casualLeave = leaveType(1L, "CL", "Casual Leave");
        when(mobileEmployeeAccessService.requireCurrentActiveEmployee(101L)).thenReturn(employee);
        when(leaveRepository.findAll()).thenReturn(List.of(casualLeave));
        doAnswer(invocation -> {
            LeaveApplicationEntity leave = invocation.getArgument(0);
            leave.setLeaveId(55L);
            leave.setStatus("PENDING");
            leave.setApplicationDate(LocalDateTime.of(2026, 8, 11, 10, 30));
            return null;
        }).when(leaveApplicationService).saveLeaveApplication(any(LeaveApplicationEntity.class));

        var response = service.apply(new MobileLeaveApplyRequest(
                101L,
                "cl",
                "FULL_DAY",
                LocalDate.of(2026, 8, 12),
                LocalDate.of(2026, 8, 13),
                null,
                " Personal work "));

        assertThat(response.leaveApplication().leaveId()).isEqualTo(55L);
        assertThat(response.leaveApplication().employeeId()).isEqualTo(101L);
        assertThat(response.leaveApplication().leaveType()).isEqualTo("CL");
        assertThat(response.leaveApplication().leaveCategory()).isEqualTo("Full Day");
        assertThat(response.leaveApplication().description()).isEqualTo("Personal work");
        assertThat(response.leaveApplication().cancellable()).isTrue();
    }

    @Test
    void cancellationRejectsAnotherEmployeesLeave() {
        when(mobileEmployeeAccessService.requireCurrentActiveEmployee(101L)).thenReturn(employee(101L));
        LeaveApplicationEntity leave = leaveApplication(77L, 202L, "PENDING");
        when(leaveApplicationRepository.findByLeaveIdForUpdate(77L)).thenReturn(Optional.of(leave));

        assertThatThrownBy(() -> service.cancel(101L, 77L))
                .isInstanceOfSatisfying(MobileApiException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(ex.getCode()).isEqualTo("LEAVE_CANCELLATION_FORBIDDEN");
                });
        verify(leaveApplicationRepository, never()).save(any());
    }

    @Test
    void hodCanApproveOnlyPendingLeaveInsideReportingAuthority() {
        MobileEmployeeAccessContext context = hodContext(500L, 7L);
        LeaveApplicationEntity leave = leaveApplication(88L, 101L, "PENDING");
        when(mobileEmployeeAccessService.requireCurrentActiveEmployeeContext(500L)).thenReturn(context);
        when(leaveApplicationRepository.findByLeaveIdForUpdate(88L)).thenReturn(Optional.of(leave));
        when(reportingManagerService.getEffectiveEmployeeIdsForAuthority(7L)).thenReturn(List.of(101L));

        var response = service.approve(500L, 88L, "Approved");

        assertThat(response.leaveApplication().status()).isEqualTo("APPROVED");
        assertThat(response.leaveApplication().hodRemarks()).isEqualTo("Approved");
        verify(leaveApplicationRepository).save(leave);
    }

    @Test
    void hodCannotApproveLeaveOutsideReportingAuthority() {
        MobileEmployeeAccessContext context = hodContext(500L, 7L);
        LeaveApplicationEntity leave = leaveApplication(88L, 202L, "PENDING");
        when(mobileEmployeeAccessService.requireCurrentActiveEmployeeContext(500L)).thenReturn(context);
        when(leaveApplicationRepository.findByLeaveIdForUpdate(88L)).thenReturn(Optional.of(leave));
        when(reportingManagerService.getEffectiveEmployeeIdsForAuthority(7L)).thenReturn(List.of(101L));

        assertThatThrownBy(() -> service.approve(500L, 88L, null))
                .isInstanceOfSatisfying(MobileApiException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(ex.getCode()).isEqualTo("LEAVE_APPROVAL_FORBIDDEN");
                });
        verify(leaveApplicationRepository, never()).save(any());
    }

    @Test
    void rejectionRequiresRemarks() {
        assertThatThrownBy(() -> service.reject(500L, 88L, " "))
                .isInstanceOfSatisfying(MobileApiException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ex.getCode()).isEqualTo("REJECTION_REMARKS_REQUIRED");
                });
        verify(mobileEmployeeAccessService, never()).requireCurrentActiveEmployeeContext(any());
    }

    private EmployeeEntity employee(Long employeeId) {
        EmployeeEntity employee = new EmployeeEntity();
        employee.setEmployeeId(employeeId);
        return employee;
    }

    private LeaveEntity leaveType(Long id, String code, String name) {
        LeaveEntity leave = new LeaveEntity();
        leave.setLeaveId(id);
        leave.setLeaveCode(code);
        leave.setLeaveName(name);
        return leave;
    }

    private LeaveApplicationEntity leaveApplication(Long leaveId, Long employeeId, String status) {
        LeaveApplicationEntity leave = new LeaveApplicationEntity();
        leave.setLeaveId(leaveId);
        leave.setEmployeeId(employeeId);
        leave.setLeaveType("CL");
        leave.setLeaveCategory("Full Day");
        leave.setStartDate(LocalDate.of(2026, 8, 12));
        leave.setEndDate(LocalDate.of(2026, 8, 12));
        leave.setStatus(status);
        return leave;
    }

    private MobileEmployeeAccessContext hodContext(Long employeeId, Long userId) {
        Role role = new Role();
        role.setName("ROLE_HOD");
        User user = new User();
        user.setId(userId);
        user.setRoles(List.of(role));
        return new MobileEmployeeAccessContext(user, employee(employeeId));
    }
}
