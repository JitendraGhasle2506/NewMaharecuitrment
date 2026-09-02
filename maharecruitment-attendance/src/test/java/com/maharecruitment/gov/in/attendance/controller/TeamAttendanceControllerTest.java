package com.maharecruitment.gov.in.attendance.controller;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.maharecruitment.gov.in.attendance.dto.AttendanceRegisterDTO;
import com.maharecruitment.gov.in.attendance.service.AttendanceRegisterService;
import com.maharecruitment.gov.in.attendance.service.TeamAttendanceService;
import com.maharecruitment.gov.in.attendance.service.model.TeamAttendanceMemberView;
import com.maharecruitment.gov.in.attendance.service.model.TeamAttendanceOverview;
import com.maharecruitment.gov.in.common.dto.SessionUserDTO;

@ExtendWith(MockitoExtension.class)
class TeamAttendanceControllerTest {

    @Mock
    private AttendanceRegisterService attendanceService;

    @Mock
    private TeamAttendanceService teamAttendanceService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new TeamAttendanceController(attendanceService, teamAttendanceService)).build();
    }

    @Test
    void overviewUsesLoggedInAuthorityInsteadOfClientSuppliedRole() throws Exception {
        YearMonth period = YearMonth.of(2026, 8);
        TeamAttendanceOverview overview = new TeamAttendanceOverview(
                period,
                LocalDate.of(2026, 8, 31),
                List.of(member(101L)),
                1,
                0,
                0,
                18,
                1,
                1,
                0,
                90);
        when(teamAttendanceService.getOverview(7L, period)).thenReturn(overview);

        mockMvc.perform(get("/reporting-manager/attendance")
                        .session(authenticatedSession())
                        .param("month", "8")
                        .param("year", "2026")
                        .param("roleType", "HOD"))
                .andExpect(status().isOk())
                .andExpect(view().name("attendance/team-attendance-list"))
                .andExpect(model().attribute("overview", overview))
                .andExpect(model().attribute("selectedMonth", 8))
                .andExpect(model().attribute("selectedYear", 2026));

        verify(teamAttendanceService).getOverview(7L, period);
    }

    @Test
    void employeeRegisterRejectsEmployeeOutsideReportingAuthority() throws Exception {
        YearMonth period = YearMonth.of(2026, 8);
        when(teamAttendanceService.getAuthorizedMember(7L, 999L, period)).thenReturn(Optional.empty());

        mockMvc.perform(get("/reporting-manager/attendance/view")
                        .session(authenticatedSession())
                        .param("empId", "999")
                        .param("month", "8")
                        .param("year", "2026"))
                .andExpect(status().isForbidden());

        verify(attendanceService, never()).getInternalAttendanceForEmployee(999L, 8, 2026);
    }

    @Test
    void employeeRegisterLoadsAttendanceAfterAuthorityCheck() throws Exception {
        YearMonth period = YearMonth.of(2026, 8);
        TeamAttendanceMemberView member = member(101L);
        AttendanceRegisterDTO attendance = new AttendanceRegisterDTO();
        when(teamAttendanceService.getAuthorizedMember(7L, 101L, period)).thenReturn(Optional.of(member));
        when(attendanceService.getInternalAttendanceForEmployee(101L, 8, 2026)).thenReturn(attendance);

        mockMvc.perform(get("/reporting-manager/attendance/view")
                        .session(authenticatedSession())
                        .param("empId", "101")
                        .param("month", "8")
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(view().name("attendance/view-employee-attendance"))
                .andExpect(model().attribute("member", member))
                .andExpect(model().attribute("attendance", attendance));

        verify(attendanceService).getInternalAttendanceForEmployee(101L, 8, 2026);
    }

    private MockHttpSession authenticatedSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("SESSION_USER", new SessionUserDTO(
                7L,
                "Reporting Manager",
                "manager@example.gov.in",
                List.of("ROLE_EMPLOYEE"),
                null,
                null,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()));
        return session;
    }

    private TeamAttendanceMemberView member(Long employeeId) {
        return new TeamAttendanceMemberView(
                employeeId,
                "EMP101",
                "Asha Patil",
                "AP",
                "Developer",
                "Applications",
                "Recruitment Portal",
                "PRESENT",
                "09:30",
                "18:00",
                18,
                1,
                1,
                0,
                1,
                8,
                90);
    }
}
