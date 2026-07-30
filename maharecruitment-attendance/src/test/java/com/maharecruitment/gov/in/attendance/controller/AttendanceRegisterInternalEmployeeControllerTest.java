package com.maharecruitment.gov.in.attendance.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.maharecruitment.gov.in.attendance.dto.AttendanceRegisterDTO;
import com.maharecruitment.gov.in.attendance.service.AttendanceRegisterService;
import com.maharecruitment.gov.in.attendance.service.HolidayService;
import com.maharecruitment.gov.in.attendance.service.WeekOffWorkingDayService;
import com.maharecruitment.gov.in.common.dto.SessionUserDTO;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;

@ExtendWith(MockitoExtension.class)
class AttendanceRegisterInternalEmployeeControllerTest {

    private static final String REVEAL_PATH = "/employee/intAttendance/aadhaar/reveal";

    @Mock
    private AttendanceRegisterService attendanceService;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private HolidayService holidayService;

    @Mock
    private WeekOffWorkingDayService weekOffWorkingDayService;

    private MockMvc mockMvc;
    private MockMvc csrfProtectedMockMvc;

    @BeforeEach
    void setUp() {
        AttendanceRegisterInternalEmployeeController controller =
                new AttendanceRegisterInternalEmployeeController();
        ReflectionTestUtils.setField(controller, "attendanceService", attendanceService);
        ReflectionTestUtils.setField(controller, "employeeRepository", employeeRepository);
        ReflectionTestUtils.setField(controller, "holidayService", holidayService);
        ReflectionTestUtils.setField(controller, "weekOffWorkingDayService", weekOffWorkingDayService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        csrfProtectedMockMvc = MockMvcBuilders.standaloneSetup(controller)
                .addFilters(new CsrfFilter(new HttpSessionCsrfTokenRepository()))
                .build();
    }

    @Test
    void revealReturnsOnlyAuthenticatedEmployeesNormalizedAadhaarAndDisablesCaching() throws Exception {
        MockHttpSession session = authenticatedSession(41L);
        EmployeeEntity employee = employee(501L, "1234 5678 9012");
        when(employeeRepository.findByUser_Id(41L)).thenReturn(Optional.of(employee));

        mockMvc.perform(post(REVEAL_PATH).session(session))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.CACHE_CONTROL,
                        "no-store, no-cache, must-revalidate"))
                .andExpect(header().string(HttpHeaders.PRAGMA, "no-cache"))
                .andExpect(result ->
                        assertThat(result.getResponse().getContentAsString()).isEqualTo("123456789012"));

        verify(employeeRepository).findByUser_Id(41L);
    }

    @Test
    void revealRejectsRequestWithoutCsrfBeforeReadingEmployeeData() throws Exception {
        MockHttpSession session = authenticatedSession(41L);

        csrfProtectedMockMvc.perform(post(REVEAL_PATH).session(session))
                .andExpect(status().isForbidden());

        verify(employeeRepository, never()).findByUser_Id(41L);
    }

    @Test
    void revealReturnsUnauthorizedWhenSessionUserIsMissing() throws Exception {
        mockMvc.perform(post(REVEAL_PATH).session(new MockHttpSession()))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(
                        HttpHeaders.CACHE_CONTROL,
                        "no-store, no-cache, must-revalidate"))
                .andExpect(result -> assertThat(result.getResponse().getContentAsString()).isEmpty());
    }

    @Test
    void revealReturnsForbiddenWhenSessionUserHasNoEmployeeMapping() throws Exception {
        MockHttpSession session = authenticatedSession(99L);
        when(employeeRepository.findByUser_Id(99L)).thenReturn(Optional.empty());

        mockMvc.perform(post(REVEAL_PATH).session(session))
                .andExpect(status().isForbidden())
                .andExpect(result -> assertThat(result.getResponse().getContentAsString()).isEmpty());
    }

    @Test
    void initialAndMonthFilteredViewsKeepMaskedAadhaarInTheInitialModel() throws Exception {
        MockHttpSession session = authenticatedSession(41L);
        EmployeeEntity employee = employee(501L, "123456789012");
        AttendanceRegisterDTO attendance = new AttendanceRegisterDTO();
        attendance.setAadhaarNumber("XXXXXXXX9012");

        when(employeeRepository.findByUser_Id(41L)).thenReturn(Optional.of(employee));
        when(attendanceService.getInternalAttendanceForEmployee(eq(501L), anyInt(), anyInt()))
                .thenReturn(attendance);
        when(attendanceService.getMyManualRequests(501L)).thenReturn(List.of());

        mockMvc.perform(get("/employee/intAttendance").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("attendance/attendance-register-internal"))
                .andExpect(model().attribute(
                        "attendance",
                        org.hamcrest.Matchers.hasProperty("aadhaarNumber", org.hamcrest.Matchers.is("XXXXXXXX9012"))));

        mockMvc.perform(post("/employee/fetchMyAttendance")
                        .session(session)
                        .param("dateRange", "06-2026"))
                .andExpect(status().isOk())
                .andExpect(view().name("attendance/attendance-register-internal"))
                .andExpect(model().attribute(
                        "attendance",
                        org.hamcrest.Matchers.hasProperty("aadhaarNumber", org.hamcrest.Matchers.is("XXXXXXXX9012"))));
    }

    private MockHttpSession authenticatedSession(Long userId) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("SESSION_USER", new SessionUserDTO(
                userId,
                "Internal Employee",
                "employee@example.gov.in",
                List.of("ROLE_EMPLOYEE"),
                null,
                null,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()));
        return session;
    }

    private EmployeeEntity employee(Long employeeId, String aadhaar) {
        EmployeeEntity employee = new EmployeeEntity();
        employee.setEmployeeId(employeeId);
        employee.setAadhaarNumber(aadhaar);
        return employee;
    }
}
