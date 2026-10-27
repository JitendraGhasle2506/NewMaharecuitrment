package com.maharecruitment.gov.in.attendance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.maharecruitment.gov.in.attendance.entity.AttendanceRequestAuditLogEntity;
import com.maharecruitment.gov.in.attendance.entity.LeaveApplicationEntity;
import com.maharecruitment.gov.in.attendance.entity.TourApplicationEntity;
import com.maharecruitment.gov.in.attendance.repository.AttendanceRequestAuditLogRepository;
import com.maharecruitment.gov.in.auth.entity.Role;
import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.common.audit.AuditIpEntityListener;
import com.maharecruitment.gov.in.common.audit.AuditRequestContext;
import com.maharecruitment.gov.in.master.entity.ManpowerDesignationMaster;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;

@ExtendWith(MockitoExtension.class)
class LeaveTourAuditServiceTest {

    @Mock
    private AttendanceRequestAuditLogRepository auditLogRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    private LeaveTourAuditService service;

    @BeforeEach
    void setUp() {
        service = new LeaveTourAuditService(auditLogRepository, userRepository, employeeRepository);
    }

    @AfterEach
    void clearRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void webLeaveApplicationIsLoggedAgainstTheApplyingEmployee() {
        bindRequest("/employee/submitLeave", "Mozilla/5.0 Test");
        User employeeUser = user(21L, "asha@mahait.org", "Asha Patil", "ROLE_EMPLOYEE");
        EmployeeEntity employee = employee(employeeUser, "Developer");
        when(employeeRepository.findById(101L)).thenReturn(Optional.of(employee));
        when(userRepository.findById(21L)).thenReturn(Optional.of(employeeUser));
        when(employeeRepository.findByUser_Id(21L)).thenReturn(Optional.of(employee));

        service.leaveApplied(leave("PENDING", "MANAGER"));

        AttendanceRequestAuditLogEntity event = captureEvent();
        assertThat(event.getRequestType()).isEqualTo("LEAVE");
        assertThat(event.getRequestId()).isEqualTo(12L);
        assertThat(event.getEmployeeId()).isEqualTo(101L);
        assertThat(event.getActionType()).isEqualTo("APPLY");
        assertThat(event.getPreviousStatus()).isNull();
        assertThat(event.getNewStatus()).isEqualTo("PENDING");
        assertThat(event.getNewStage()).isEqualTo("MANAGER");
        assertThat(event.getRequestCategory()).isEqualTo("CL / FULL_DAY");
        assertThat(event.getStartDate()).isEqualTo(LocalDate.of(2026, 10, 10));
        assertThat(event.getActorUserId()).isEqualTo(21L);
        assertThat(event.getActorName()).isEqualTo("Asha Patil");
        assertThat(event.getActorLoginId()).isEqualTo("asha@mahait.org");
        assertThat(event.getActorDesignation()).isEqualTo("Developer");
        assertThat(event.getActorRoles()).isEqualTo("ROLE_EMPLOYEE");
        assertThat(event.getChannel()).isEqualTo("WEB");
        assertThat(event.getUserAgent()).isEqualTo("Mozilla/5.0 Test");
        assertThat(event.getDetails()).contains("10-10-2026 to 12-10-2026", "- -> PENDING");
    }

    @Test
    void mobileManagerApprovalOfTwoLevelLeaveIsLoggedAsForward() {
        bindRequest("/api/mobile/leaves/approvals/12/approve", "okhttp/4.12");
        when(userRepository.findById(7L)).thenReturn(Optional.of(user(7L, "manager@mahait.org", "Manager", "ROLE_PM")));

        service.leaveDecided(leave("PENDING", "HOD"), "APPROVED", "MANAGER", "Recommended", 7L);

        AttendanceRequestAuditLogEntity event = captureEvent();
        assertThat(event.getActionType()).isEqualTo("FORWARD");
        assertThat(event.getPreviousStatus()).isEqualTo("PENDING");
        assertThat(event.getNewStatus()).isEqualTo("PENDING");
        assertThat(event.getPreviousStage()).isEqualTo("MANAGER");
        assertThat(event.getNewStage()).isEqualTo("HOD");
        assertThat(event.getRemarks()).isEqualTo("Recommended");
        assertThat(event.getChannel()).isEqualTo("MOBILE");
        assertThat(event.getDetails()).contains("forwarded to HOD");
    }

