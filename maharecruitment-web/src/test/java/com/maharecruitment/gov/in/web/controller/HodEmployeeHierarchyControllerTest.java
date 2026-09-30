package com.maharecruitment.gov.in.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import com.maharecruitment.gov.in.common.dto.SessionUserDTO;
import com.maharecruitment.gov.in.recruitment.dto.organization.EmployeeHierarchyNode;
import com.maharecruitment.gov.in.recruitment.dto.organization.EmployeeReportingType;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.recruitment.service.organization.EmployeeHierarchyService;

class HodEmployeeHierarchyControllerTest {
    private EmployeeHierarchyService hierarchyService;
    private EmployeeRepository employeeRepository;
    private HodEmployeeHierarchyController controller;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        hierarchyService = mock(EmployeeHierarchyService.class);
        employeeRepository = mock(EmployeeRepository.class);
        controller = new HodEmployeeHierarchyController(hierarchyService, employeeRepository);
        session = new MockHttpSession();
        session.setAttribute("SESSION_USER", new SessionUserDTO(
                10L, "HOD User", "hod@example.test", List.of("ROLE_HOD"), null, null, null,
                LocalDateTime.now(), LocalDateTime.now()));

        EmployeeEntity hod = new EmployeeEntity();
        hod.setEmployeeId(100L);
        hod.setEmployeeCode("HOD100");
        hod.setFullName("HOD User");
        when(employeeRepository.findByUser_Id(10L)).thenReturn(Optional.of(hod));
    }

    @Test
    void reportingChainUsesTheSessionEmployeeAndRejectsMissingSessions() throws Exception {
        when(hierarchyService.reportingChain(100L, EmployeeReportingType.PRIMARY)).thenReturn(List.of(
                new EmployeeHierarchyService.ReportingChainMember(1L, "Upper Manager", "Director", false),
                new EmployeeHierarchyService.ReportingChainMember(100L, "Employee", "Manager", true)));
        var mvc = MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(get("/employee/api/employee-hierarchy/reporting-chain").session(session)
                .param("employeeId", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].employeeName").value("Upper Manager"))
                .andExpect(jsonPath("$.data[1].employeeId").value(100))
                .andExpect(jsonPath("$.data[1].currentEmployee").value(true));
        verify(hierarchyService).reportingChain(100L, EmployeeReportingType.PRIMARY);
        mvc.perform(get("/employee/api/employee-hierarchy/reporting-chain"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void exposesOnlyTheLoggedInHodAsAnOption() {
        var options = controller.options(session).data();

        assertThat(options.hods()).containsExactly(
                new EmployeeHierarchyService.Option(100L, "HOD User (HOD100)"));
        assertThat(options.departments()).isEmpty();
        assertThat(options.designations()).isEmpty();
    }

    @Test
    void loadsTheLoggedInHodsOwnSubordinateTree() {
        EmployeeHierarchyNode root = new EmployeeHierarchyNode(
                100L, "HOD User", "HOD100", "HOD", "IT", null, 0, 0, true);
        when(hierarchyService.tree(100L, null, EmployeeReportingType.PRIMARY, null, null, 0, 25, 1))
                .thenReturn(root);

        assertThat(controller.tree(100L, EmployeeReportingType.PRIMARY, null, 0, 25, 1, session).data())
                .isSameAs(root);
        verify(hierarchyService).tree(100L, null, EmployeeReportingType.PRIMARY, null, null, 0, 25, 1);
    }

    @Test
    void rejectsAnotherEmployeesHierarchyRoot() {
        assertThatThrownBy(() -> controller.tree(
                999L, EmployeeReportingType.PRIMARY, null, 0, 25, 1, session))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void employeeRoutesAutomaticallyOfferOnlyTheSignedInEmployeeAndLoadTheirTree() throws Exception {
        session.setAttribute("SESSION_USER", new SessionUserDTO(
                10L, "Employee", "employee@example.test", List.of("ROLE_EMPLOYEE"), null, null, null,
                LocalDateTime.now(), LocalDateTime.now()));
        EmployeeHierarchyNode root = new EmployeeHierarchyNode(
                100L, "Employee", "EMP100", "Manager", "IT", null, 0, 0, true);
        when(hierarchyService.tree(100L, null, EmployeeReportingType.PRIMARY, null, null, 0, 25, 1))
                .thenReturn(root);
        var mvc = MockMvcBuilders.standaloneSetup(controller).build();

        mvc.perform(get("/employee/api/employee-hierarchy/options").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hods.length()").value(1))
                .andExpect(jsonPath("$.data.hods[0].id").value(100));
        mvc.perform(get("/employee/api/employee-hierarchy/100").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.employeeId").value(100));
    }

    @Test
    void executiveRouteLoadsOnlyTheSignedInExecutivesTree() throws Exception {
        session.setAttribute("SESSION_USER", new SessionUserDTO(
                10L, "COO User", "coo@example.test", List.of("ROLE_COO"), null, null, null,
                LocalDateTime.now(), LocalDateTime.now()));
        EmployeeHierarchyNode root = new EmployeeHierarchyNode(
                100L, "COO User", "COO100", "COO", "Executive", null, 0, 0, true);
        when(hierarchyService.tree(100L, null, EmployeeReportingType.PRIMARY, null, null, 0, 25, 1))
                .thenReturn(root);
        var mvc = MockMvcBuilders.standaloneSetup(controller).build();

        mvc.perform(get("/coo/api/employee-hierarchy/options").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hods[0].id").value(100));
        mvc.perform(get("/coo/api/employee-hierarchy/100").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.employeeId").value(100));
        mvc.perform(get("/coo/api/employee-hierarchy/999").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void employeeTreeAndSearchRejectOtherRootsAndMissingSessions() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(get("/employee/api/employee-hierarchy/999").session(session))
                .andExpect(status().isForbidden());
        mvc.perform(get("/employee/api/employee-hierarchy/999/search").param("q", "name").session(session))
                .andExpect(status().isForbidden());
        mvc.perform(get("/employee/api/employee-hierarchy/options"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(hierarchyService);
    }

    @Test
    void employeeSearchAndPaginationStayWithinTheirOwnRoot() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(get("/employee/api/employee-hierarchy/100/search").param("q", "team").session(session))
                .andExpect(status().isOk());
        verify(hierarchyService).search(100L, EmployeeReportingType.PRIMARY, null, null, "team");
        mvc.perform(get("/employee/api/employee-hierarchy/100")
                .param("nodeId", "101").param("offset", "25").session(session))
                .andExpect(status().isOk());
        verify(hierarchyService).tree(100L, 101L, EmployeeReportingType.PRIMARY, null, null, 25, 25, 1);
    }
}
