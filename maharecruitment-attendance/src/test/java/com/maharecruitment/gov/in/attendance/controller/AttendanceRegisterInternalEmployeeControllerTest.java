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

import java.time.LocalDate;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.maharecruitment.gov.in.attendance.dto.AttendanceRegisterDTO;
import com.maharecruitment.gov.in.attendance.dto.AttendanceLocationDTO;
import com.maharecruitment.gov.in.attendance.dto.EmployeeAttendanceRequestDTO;
import com.maharecruitment.gov.in.attendance.dto.ManualAttendanceRequestDTO;
import com.maharecruitment.gov.in.attendance.entity.LeaveApplicationEntity;
import com.maharecruitment.gov.in.attendance.entity.TourApplicationEntity;
import com.maharecruitment.gov.in.attendance.service.AttendanceRegisterService;
import com.maharecruitment.gov.in.attendance.service.HolidayService;
import com.maharecruitment.gov.in.attendance.service.LeaveApplicationService;
import com.maharecruitment.gov.in.attendance.service.TourApplicationService;
import com.maharecruitment.gov.in.attendance.service.WeekOffWorkingDayService;
import com.maharecruitment.gov.in.auth.entity.DepartmentRegistrationEntity;
import com.maharecruitment.gov.in.common.dto.SessionUserDTO;
import com.maharecruitment.gov.in.master.entity.SubDepartment;
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

    @Mock
    private LeaveApplicationService leaveApplicationService;

    @Mock
    private TourApplicationService tourApplicationService;

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
        ReflectionTestUtils.setField(controller, "leaveApplicationService", leaveApplicationService);
        ReflectionTestUtils.setField(controller, "tourApplicationService", tourApplicationService);

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
        AttendanceLocationDTO primaryLocation = new AttendanceLocationDTO(
                11L, "Head Office", "Mumbai", null, null, 150, true);
        AttendanceLocationDTO secondaryLocation = new AttendanceLocationDTO(
                12L, "Regional Office", "Pune", null, null, 100, false);
        attendance.setPrimaryLocation(primaryLocation);
        attendance.setSecondaryLocations(List.of(secondaryLocation));
        attendance.setAllMappedLocations(List.of(primaryLocation, secondaryLocation));

        when(employeeRepository.findByUser_Id(41L)).thenReturn(Optional.of(employee));
        when(attendanceService.getInternalAttendanceForEmployee(eq(501L), anyInt(), anyInt()))
                .thenReturn(attendance);
        when(attendanceService.getMyManualRequests(501L)).thenReturn(List.of());

        mockMvc.perform(get("/employee/intAttendance").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("attendance/attendance-register-internal"))
                .andExpect(model().attribute("externalEmployee", false))
                .andExpect(model().attribute(
                        "attendance",
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.hasProperty(
                                        "aadhaarNumber",
                                        org.hamcrest.Matchers.is("XXXXXXXX9012")),
                                org.hamcrest.Matchers.hasProperty(
                                        "primaryLocation",
                                        org.hamcrest.Matchers.hasProperty(
                                                "displayName",
                                                org.hamcrest.Matchers.is("Head Office - Mumbai"))),
                                org.hamcrest.Matchers.hasProperty(
                                        "secondaryLocations",
                                        org.hamcrest.Matchers.hasSize(1)))));

        mockMvc.perform(post("/employee/fetchMyAttendance")
                        .session(session)
                        .param("dateRange", "06-2026"))
                .andExpect(status().isOk())
                .andExpect(view().name("attendance/attendance-register-internal"))
                .andExpect(model().attribute("externalEmployee", false))
                .andExpect(model().attribute(
                        "attendance",
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.hasProperty(
                                        "aadhaarNumber",
                                        org.hamcrest.Matchers.is("XXXXXXXX9012")),
                                org.hamcrest.Matchers.hasProperty(
                                        "primaryLocation",
                                        org.hamcrest.Matchers.hasProperty(
                                                "displayName",
                                                org.hamcrest.Matchers.is("Head Office - Mumbai"))),
                                org.hamcrest.Matchers.hasProperty(
                                        "secondaryLocations",
                                        org.hamcrest.Matchers.hasSize(1)))));
    }

    @Test
    void externalEmployeeAttendanceShowsTheirDepartment() throws Exception {
        MockHttpSession session = authenticatedSession(41L);
        EmployeeEntity employee = employee(501L, "123456789012");
        employee.setRecruitmentType("external");
        DepartmentRegistrationEntity department = new DepartmentRegistrationEntity();
        department.setDepartmentName("Finance Department");
        employee.setDepartmentRegistration(department);
        SubDepartment subDepartment = new SubDepartment();
        subDepartment.setSubDeptName("Accounts");
        employee.setSubDepartment(subDepartment);

        when(employeeRepository.findByUser_Id(41L)).thenReturn(Optional.of(employee));
        when(attendanceService.getInternalAttendanceForEmployee(eq(501L), anyInt(), anyInt()))
                .thenReturn(new AttendanceRegisterDTO());
        when(attendanceService.getMyManualRequests(501L)).thenReturn(List.of());

        mockMvc.perform(get("/employee/intAttendance").session(session))
                .andExpect(status().isOk())
                .andExpect(model().attribute("externalEmployee", true))
                .andExpect(model().attribute("employeeDepartment", "Finance Department"))
                .andExpect(model().attribute("employeeSubDepartment", "Accounts"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void attendanceViewGroupsAllManualLeaveAndTourRequestsByFinalStatus() throws Exception {
        MockHttpSession session = authenticatedSession(41L);
        EmployeeEntity employee = employee(501L, "123456789012");
        LocalDateTime submittedAt = LocalDateTime.of(2025, 12, 10, 10, 30);

        ManualAttendanceRequestDTO pendingAttendance = manualRequest(
                LocalDate.of(2025, 12, 1), submittedAt.minusDays(1), "PENDING");
        ManualAttendanceRequestDTO rejectedAttendance = manualRequest(
                LocalDate.of(2025, 11, 1), submittedAt.minusDays(2), "REJECTED");
        LeaveApplicationEntity pendingLeave = leaveRequest(
                LocalDate.of(2025, 10, 1), submittedAt, "PENDING");
        LeaveApplicationEntity approvedLeave = leaveRequest(
                LocalDate.of(2025, 9, 1), submittedAt.minusDays(3), "ACCEPTED");
        TourApplicationEntity approvedTour = tourRequest(
                LocalDate.of(2025, 8, 1), submittedAt.minusDays(4), "APPROVED");
        TourApplicationEntity rejectedTour = tourRequest(
                LocalDate.of(2025, 7, 1), submittedAt.minusDays(5), "REJECTED");

        when(employeeRepository.findByUser_Id(41L)).thenReturn(Optional.of(employee));
        when(attendanceService.getInternalAttendanceForEmployee(eq(501L), anyInt(), anyInt()))
                .thenReturn(new AttendanceRegisterDTO());
        when(attendanceService.getMyManualRequests(501L))
                .thenReturn(List.of(pendingAttendance, rejectedAttendance));
        when(leaveApplicationService.getLeaveApplicationsByEmployee(501L))
                .thenReturn(List.of(pendingLeave, approvedLeave));
        when(tourApplicationService.getTourApplicationsByEmployee(501L))
                .thenReturn(List.of(approvedTour, rejectedTour));

        MvcResult result = mockMvc.perform(get("/employee/intAttendance").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("attendance/attendance-register-internal"))
                .andExpect(model().attribute("pendingRequests", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(model().attribute("approvedRequests", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(model().attribute("rejectedRequests", org.hamcrest.Matchers.hasSize(2)))
                .andReturn();

        List<EmployeeAttendanceRequestDTO> pendingRequests = (List<EmployeeAttendanceRequestDTO>)
                result.getModelAndView().getModel().get("pendingRequests");
        List<EmployeeAttendanceRequestDTO> approvedRequests = (List<EmployeeAttendanceRequestDTO>)
                result.getModelAndView().getModel().get("approvedRequests");
        List<EmployeeAttendanceRequestDTO> rejectedRequests = (List<EmployeeAttendanceRequestDTO>)
                result.getModelAndView().getModel().get("rejectedRequests");

        assertThat(pendingRequests)
                .extracting(EmployeeAttendanceRequestDTO::getRequestType)
                .containsExactlyInAnyOrder("MANUAL_ATTENDANCE", "LEAVE");
        assertThat(approvedRequests)
                .extracting(EmployeeAttendanceRequestDTO::getRequestType)
                .containsExactlyInAnyOrder("LEAVE", "TOUR");
        assertThat(approvedRequests)
                .extracting(EmployeeAttendanceRequestDTO::getStatus)
                .containsOnly("APPROVED");
        assertThat(rejectedRequests)
                .extracting(EmployeeAttendanceRequestDTO::getRequestType)
                .containsExactlyInAnyOrder("MANUAL_ATTENDANCE", "TOUR");
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

    private ManualAttendanceRequestDTO manualRequest(
            LocalDate attendanceDate,
            LocalDateTime createdAt,
            String status) {
        ManualAttendanceRequestDTO request = new ManualAttendanceRequestDTO();
        request.setAttendanceDate(attendanceDate);
        request.setCreatedAt(createdAt);
        request.setReason("Attendance correction");
        request.setInTime("10:00");
        request.setOutTime("18:00");
        request.setHodStatus(status);
        return request;
    }

    private LeaveApplicationEntity leaveRequest(
            LocalDate startDate,
            LocalDateTime applicationDate,
            String status) {
        LeaveApplicationEntity request = new LeaveApplicationEntity();
        request.setStartDate(startDate);
        request.setEndDate(startDate.plusDays(1));
        request.setApplicationDate(applicationDate);
        request.setLeaveType("CL");
        request.setLeaveCategory("Full Day");
        request.setDescription("Personal work");
        request.setStatus(status);
        return request;
    }

    private TourApplicationEntity tourRequest(
            LocalDate startDate,
            LocalDateTime applicationDate,
            String status) {
        TourApplicationEntity request = new TourApplicationEntity();
        request.setStartDate(startDate);
        request.setEndDate(startDate);
        request.setApplicationDate(applicationDate);
        request.setTourCategory("Full Day");
        request.setDescription("Official visit");
        request.setStatus(status);
        return request;
    }
}