    @Test
    void finalLeaveDecisionsAreLoggedAsApproveOrRejectWithApproverRoles() {
        User hod = user(9L, "hod@mahait.org", "HOD", "ROLE_HOD", "ROLE_EMPLOYEE");
        when(userRepository.findById(9L)).thenReturn(Optional.of(hod));

        service.leaveDecided(leave("APPROVED", null), "APPROVED", null, null, 9L);
        service.leaveDecided(leave("REJECTED", null), "REJECTED", null, "Not eligible", 9L);

        ArgumentCaptor<AttendanceRequestAuditLogEntity> captor =
                ArgumentCaptor.forClass(AttendanceRequestAuditLogEntity.class);
        verify(auditLogRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(AttendanceRequestAuditLogEntity::getActionType)
                .containsExactly("APPROVE", "REJECT");
        assertThat(captor.getAllValues().get(1).getRemarks()).isEqualTo("Not eligible");
        assertThat(captor.getAllValues().get(0).getActorRoles()).isEqualTo("ROLE_EMPLOYEE,ROLE_HOD");
        assertThat(captor.getAllValues().get(0).getChannel()).isEqualTo("SYSTEM");
    }

    @Test
    void cancellationIsLoggedAgainstTheEmployee() {
        User employeeUser = user(21L, "asha@mahait.org", "Asha Patil", "ROLE_EMPLOYEE");
        when(employeeRepository.findById(101L)).thenReturn(Optional.of(employee(employeeUser, "Developer")));
        when(userRepository.findById(21L)).thenReturn(Optional.of(employeeUser));

        service.leaveCancelled(leave("CANCELLED", null));

        AttendanceRequestAuditLogEntity event = captureEvent();
        assertThat(event.getActionType()).isEqualTo("CANCEL");
        assertThat(event.getPreviousStatus()).isEqualTo("PENDING");
        assertThat(event.getNewStatus()).isEqualTo("CANCELLED");
        assertThat(event.getActorUserId()).isEqualTo(21L);
    }

    @Test
    void tourDecisionIsLoggedAsTourRequest() {
        when(userRepository.findById(9L)).thenReturn(Optional.of(user(9L, "hod@mahait.org", "HOD", "ROLE_HOD")));
        TourApplicationEntity tour = new TourApplicationEntity();
        tour.setTourId(30L);
        tour.setEmployeeId(101L);
        tour.setTourCategory("Half Day");
        tour.setTimePeriod("Fore Noon");
        tour.setStartDate(LocalDate.of(2026, 10, 5));
        tour.setEndDate(LocalDate.of(2026, 10, 5));
        tour.setStatus("APPROVED");

        service.tourDecided(tour, "APPROVED", null, "Approved", 9L);

        AttendanceRequestAuditLogEntity event = captureEvent();
        assertThat(event.getRequestType()).isEqualTo("TOUR");
        assertThat(event.getRequestId()).isEqualTo(30L);
        assertThat(event.getActionType()).isEqualTo("APPROVE");
        assertThat(event.getRequestCategory()).isEqualTo("Half Day / Fore Noon");
    }

    @Test
    void auditRowTakesTheClientIpOfTheRequestWhenSaved() {
        AttendanceRequestAuditLogEntity event = new AttendanceRequestAuditLogEntity();
        event.setIpAddress("1.2.3.4");

        try (var context = AuditRequestContext.open("10.20.30.40")) {
            new AuditIpEntityListener().capture(event);
        }

        assertThat(event.getIpAddress()).isEqualTo("10.20.30.40");
    }

    private AttendanceRequestAuditLogEntity captureEvent() {
        ArgumentCaptor<AttendanceRequestAuditLogEntity> captor =
                ArgumentCaptor.forClass(AttendanceRequestAuditLogEntity.class);
        verify(auditLogRepository).save(captor.capture());
        return captor.getValue();
    }

    private void bindRequest(String uri, String userAgent) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
        request.addHeader("User-Agent", userAgent);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private LeaveApplicationEntity leave(String status, String stage) {
        LeaveApplicationEntity leave = new LeaveApplicationEntity();
        leave.setLeaveId(12L);
        leave.setEmployeeId(101L);
        leave.setLeaveType("CL");
        leave.setLeaveCategory("FULL_DAY");
        leave.setStartDate(LocalDate.of(2026, 10, 10));
        leave.setEndDate(LocalDate.of(2026, 10, 12));
        leave.setStatus(status);
        leave.setApprovalStage(stage);
        return leave;
    }

    private EmployeeEntity employee(User user, String designationName) {
        ManpowerDesignationMaster designation = new ManpowerDesignationMaster();
        designation.setDesignationName(designationName);
        EmployeeEntity employee = new EmployeeEntity();
        employee.setUser(user);
        employee.setDesignation(designation);
        return employee;
    }

    private User user(Long id, String email, String name, String... roleNames) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setName(name);
        user.setRoles(List.of(roleNames).stream().map(roleName -> {
            Role role = new Role();
            role.setName(roleName);
            return role;
        }).toList());
        return user;
    }
}
