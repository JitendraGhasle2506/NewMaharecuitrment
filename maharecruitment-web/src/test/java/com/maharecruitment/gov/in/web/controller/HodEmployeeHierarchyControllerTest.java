package com.maharecruitment.gov.in.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpSession;
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
}
