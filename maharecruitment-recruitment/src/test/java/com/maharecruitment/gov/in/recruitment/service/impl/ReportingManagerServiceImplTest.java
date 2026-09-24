package com.maharecruitment.gov.in.recruitment.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maharecruitment.gov.in.auth.entity.Role;
import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.master.entity.CellMaster;
import com.maharecruitment.gov.in.master.entity.ProjectMst;
import com.maharecruitment.gov.in.master.entity.WingMaster;
import com.maharecruitment.gov.in.master.repository.CellMasterRepository;
import com.maharecruitment.gov.in.master.repository.ProjectMstRepository;
import com.maharecruitment.gov.in.recruitment.entity.CellReportingAuthorityMappingEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeCellMappingEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeReportingMappingEntity;
import com.maharecruitment.gov.in.recruitment.repository.CellReportingAuthorityMappingRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeCellMappingRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeReportingHodProjection;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeReportingMappingRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.recruitment.repository.organization.PositionMasterRepository;

@ExtendWith(MockitoExtension.class)
class ReportingManagerServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private PositionMasterRepository positionRepository;

    @Mock
    private ProjectMstRepository projectRepository;

    @Mock
    private EmployeeReportingMappingRepository mappingRepository;

    @Mock
    private CellMasterRepository cellMasterRepository;

    @Mock
    private EmployeeCellMappingRepository employeeCellMappingRepository;

    @Mock
    private CellReportingAuthorityMappingRepository cellAuthorityMappingRepository;

    @InjectMocks
    private ReportingManagerServiceImpl service;

    @Test
    void getReportingAuthoritiesReturnsAllSupportedRolesInDisplayOrder() {
        User hod = userWithRole(7L, "Anita Deshmukh", "ROLE_HOD");
        User coo = userWithRole(8L, "Ravi Shah", "ROLE_COO");
        User stm = userWithRole(9L, "Sanjay Patil", "ROLE_STM");
        User pm = userWithRole(10L, "Priya Jadhav", "ROLE_PM");
        when(userRepository.findDistinctUserIdsByRoleName("ROLE_HOD")).thenReturn(List.of(7L));
        when(userRepository.findDistinctUserIdsByRoleName("ROLE_COO")).thenReturn(List.of(8L));
        when(userRepository.findDistinctUserIdsByRoleName("ROLE_STM")).thenReturn(List.of(9L));
        when(userRepository.findDistinctUserIdsByRoleName("ROLE_PM")).thenReturn(List.of(10L));
        when(userRepository.findAllById(Set.of(7L, 8L, 9L, 10L)))
                .thenReturn(List.of(pm, stm, hod, coo));

        List<Map<String, Object>> result = service.getReportingAuthorities();

        assertEquals(4, result.size());
        assertEquals("COO", result.get(0).get("authorityType"));
        assertEquals(8L, result.get(0).get("id"));
        assertEquals("HOD", result.get(1).get("authorityType"));
        assertEquals(7L, result.get(1).get("id"));
        assertEquals("STM", result.get(2).get("authorityType"));
        assertEquals(9L, result.get(2).get("id"));
        assertEquals("PM", result.get(3).get("authorityType"));
        assertEquals(10L, result.get(3).get("id"));
    }

    @Test
    void getReportingAuthoritiesIncludesStmAndPmWithoutAuthorityRoles() {
        User stm = userWithRole(9L, "Sanjay Patil", "ROLE_EMPLOYEE");
        User pm = userWithRole(10L, "Priya Jadhav", "ROLE_EMPLOYEE");
        EmployeeEntity stmEmployee = employee(90L, "Sanjay Patil", "EMP090", "INTERNAL", "ACTIVE");
        EmployeeEntity pmEmployee = employee(100L, "Priya Jadhav", "EMP100", "INTERNAL", "ACTIVE");
        stmEmployee.setUser(stm);
        pmEmployee.setUser(pm);

        when(employeeRepository.findActiveEmployeesByRoleNameOrDesignationNames(
                "ROLE_STM",
                Set.of("STM", "SENIOR TECHNICAL MANAGER", "SENIOR TECHNICAL MANAGER (STM)"),
                "%SENIOR%TECHNICAL%MANAGER%"))
                .thenReturn(List.of(stmEmployee));
        when(employeeRepository.findActiveEmployeesByRoleNameOrDesignationNames(
                "ROLE_PM",
                Set.of("PM", "PROJECT MANAGER", "PROJECT MANAGER (PM)"),
                "%PROJECT%MANAGER%"))
                .thenReturn(List.of());
        when(positionRepository.findFilledActiveEmployeesByManagerNames(
                Set.of("STM", "SENIOR TECHNICAL MANAGER", "SENIOR TECHNICAL MANAGER (STM)"),
                "%SENIOR%TECHNICAL%MANAGER%",
                "ROLE_STM",
                com.maharecruitment.gov.in.recruitment.entity.organization.OrganizationRecordStatus.ACTIVE,
                com.maharecruitment.gov.in.recruitment.entity.organization.PositionStatus.FILLED,
                "ACTIVE"))
                .thenReturn(List.of());
        when(positionRepository.findFilledActiveEmployeesByManagerNames(
                Set.of("PM", "PROJECT MANAGER", "PROJECT MANAGER (PM)"),
                "%PROJECT%MANAGER%",
                "ROLE_PM",
                com.maharecruitment.gov.in.recruitment.entity.organization.OrganizationRecordStatus.ACTIVE,
                com.maharecruitment.gov.in.recruitment.entity.organization.PositionStatus.FILLED,
                "ACTIVE"))
                .thenReturn(List.of(pmEmployee));
        when(userRepository.findAllById(Set.of(9L, 10L))).thenReturn(List.of(pm, stm));

        List<Map<String, Object>> result = service.getReportingAuthorities();

        assertEquals(2, result.size());
        assertEquals("STM", result.get(0).get("authorityType"));
        assertEquals(9L, result.get(0).get("id"));
        assertEquals("PM", result.get(1).get("authorityType"));
        assertEquals(10L, result.get(1).get("id"));
    }

    @Test
    void getReportingAuthoritiesIncludesPmMatchedToActiveUserByEmail() {
        User pm = userWithRole(10L, "Priya Jadhav", "ROLE_EMPLOYEE");
        pm.setEmail("priya.jadhav@example.com");
        EmployeeEntity pmEmployee = employee(100L, "Priya Jadhav", "EMP100", "INTERNAL", "ACTIVE");
        pmEmployee.setEmail("Priya.Jadhav@example.com");
        when(employeeRepository.findActiveEmployeesByRoleNameOrDesignationNames(
                "ROLE_STM",
                Set.of("STM", "SENIOR TECHNICAL MANAGER", "SENIOR TECHNICAL MANAGER (STM)"),
                "%SENIOR%TECHNICAL%MANAGER%"))
                .thenReturn(List.of());
        when(employeeRepository.findActiveEmployeesByRoleNameOrDesignationNames(
                "ROLE_PM",
                Set.of("PM", "PROJECT MANAGER", "PROJECT MANAGER (PM)"),
                "%PROJECT%MANAGER%"))
                .thenReturn(List.of(pmEmployee));
        when(userRepository.findActiveUsersByNormalizedEmailIn(Set.of("priya.jadhav@example.com")))
                .thenReturn(List.of(pm));
        when(userRepository.findAllById(Set.of(10L))).thenReturn(List.of(pm));

        List<Map<String, Object>> result = service.getReportingAuthorities();

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).get("id"));
        assertEquals("PM", result.get(0).get("authorityType"));
    }

    @Test
    void getReportingAuthoritiesIncludesActiveInternalEmployeesAsOtherAuthorities() {
        User employeeUser = userWithRole(11L, "Neha Kulkarni", "ROLE_EMPLOYEE");
        EmployeeEntity employee = employee(110L, "Neha Kulkarni", "EMP110", "INTERNAL", "ACTIVE");
        employee.setUser(employeeUser);

        when(employeeRepository
                .findByRecruitmentTypeIgnoreCaseAndStatusIgnoreCaseOrderByFullNameAscEmployeeIdAsc(
                        "INTERNAL", "ACTIVE"))
                .thenReturn(List.of(employee));
        when(userRepository.findAllById(Set.of(11L))).thenReturn(List.of(employeeUser));

        List<Map<String, Object>> result = service.getReportingAuthorities();

        assertEquals(1, result.size());
        assertEquals(11L, result.get(0).get("id"));
        assertEquals("OTHER", result.get(0).get("authorityType"));
    }

    @Test
    void getCellAuthorityUsersIncludesEligibleInternalAndMahaitEmployeeUsers() {
        User first = userWithRole(21L, "Asha", "ROLE_EMPLOYEE");
        User second = userWithRole(22L, "Vijay", "ROLE_AGENCY");
        second.getRoles().clear();
        Role auditorRole = new Role();
        auditorRole.setName("ROLE_AUDITOR");
        second.addRole(auditorRole);
        EmployeeEntity internalEmployee = employee(
                121L, "Asha Employee", "EMP121", "INTERNAL", "ACTIVE");
        internalEmployee.setUser(first);
        EmployeeEntity mahaitEmployee = employee(
                122L, "Vijay Employee", "EMP122", "MAHAIT", "ACTIVE");
        mahaitEmployee.setUser(second);
        when(employeeRepository.findActiveCellAuthorityEmployees(Set.of("INTERNAL", "MAHAIT")))
                .thenReturn(List.of(internalEmployee, mahaitEmployee));

        List<Map<String, Object>> result = service.getCellAuthorityUsers();

        assertEquals(List.of(21L, 22L), result.stream().map(row -> row.get("id")).toList());
        assertEquals("Asha Employee", result.get(0).get("name"));
        assertEquals("EMPLOYEE", result.get(0).get("roles"));
        assertEquals("AUDITOR", result.get(1).get("roles"));
    }

    @Test
    void getProjectsReturnsActiveProjectsForSelection() {
        when(projectRepository.findByActiveFlagIgnoreCaseOrderByProjectNameAsc("Y"))
                .thenReturn(List.of(project(12L, "Citizen Services")));

        List<Map<String, Object>> result = service.getProjects();

        assertEquals(1, result.size());
        assertEquals(12L, result.get(0).get("id"));
        assertEquals("Citizen Services", result.get(0).get("name"));
    }

    @Test
    void getManagersByTypeStmQueriesStmRoleOrDesignation() {
        EmployeeEntity employee = employee(10L, "John Doe", "EMP10", "INTERNAL", "ACTIVE");
        when(employeeRepository.findActiveEmployeesByRoleNameOrDesignationNames(
                "ROLE_STM",
                Set.of("STM", "SENIOR TECHNICAL MANAGER", "SENIOR TECHNICAL MANAGER (STM)"),
                "%SENIOR%TECHNICAL%MANAGER%"))
                .thenReturn(List.of(employee));

        List<Map<String, Object>> result = service.getManagersByType("STM");

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).get("id"));
        assertEquals("John Doe (EMP10)", result.get(0).get("name"));
    }

    @Test
    void getManagersByTypeMarksResourcesThatAreAlreadyMapped() {
        EmployeeEntity employee = employee(10L, "John Doe", "EMP10", "INTERNAL", "ACTIVE");
        EmployeeReportingMappingEntity mapping = new EmployeeReportingMappingEntity();
        mapping.setMappingId(3L);
        mapping.setEmployeeId(10L);
        mapping.setHodUserId(7L);
        mapping.setManagerType("STM");
        when(employeeRepository.findActiveEmployeesByRoleNameOrDesignationNames(
                "ROLE_STM",
                Set.of("STM", "SENIOR TECHNICAL MANAGER", "SENIOR TECHNICAL MANAGER (STM)"),
                "%SENIOR%TECHNICAL%MANAGER%"))
                .thenReturn(List.of(employee));
        when(mappingRepository.findByEmployeeIdIn(Set.of(10L))).thenReturn(List.of(mapping));

        Map<String, Object> result = service.getManagersByType("STM").get(0);

        assertEquals(true, result.get("mapped"));
        assertEquals(7L, result.get("mappedAuthorityUserId"));
        assertEquals("STM", result.get("mappedManagerType"));
    }

    @Test
    void getManagersByTypePmQueriesPmRoleOrDesignation() {
        EmployeeEntity employee = employee(20L, "Jane Smith", "EMP20", "INTERNAL", "ACTIVE");
        when(employeeRepository.findActiveEmployeesByRoleNameOrDesignationNames(
                "ROLE_PM",
                Set.of("PM", "PROJECT MANAGER", "PROJECT MANAGER (PM)"),
                "%PROJECT%MANAGER%"))
                .thenReturn(List.of(employee));

        List<Map<String, Object>> result = service.getManagersByType("PM");

        assertEquals(1, result.size());
        assertEquals(20L, result.get(0).get("id"));
        assertEquals("Jane Smith (EMP20)", result.get(0).get("name"));
    }

    @Test
    void getManagersByTypeHodQueriesHodRoleOrDesignation() {
        EmployeeEntity employee = employee(9L, "HOD Manager", "EMP09", "INTERNAL", "ACTIVE");
        when(employeeRepository.findActiveEmployeesByRoleNameOrDesignationNames(
                "ROLE_HOD",
                Set.of("HOD", "HEAD OF DEPARTMENT", "HEAD OF DEPARTMENT (HOD)"),
                "%HEAD%OF%DEPARTMENT%"))
                .thenReturn(List.of(employee));

        List<Map<String, Object>> result = service.getManagersByType("HOD");

        assertEquals(1, result.size());
        assertEquals(9L, result.get(0).get("id"));
        assertEquals("HOD Manager (EMP09)", result.get(0).get("name"));
    }

    @Test
    void getManagersByTypePmShowsMappedHodAndKeepsUnmappedProjectManagers() {
        EmployeeEntity mappedManager = employee(20L, "Mapped PM", "EMP20", "INTERNAL", "ACTIVE");
        EmployeeEntity unmappedManager = employee(21L, "Unmapped PM", "EMP21", "INTERNAL", "ACTIVE");
        when(employeeRepository.findActiveEmployeesByRoleNameOrDesignationNames(
                "ROLE_PM",
                Set.of("PM", "PROJECT MANAGER", "PROJECT MANAGER (PM)"),
                "%PROJECT%MANAGER%"))
                .thenReturn(List.of(mappedManager, unmappedManager));
        when(mappingRepository.findHodReferencesByEmployeeIdIn(Set.of(20L, 21L)))
                .thenReturn(List.of(mappingHod(20L, 7L, 1L)));
        when(userRepository.findAllById(Set.of(7L))).thenReturn(List.of(user(7L, "Anita Deshmukh")));

        List<Map<String, Object>> result = service.getManagersByType("PM");

        assertEquals(2, result.size());
        assertEquals(20L, result.get(0).get("id"));
        assertEquals("Mapped PM (EMP20) - Mapped HOD: Anita Deshmukh", result.get(0).get("name"));
        assertEquals(7L, result.get(0).get("mappedHodUserId"));
        assertEquals("Anita Deshmukh", result.get(0).get("mappedHodName"));
        assertEquals(21L, result.get(1).get("id"));
        assertEquals("Unmapped PM (EMP21)", result.get(1).get("name"));
        assertNull(result.get(1).get("mappedHodUserId"));
    }

    @Test
    void getManagersByTypePmStillLoadsWhenPositionLookupFails() {
        EmployeeEntity employee = employee(20L, "Jane Smith", "EMP20", "INTERNAL", "ACTIVE");
        when(employeeRepository.findActiveEmployeesByRoleNameOrDesignationNames(
                "ROLE_PM",
                Set.of("PM", "PROJECT MANAGER", "PROJECT MANAGER (PM)"),
                "%PROJECT%MANAGER%"))
                .thenReturn(List.of(employee));
        doThrow(new IllegalStateException("position source unavailable"))
                .when(positionRepository).findFilledActiveEmployeesByManagerNames(
                        Set.of("PM", "PROJECT MANAGER", "PROJECT MANAGER (PM)"),
                        "%PROJECT%MANAGER%",
                        "ROLE_PM",
                        com.maharecruitment.gov.in.recruitment.entity.organization.OrganizationRecordStatus.ACTIVE,
                        com.maharecruitment.gov.in.recruitment.entity.organization.PositionStatus.FILLED,
                        "ACTIVE");

        List<Map<String, Object>> result = service.getManagersByType("PM");

        assertEquals(1, result.size());
        assertEquals(20L, result.get(0).get("id"));
        assertEquals("Jane Smith (EMP20)", result.get(0).get("name"));
    }

    @Test
    void getManagersByTypePmIncludesFilledProjectManagerPositionEmployee() {
        EmployeeEntity employee = employee(21L, "Prakash More", "EMP21", "INTERNAL", "ACTIVE");
        when(positionRepository.findFilledActiveEmployeesByManagerNames(
                Set.of("PM", "PROJECT MANAGER", "PROJECT MANAGER (PM)"),
                "%PROJECT%MANAGER%",
                "ROLE_PM",
                com.maharecruitment.gov.in.recruitment.entity.organization.OrganizationRecordStatus.ACTIVE,
                com.maharecruitment.gov.in.recruitment.entity.organization.PositionStatus.FILLED,
                "ACTIVE"))
                .thenReturn(List.of(employee));

        List<Map<String, Object>> result = service.getManagersByType("PM");

        assertEquals(1, result.size());
        assertEquals(21L, result.get(0).get("id"));
        assertEquals("Prakash More (EMP21)", result.get(0).get("name"));
    }

    @Test
    void getManagersByTypeOtherStillReturnsEligibleActiveEmployees() {
        EmployeeEntity employee = employee(30L, "Asha Patil", "EMP30", "INTERNAL", "ACTIVE");
        when(employeeRepository.findActiveEmployeesNotMappedAsReportingManagers())
                .thenReturn(List.of(employee));

        List<Map<String, Object>> result = service.getManagersByType("OTHER");

        assertEquals(1, result.size());
        assertEquals(30L, result.get(0).get("id"));
    }

    @Test
    void missingManagerTypeStoresDirectReportingAuthorityMapping() {
        EmployeeEntity employee = employee(101L, "Rahul Patil", "EMP101", "INTERNAL", "ACTIVE");
        stubValidHodAndEmployees(7L, employee);
        when(employeeRepository.findByUser_Id(7L)).thenReturn(Optional.empty());
        when(mappingRepository.findByEmployeeIdIn(List.of(101L))).thenReturn(List.of());

        service.saveMapping(7L, null, null, 99L, List.of(101L));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<EmployeeReportingMappingEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(mappingRepository).saveAll(captor.capture());
        EmployeeReportingMappingEntity mapping = captor.getValue().get(0);
        assertEquals(7L, mapping.getHodUserId());
        assertEquals("OTHER", mapping.getManagerType());
        assertNull(mapping.getManagerEmployeeId());
        assertNull(mapping.getProjectId());
        assertEquals(101L, mapping.getEmployeeId());
    }

    @Test
    void stmAllowsMultipleManagersToBeMappedDirectlyToAuthority() {
        EmployeeEntity firstManager = employee(50L, "First STM", "EMP050", "INTERNAL", "ACTIVE");
        EmployeeEntity secondManager = employee(51L, "Second STM", "EMP051", "INTERNAL", "ACTIVE");
        stubValidHodAndEmployees(7L, firstManager, secondManager);
        when(employeeRepository.findActiveEmployeesByRoleNameOrDesignationNames(
                "ROLE_STM",
                Set.of("STM", "SENIOR TECHNICAL MANAGER", "SENIOR TECHNICAL MANAGER (STM)"),
                "%SENIOR%TECHNICAL%MANAGER%"))
                .thenReturn(List.of(firstManager, secondManager));
        when(mappingRepository.findByEmployeeIdIn(List.of(50L, 51L))).thenReturn(List.of());

        service.saveMapping(7L, "STM", null, 12L, List.of(50L, 51L));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<EmployeeReportingMappingEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(mappingRepository).saveAll(captor.capture());
        assertEquals(List.of(50L, 51L),
                captor.getValue().stream().map(EmployeeReportingMappingEntity::getEmployeeId).toList());
        assertEquals(List.of("STM", "STM"),
                captor.getValue().stream().map(EmployeeReportingMappingEntity::getManagerType).toList());
        assertTrue(captor.getValue().stream()
                .allMatch(mapping -> mapping.getManagerEmployeeId() == null && mapping.getProjectId() == null));
    }

    @Test
    void pmAllowsMultipleManagersToBeMappedDirectlyToAuthority() {
        EmployeeEntity firstManager = employee(60L, "First PM", "EMP060", "INTERNAL", "ACTIVE");
        EmployeeEntity secondManager = employee(61L, "Second PM", "EMP061", "INTERNAL", "ACTIVE");
        stubValidHodAndEmployees(7L, firstManager, secondManager);
        when(employeeRepository.findActiveEmployeesByRoleNameOrDesignationNames(
                "ROLE_PM",
                Set.of("PM", "PROJECT MANAGER", "PROJECT MANAGER (PM)"),
                "%PROJECT%MANAGER%"))
                .thenReturn(List.of(firstManager, secondManager));
        when(mappingRepository.findByEmployeeIdIn(List.of(60L, 61L))).thenReturn(List.of());

        service.saveMapping(7L, "PM", null, null, List.of(60L, 61L));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<EmployeeReportingMappingEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(mappingRepository).saveAll(captor.capture());
        assertEquals(List.of(60L, 61L),
                captor.getValue().stream().map(EmployeeReportingMappingEntity::getEmployeeId).toList());
        assertTrue(captor.getValue().stream()
                .allMatch(mapping -> mapping.getManagerEmployeeId() == null && mapping.getProjectId() == null));
    }

    @Test
    void stmAuthorityCanReceiveDirectEmployeeMappings() {
        User stm = userWithRole(9L, "STM Manager", "ROLE_STM");
        EmployeeEntity employee = employee(101L, "Rahul Patil", "EMP101", "INTERNAL", "ACTIVE");
        when(userRepository.findById(9L)).thenReturn(Optional.of(stm));
        when(employeeRepository.findAllById(Set.of(101L))).thenReturn(List.of(employee));
        when(employeeRepository.findByUser_Id(9L)).thenReturn(Optional.empty());
        when(mappingRepository.findByEmployeeIdIn(List.of(101L))).thenReturn(List.of());

        service.saveMapping(9L, "STM", null, null, List.of(101L));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<EmployeeReportingMappingEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(mappingRepository).saveAll(captor.capture());
        EmployeeReportingMappingEntity mapping = captor.getValue().get(0);
        assertEquals(9L, mapping.getHodUserId());
        assertEquals("STM", mapping.getManagerType());
        assertEquals(101L, mapping.getEmployeeId());
        assertNull(mapping.getManagerEmployeeId());
    }

    @Test
    void designationBasedStmAuthorityCanReceiveDirectEmployeeMappings() {
        User stm = userWithRole(9L, "STM Manager", "ROLE_EMPLOYEE");
        EmployeeEntity stmEmployee = employee(90L, "STM Manager", "EMP090", "INTERNAL", "ACTIVE");
        EmployeeEntity employee = employee(101L, "Rahul Patil", "EMP101", "INTERNAL", "ACTIVE");
        stmEmployee.setUser(stm);
        when(userRepository.findById(9L)).thenReturn(Optional.of(stm));
        when(employeeRepository.findActiveEmployeesByRoleNameOrDesignationNames(
                "ROLE_STM",
                Set.of("STM", "SENIOR TECHNICAL MANAGER", "SENIOR TECHNICAL MANAGER (STM)"),
                "%SENIOR%TECHNICAL%MANAGER%"))
                .thenReturn(List.of(stmEmployee));
        when(employeeRepository.findActiveEmployeesByRoleNameOrDesignationNames(
                "ROLE_PM",
                Set.of("PM", "PROJECT MANAGER", "PROJECT MANAGER (PM)"),
                "%PROJECT%MANAGER%"))
                .thenReturn(List.of());
        when(employeeRepository.findAllById(Set.of(101L))).thenReturn(List.of(employee));
        when(employeeRepository.findByUser_Id(9L)).thenReturn(Optional.of(stmEmployee));
        when(mappingRepository.findByEmployeeIdIn(List.of(101L))).thenReturn(List.of());

        service.saveMapping(9L, "STM", null, null, List.of(101L));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<EmployeeReportingMappingEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(mappingRepository).saveAll(captor.capture());
        assertEquals(9L, captor.getValue().get(0).getHodUserId());
        assertEquals("STM", captor.getValue().get(0).getManagerType());
        assertEquals(101L, captor.getValue().get(0).getEmployeeId());
    }

    @Test
    void pmAuthorityCanReceiveDirectEmployeeMappings() {
        User pm = userWithRole(10L, "PM Manager", "ROLE_PM");
        EmployeeEntity employee = employee(102L, "Asha Patil", "EMP102", "INTERNAL", "ACTIVE");
        when(userRepository.findById(10L)).thenReturn(Optional.of(pm));
        when(employeeRepository.findAllById(Set.of(102L))).thenReturn(List.of(employee));
        when(employeeRepository.findByUser_Id(10L)).thenReturn(Optional.empty());
        when(mappingRepository.findByEmployeeIdIn(List.of(102L))).thenReturn(List.of());

        service.saveMapping(10L, "PM", null, null, List.of(102L));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<EmployeeReportingMappingEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(mappingRepository).saveAll(captor.capture());
        EmployeeReportingMappingEntity mapping = captor.getValue().get(0);
        assertEquals(10L, mapping.getHodUserId());
        assertEquals("PM", mapping.getManagerType());
        assertEquals(102L, mapping.getEmployeeId());
        assertNull(mapping.getManagerEmployeeId());
    }

    @Test
    void legacyStmFlowKeepsSelectedManagerButDiscardsProject() {
        EmployeeEntity manager = employee(50L, "STM Manager", "EMP050", "INTERNAL", "ACTIVE");
        EmployeeEntity employee = employee(101L, "Rahul Patil", "EMP101", "INTERNAL", "ACTIVE");
        stubValidHodAndEmployees(7L, employee);
        when(employeeRepository.findById(50L)).thenReturn(Optional.of(manager));
        when(mappingRepository.findByEmployeeIdIn(List.of(101L))).thenReturn(List.of());

        service.saveMapping(7L, "STM", 50L, 12L, List.of(101L));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<EmployeeReportingMappingEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(mappingRepository).saveAll(captor.capture());
        EmployeeReportingMappingEntity mapping = captor.getValue().get(0);
        assertEquals("STM", mapping.getManagerType());
        assertEquals(50L, mapping.getManagerEmployeeId());
        assertNull(mapping.getProjectId());
    }

    @Test
    void pmFlowContinuesToStoreSelectedManager() {
        EmployeeEntity manager = employee(60L, "PM Manager", "EMP060", "INTERNAL", "ACTIVE");
        EmployeeEntity employee = employee(102L, "Asha Patil", "EMP102", "INTERNAL", "ACTIVE");
        stubValidHodAndEmployees(7L, employee);
        when(employeeRepository.findById(60L)).thenReturn(Optional.of(manager));
        when(mappingRepository.findByEmployeeIdIn(List.of(102L))).thenReturn(List.of());

        service.saveMapping(7L, "PM", 60L, null, List.of(102L));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<EmployeeReportingMappingEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(mappingRepository).saveAll(captor.capture());
        assertEquals("PM", captor.getValue().get(0).getManagerType());
        assertEquals(60L, captor.getValue().get(0).getManagerEmployeeId());
    }

    @Test
    void cooCanMapHodDirectlyWithoutProject() {
        User coo = userWithRole(8L, "COO", "ROLE_COO");
        EmployeeEntity hod = employee(9L, "HOD Manager", "EMP009", "INTERNAL", "ACTIVE");
        when(userRepository.findById(8L)).thenReturn(Optional.of(coo));
        when(employeeRepository.findAllById(any())).thenReturn(List.of(hod));
        when(employeeRepository.findActiveEmployeesByRoleNameOrDesignationNames(
                "ROLE_HOD",
                Set.of("HOD", "HEAD OF DEPARTMENT", "HEAD OF DEPARTMENT (HOD)"),
                "%HEAD%OF%DEPARTMENT%"))
                .thenReturn(List.of(hod));
        when(mappingRepository.findByEmployeeIdIn(List.of(9L))).thenReturn(List.of());

        service.saveMapping(8L, "HOD", null, 12L, List.of(9L));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<EmployeeReportingMappingEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(mappingRepository).saveAll(captor.capture());
        EmployeeReportingMappingEntity mapping = captor.getValue().get(0);
        assertEquals(8L, mapping.getHodUserId());
        assertEquals("HOD", mapping.getManagerType());
        assertEquals(9L, mapping.getEmployeeId());
        assertNull(mapping.getManagerEmployeeId());
        assertNull(mapping.getProjectId());
    }

    @Test
    void hodCannotUseHodManagerType() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user(7L, "HOD")));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.saveMapping(7L, "HOD", 9L, null, List.of(103L)));

        assertEquals("HOD manager mapping is available only when COO is selected.", exception.getMessage());
        verify(mappingRepository, never()).saveAll(anyList());
    }

    @Test
    void duplicateEmployeeRejectsEntireNewMappingBatch() {
        EmployeeEntity first = employee(101L, "Alpha", "EMP101", "INTERNAL", "ACTIVE");
        EmployeeEntity second = employee(102L, "Beta", "EMP102", "INTERNAL", "ACTIVE");
        stubValidHodAndEmployees(7L, first, second);
        when(employeeRepository.findByUser_Id(7L)).thenReturn(Optional.empty());
        EmployeeReportingMappingEntity existing = new EmployeeReportingMappingEntity();
        existing.setEmployeeId(102L);
        when(mappingRepository.findByEmployeeIdIn(List.of(101L, 102L))).thenReturn(List.of(existing));

        assertThrows(IllegalStateException.class,
                () -> service.saveMapping(7L, "OTHER", null, null, List.of(101L, 102L)));

        verify(mappingRepository, never()).saveAll(anyList());
    }

    @ParameterizedTest
    @ValueSource(strings = {"OTHER", "STM", "PM"})
    void assigningManagerReusesDirectAuthorityMappingAndCreatesOnlyNewEmployeeMappings(String managerType) {
        EmployeeEntity manager = employee(50L, "Manager", "EMP050", "INTERNAL", "ACTIVE");
        EmployeeEntity directReport = employee(101L, "Alpha", "EMP101", "INTERNAL", "ACTIVE");
        EmployeeEntity unassigned = employee(102L, "Beta", "EMP102", "INTERNAL", "ACTIVE");
        EmployeeReportingMappingEntity existing = reportingMapping(900L, 101L, 7L, "OTHER", null);
        stubValidHodAndEmployees(7L, directReport, unassigned);
        when(employeeRepository.findById(50L)).thenReturn(Optional.of(manager));
        when(mappingRepository.findByEmployeeIdIn(List.of(101L, 102L))).thenReturn(List.of(existing));

        service.saveMapping(7L, managerType, 50L, null, List.of(101L, 102L));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<EmployeeReportingMappingEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(mappingRepository).saveAll(captor.capture());
        List<EmployeeReportingMappingEntity> saved = captor.getValue();
        assertEquals(2, saved.size());
        assertSame(existing, saved.get(0));
        assertEquals(900L, saved.get(0).getMappingId());
        assertNull(saved.get(1).getMappingId());
        assertEquals(List.of(101L, 102L), saved.stream().map(EmployeeReportingMappingEntity::getEmployeeId).toList());
        assertTrue(saved.stream().allMatch(mapping -> Long.valueOf(7L).equals(mapping.getHodUserId())
                && Long.valueOf(50L).equals(mapping.getManagerEmployeeId())
                && managerType.equals(mapping.getManagerType())));
    }

    @Test
    void assigningManagerDoesNotMoveEmployeeFromAnotherAuthority() {
        EmployeeEntity manager = employee(50L, "Manager", "EMP050", "INTERNAL", "ACTIVE");
        EmployeeEntity employee = employee(101L, "Alpha", "EMP101", "INTERNAL", "ACTIVE");
        EmployeeReportingMappingEntity existing = reportingMapping(900L, 101L, 8L, "OTHER", null);
        stubValidHodAndEmployees(7L, employee);
        when(employeeRepository.findById(50L)).thenReturn(Optional.of(manager));
        when(mappingRepository.findByEmployeeIdIn(List.of(101L))).thenReturn(List.of(existing));

        assertThrows(IllegalStateException.class,
                () -> service.saveMapping(7L, "OTHER", 50L, null, List.of(101L)));

        assertEquals(8L, existing.getHodUserId());
        assertNull(existing.getManagerEmployeeId());
        verify(mappingRepository, never()).saveAll(anyList());
    }

    @Test
    void assigningManagerRejectsEntireBatchIfAnyEmployeeAlreadyHasManager() {
        EmployeeEntity manager = employee(50L, "Manager", "EMP050", "INTERNAL", "ACTIVE");
        EmployeeEntity directReport = employee(101L, "Alpha", "EMP101", "INTERNAL", "ACTIVE");
        EmployeeEntity managed = employee(102L, "Beta", "EMP102", "INTERNAL", "ACTIVE");
        EmployeeReportingMappingEntity directMapping = reportingMapping(900L, 101L, 7L, "OTHER", null);
        EmployeeReportingMappingEntity managedMapping = reportingMapping(901L, 102L, 7L, "OTHER", 60L);
        stubValidHodAndEmployees(7L, directReport, managed);
        when(employeeRepository.findById(50L)).thenReturn(Optional.of(manager));
        when(mappingRepository.findByEmployeeIdIn(List.of(101L, 102L)))
                .thenReturn(List.of(directMapping, managedMapping));

        assertThrows(IllegalStateException.class,
                () -> service.saveMapping(7L, "OTHER", 50L, null, List.of(101L, 102L)));

        assertNull(directMapping.getManagerEmployeeId());
        assertEquals(60L, managedMapping.getManagerEmployeeId());
        verify(mappingRepository, never()).saveAll(anyList());
    }

    @Test
    void inactiveEmployeeCannotBeMapped() {
        EmployeeEntity employee = employee(101L, "Inactive", "EMP101", "INTERNAL", "INACTIVE");
        stubValidHodAndEmployees(7L, employee);
        when(employeeRepository.findByUser_Id(7L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.saveMapping(7L, "OTHER", null, null, List.of(101L)));
        verify(mappingRepository, never()).saveAll(anyList());
    }

    @Test
    void externalEmployeeCannotBeMapped() {
        EmployeeEntity employee = employee(101L, "External", "EMP101", "EXTERNAL", "ACTIVE");
        stubValidHodAndEmployees(7L, employee);
        when(employeeRepository.findByUser_Id(7L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.saveMapping(7L, "OTHER", null, null, List.of(101L)));
        verify(mappingRepository, never()).saveAll(anyList());
    }

    @Test
    void otherMappingDisplaysHodAsDirectReportingAuthority() {
        EmployeeReportingMappingEntity mapping = new EmployeeReportingMappingEntity();
        mapping.setMappingId(1L);
        mapping.setEmployeeId(101L);
        mapping.setHodUserId(7L);
        mapping.setManagerType("OTHER");
        mapping.setManagerEmployeeId(null);
        when(mappingRepository.findAll()).thenReturn(List.of(mapping));
        when(employeeRepository.findAll())
                .thenReturn(List.of(employee(101L, "Rahul Patil", "EMP101", "INTERNAL", "ACTIVE")));
        when(userRepository.findAll()).thenReturn(List.of(user(7L, "Anita Deshmukh")));
        when(projectRepository.findAll()).thenReturn(List.of());

        Map<String, Object> result = service.getAllMappings().get(0);

        assertEquals("Directly Reports to HOD - Anita Deshmukh", result.get("managerName"));
        assertNull(result.get("managerEmployeeId"));
    }

    @Test
    void designationBasedStmMappingDisplaysStmAsTheAuthorityType() {
        EmployeeReportingMappingEntity mapping = new EmployeeReportingMappingEntity();
        mapping.setMappingId(1L);
        mapping.setEmployeeId(101L);
        mapping.setHodUserId(9L);
        mapping.setManagerType("STM");
        mapping.setManagerEmployeeId(null);
        when(mappingRepository.findAll()).thenReturn(List.of(mapping));
        when(employeeRepository.findAll())
                .thenReturn(List.of(employee(101L, "Rahul Patil", "EMP101", "INTERNAL", "ACTIVE")));
        when(userRepository.findAll())
                .thenReturn(List.of(userWithRole(9L, "STM Manager", "ROLE_EMPLOYEE")));
        when(projectRepository.findAll()).thenReturn(List.of());

        Map<String, Object> result = service.getAllMappings().get(0);

        assertEquals("STM", result.get("authorityType"));
        assertEquals("Directly Reports to STM - STM Manager", result.get("managerName"));
    }

    @Test
    void otherDirectReportingResolverReturnsHodUserIdWithNullManager() {
        EmployeeReportingMappingEntity mapping = new EmployeeReportingMappingEntity();
        mapping.setManagerType("OTHER");
        mapping.setHodUserId(7L);
        mapping.setManagerEmployeeId(null);

        assertEquals(7L, service.resolveDirectReportingUserId(mapping));
    }

    @Test
    void changeReportingAuthorityUpdatesExistingMappingAsDirectReport() {
        EmployeeEntity employee = employee(101L, "Rahul Patil", "EMP101", "INTERNAL", "ACTIVE");
        EmployeeReportingMappingEntity existing = reportingMapping(900L, 101L, 7L, "PM", 60L);
        when(userRepository.findById(8L)).thenReturn(Optional.of(user(8L, "New HOD")));
        when(employeeRepository.findAllById(Set.of(101L))).thenReturn(List.of(employee));
        when(employeeRepository.findByUser_Id(8L)).thenReturn(Optional.empty());
        when(mappingRepository.findFirstByEmployeeIdOrderByMappingIdDesc(101L))
                .thenReturn(Optional.of(existing));

        service.changeReportingAuthority(101L, 8L, null, null);

        verify(mappingRepository).save(existing);
        assertEquals(8L, existing.getHodUserId());
        assertEquals("OTHER", existing.getManagerType());
        assertNull(existing.getManagerEmployeeId());
        assertNull(existing.getProjectId());
    }

    @Test
    void changeReportingAuthorityRejectsSelfReporting() {
        User employeeUser = user(8L, "Employee Manager");
        EmployeeEntity employee = employee(101L, "Employee Manager", "EMP101", "INTERNAL", "ACTIVE");
        employee.setUser(employeeUser);
        when(userRepository.findById(8L)).thenReturn(Optional.of(employeeUser));
        when(employeeRepository.findAllById(Set.of(101L))).thenReturn(List.of(employee));
        when(employeeRepository.findByUser_Id(8L)).thenReturn(Optional.of(employee));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.changeReportingAuthority(101L, 8L, null, null));

        assertEquals("The selected reporting authority cannot be mapped as their own report.",
                exception.getMessage());
        verify(mappingRepository, never()).save(any());
    }

    @Test
    void changeReportingAuthorityCanAlsoChangeManagerTypeAndManager() {
        EmployeeEntity employee = employee(101L, "Rahul Patil", "EMP101", "INTERNAL", "ACTIVE");
        EmployeeEntity manager = employee(50L, "Project Manager", "EMP050", "INTERNAL", "ACTIVE");
        EmployeeReportingMappingEntity existing = reportingMapping(900L, 101L, 7L, "STM", 40L);
        when(userRepository.findById(8L)).thenReturn(Optional.of(user(8L, "New HOD")));
        when(employeeRepository.findById(50L)).thenReturn(Optional.of(manager));
        when(employeeRepository.findAllById(Set.of(101L))).thenReturn(List.of(employee));
        when(mappingRepository.findFirstByEmployeeIdOrderByMappingIdDesc(101L))
                .thenReturn(Optional.of(existing));

        service.changeReportingAuthority(101L, 8L, "PM", 50L);

        verify(mappingRepository).save(existing);
        assertEquals(8L, existing.getHodUserId());
        assertEquals("PM", existing.getManagerType());
        assertEquals(50L, existing.getManagerEmployeeId());
    }

    @Test
    void reportingAssignmentsIncludeCellEmployeesAndIdentifyManagers() {
        EmployeeEntity manager = employee(50L, "Project Manager", "EMP050", "INTERNAL", "ACTIVE");
        EmployeeEntity employee = employee(101L, "Managed Employee", "EMP101", "INTERNAL", "ACTIVE");
        EmployeeEntity cellEmployee = employee(102L, "Cell Employee", "EMP102", "INTERNAL", "ACTIVE");
        EmployeeReportingMappingEntity managerMapping = reportingMapping(900L, 50L, 7L, "PM", null);
        EmployeeReportingMappingEntity employeeMapping = reportingMapping(901L, 101L, 7L, "PM", 50L);
        CellMaster cell = CellMaster.builder().cellId(11L).cellName("Applications").build();
        EmployeeCellMappingEntity employeeCell = new EmployeeCellMappingEntity();
        employeeCell.setEmployee(cellEmployee);
        employeeCell.setCell(cell);
        CellReportingAuthorityMappingEntity cellAuthority = new CellReportingAuthorityMappingEntity();
        cellAuthority.setCell(cell);
        cellAuthority.setAuthorityUserId(8L);
        when(employeeRepository.findByRecruitmentTypeIgnoreCaseAndStatusIgnoreCaseOrderByFullNameAscEmployeeIdAsc(
                "INTERNAL", "ACTIVE")).thenReturn(List.of(manager, employee, cellEmployee));
        when(mappingRepository.findByEmployeeIdIn(Set.of(50L, 101L, 102L)))
                .thenReturn(List.of(managerMapping, employeeMapping));
        when(employeeCellMappingRepository.findByEmployeeEmployeeIdInOrderByEmployeeEmployeeIdAsc(
                Set.of(50L, 101L, 102L))).thenReturn(List.of(employeeCell));
        when(cellAuthorityMappingRepository.findByCellCellIdIn(Set.of(11L)))
                .thenReturn(List.of(cellAuthority));
        when(userRepository.findAllById(Set.of(7L, 8L)))
                .thenReturn(List.of(user(7L, "HOD Anita"), user(8L, "HOD Ravi")));

        List<Map<String, Object>> assignments = service.getEmployeeReportingAssignments();

        assertEquals(3, assignments.size());
        assertEquals(true, assignments.get(0).get("reportingManager"));
        assertEquals("EMPLOYEE", assignments.get(0).get("mappingSource"));
        assertEquals("Project Manager (EMP050)", assignments.get(1).get("managerName"));
        assertEquals("HOD Anita", assignments.get(1).get("authorityName"));
        assertEquals("CELL", assignments.get(2).get("mappingSource"));
        assertEquals("Applications", assignments.get(2).get("cellName"));
        assertEquals("HOD Ravi", assignments.get(2).get("authorityName"));
    }

    @Test
    void effectiveAuthorityEmployeesIncludeCellFallbackWithoutReplacingExplicitMappings() {
        when(employeeRepository.findByUser_Id(7L)).thenReturn(Optional.empty());
        when(mappingRepository.findEmployeeIdsByAuthorityIdentity(7L, null)).thenReturn(List.of(101L, 102L));
        when(cellAuthorityMappingRepository.findCellIdsByAuthorityUserId(7L)).thenReturn(List.of(11L));
        when(employeeCellMappingRepository.findEmployeeIdsWithoutExplicitReportingMapping(List.of(11L), 7L))
                .thenReturn(List.of(103L, 104L));

        List<Long> employeeIds = service.getEffectiveEmployeeIdsForAuthority(7L);

        assertEquals(List.of(101L, 102L, 103L, 104L), employeeIds);
    }

    @Test
    void effectiveAuthorityEmployeesIncludeLegacyDirectReportsByManagerEmployeeId() {
        EmployeeEntity manager = employee(50L, "Project Manager", "EMP050", "INTERNAL", "ACTIVE");
        when(employeeRepository.findByUser_Id(7L)).thenReturn(Optional.of(manager));
        when(mappingRepository.findEmployeeIdsByAuthorityIdentity(7L, 50L)).thenReturn(List.of(101L));
        when(cellAuthorityMappingRepository.findCellIdsByAuthorityUserId(7L)).thenReturn(List.of());

        List<Long> employeeIds = service.getEffectiveEmployeeIdsForAuthority(7L);

        assertEquals(List.of(101L), employeeIds);
    }

    @Test
    void saveCellReportingMappingCreatesFallbackForActiveCellAndAuthority() {
        WingMaster wing = WingMaster.builder()
                .wingId(3L)
                .wingName("Technology")
                .activeFlag("Y")
                .build();
        CellMaster cell = CellMaster.builder()
                .cellId(11L)
                .cellName("Applications")
                .wing(wing)
                .activeFlag("Y")
                .build();
        when(cellMasterRepository.findByCellId(11L)).thenReturn(Optional.of(cell));
        when(userRepository.findByIdAndActiveTrue(7L)).thenReturn(Optional.of(user(7L, "HOD")));
        when(cellAuthorityMappingRepository.findByCellCellIdOrderByAuthorityLevelAsc(11L))
                .thenReturn(List.of());
        when(cellAuthorityMappingRepository.findFirstByCellCellIdAndAuthorityLevelOrderByMappingIdAsc(11L, 1))
                .thenReturn(Optional.empty());

        service.saveCellReportingMapping(null, 11L, 1, 7L);

        ArgumentCaptor<CellReportingAuthorityMappingEntity> captor =
                ArgumentCaptor.forClass(CellReportingAuthorityMappingEntity.class);
        verify(cellAuthorityMappingRepository).save(captor.capture());
        assertEquals(11L, captor.getValue().getCell().getCellId());
        assertEquals(7L, captor.getValue().getAuthorityUserId());
        assertEquals(1, captor.getValue().getAuthorityLevel());
    }

    @Test
    void saveCellReportingMappingAllowsLevelTwoWithoutLevelOne() {
        WingMaster wing = WingMaster.builder().wingId(3L).activeFlag("Y").build();
        CellMaster cell = CellMaster.builder()
                .cellId(11L)
                .cellName("Applications")
                .wing(wing)
                .activeFlag("Y")
                .build();
        CellReportingAuthorityMappingEntity existingLevelTwo = new CellReportingAuthorityMappingEntity();
        existingLevelTwo.setMappingId(32L);
        existingLevelTwo.setCell(cell);
        existingLevelTwo.setAuthorityLevel(2);
        existingLevelTwo.setAuthorityUserId(8L);
        when(cellMasterRepository.findByCellId(11L)).thenReturn(Optional.of(cell));
        when(userRepository.findByIdAndActiveTrue(9L)).thenReturn(Optional.of(user(9L, "Second L2")));
        when(cellAuthorityMappingRepository.findByCellCellIdOrderByAuthorityLevelAsc(11L))
                .thenReturn(List.of(existingLevelTwo));

        service.saveCellReportingMapping(null, 11L, 2, 9L);

        ArgumentCaptor<CellReportingAuthorityMappingEntity> captor =
                ArgumentCaptor.forClass(CellReportingAuthorityMappingEntity.class);
        verify(cellAuthorityMappingRepository).save(captor.capture());
        assertNull(captor.getValue().getMappingId());
        assertEquals(2, captor.getValue().getAuthorityLevel());
        assertEquals(9L, captor.getValue().getAuthorityUserId());
    }

    @Test
    void managerCategoryDirectReportingResolverReturnsAuthorityUserId() {
        EmployeeReportingMappingEntity mapping = new EmployeeReportingMappingEntity();
        mapping.setManagerType("PM");
        mapping.setHodUserId(7L);
        mapping.setManagerEmployeeId(null);

        assertEquals(7L, service.resolveDirectReportingUserId(mapping));
    }

    @Test
    void otherEmployeeListExcludesMappedEmployeesAndSelectedHodEmployee() {
        EmployeeEntity hodEmployee = employee(5L, "HOD Employee", "EMP005", "INTERNAL", "ACTIVE");
        EmployeeEntity available = employee(10L, "Asha", "EMP010", "INTERNAL", "ACTIVE");
        EmployeeEntity mapped = employee(11L, "Zara", "EMP011", "INTERNAL", "ACTIVE");
        EmployeeReportingMappingEntity existing = new EmployeeReportingMappingEntity();
        existing.setEmployeeId(11L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user(7L, "HOD")));
        when(employeeRepository.findByUser_Id(7L)).thenReturn(Optional.of(hodEmployee));
        when(mappingRepository.findByEmployeeIdIn(Set.of(5L, 10L, 11L))).thenReturn(List.of(existing));
        when(employeeRepository.findByRecruitmentTypeIgnoreCaseAndStatusIgnoreCaseOrderByFullNameAscEmployeeIdAsc(
                "INTERNAL", "ACTIVE")).thenReturn(List.of(available, hodEmployee, mapped));

        List<Map<String, Object>> result = service.getInternalEmployees(null, 7L, "OTHER", null);

        assertEquals(List.of(10L), result.stream().map(row -> (Long) row.get("id")).toList());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "OTHER", "STM", "PM"})
    void employeesWithoutManagerRemainVisibleUnderTheirAuthority(String storedManagerType) {
        EmployeeEntity authority = employee(5L, "Authority", "EMP005", "INTERNAL", "ACTIVE");
        EmployeeEntity unassigned = employee(10L, "Alpha", "EMP010", "INTERNAL", "ACTIVE");
        EmployeeEntity directReport = employee(11L, "Beta", "EMP011", "INTERNAL", "ACTIVE");
        EmployeeEntity elsewhere = employee(12L, "Elsewhere", "EMP012", "INTERNAL", "ACTIVE");
        EmployeeEntity managed = employee(13L, "Managed", "EMP013", "INTERNAL", "ACTIVE");
        EmployeeEntity manager = employee(50L, "Manager", "EMP050", "INTERNAL", "ACTIVE");
        when(userRepository.findById(7L)).thenReturn(Optional.of(user(7L, "HOD")));
        when(employeeRepository.findByUser_Id(7L)).thenReturn(Optional.of(authority));
        when(employeeRepository.findByRecruitmentTypeIgnoreCaseAndStatusIgnoreCaseOrderByFullNameAscEmployeeIdAsc(
                "INTERNAL", "ACTIVE")).thenReturn(List.of(authority, unassigned, directReport, elsewhere, managed, manager));
        when(mappingRepository.findByEmployeeIdIn(Set.of(5L, 10L, 11L, 12L, 13L, 50L))).thenReturn(List.of(
                reportingMapping(900L, 11L, 7L, storedManagerType, null),
                reportingMapping(901L, 12L, 8L, "OTHER", null),
                reportingMapping(902L, 13L, 7L, "OTHER", 60L)));

        List<Map<String, Object>> withoutManager = service.getInternalEmployees(null, 7L, null, null);
        List<Map<String, Object>> withManager = service.getInternalEmployees(null, 7L, "OTHER", 50L);

        assertEquals(List.of(10L, 11L, 50L), withoutManager.stream().map(row -> (Long) row.get("id")).toList());
        assertEquals(true, withoutManager.get(1).get("directAuthorityReport"));
        assertEquals(false, withoutManager.get(1).get("canAssignManager"));
        assertEquals(List.of(10L, 11L), withManager.stream().map(row -> (Long) row.get("id")).toList());
        assertEquals(false, withManager.get(0).get("mapped"));
        assertEquals(true, withManager.get(1).get("mapped"));
        assertEquals(true, withManager.get(1).get("canAssignManager"));
        verify(mappingRepository, never()).findAll();
    }

    @Test
    void editingStillIncludesEmployeeWithAnAssignedManager() {
        EmployeeEntity employee = employee(11L, "Beta", "EMP011", "INTERNAL", "ACTIVE");
        when(userRepository.findById(7L)).thenReturn(Optional.of(user(7L, "HOD")));
        when(employeeRepository.findByRecruitmentTypeIgnoreCaseAndStatusIgnoreCaseOrderByFullNameAscEmployeeIdAsc(
                "INTERNAL", "ACTIVE")).thenReturn(List.of(employee));
        when(mappingRepository.findByEmployeeIdIn(Set.of(11L)))
                .thenReturn(List.of(reportingMapping(900L, 11L, 7L, "OTHER", 60L)));

        List<Map<String, Object>> result = service.getInternalEmployees(11L, 7L, "OTHER", 50L);

        assertEquals(List.of(11L), result.stream().map(row -> (Long) row.get("id")).toList());
        assertEquals(60L, result.get(0).get("mappedManagerEmployeeId"));
    }

    @Test
    void getManagersByTypeRejectsUnknownType() {
        assertThrows(IllegalArgumentException.class, () -> service.getManagersByType("UNKNOWN"));
    }

    private void stubValidHodAndEmployees(Long hodUserId, EmployeeEntity... employees) {
        when(userRepository.findById(hodUserId)).thenReturn(Optional.of(user(hodUserId, "HOD")));
        when(employeeRepository.findAllById(any())).thenReturn(List.of(employees));
    }

    private EmployeeReportingMappingEntity reportingMapping(
            Long mappingId, Long employeeId, Long authorityUserId, String managerType, Long managerEmployeeId) {
        EmployeeReportingMappingEntity mapping = new EmployeeReportingMappingEntity();
        mapping.setMappingId(mappingId);
        mapping.setEmployeeId(employeeId);
        mapping.setHodUserId(authorityUserId);
        mapping.setManagerType(managerType);
        mapping.setManagerEmployeeId(managerEmployeeId);
        return mapping;
    }

    private User user(Long id, String name) {
        return userWithRole(id, name, "ROLE_HOD");
    }

    private User userWithRole(Long id, String name, String roleName) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        Role role = new Role();
        role.setName(roleName);
        user.addRole(role);
        return user;
    }

    private ProjectMst project(Long id, String name) {
        ProjectMst project = new ProjectMst();
        project.setProjectId(id);
        project.setProjectName(name);
        project.setActiveFlag("Y");
        return project;
    }

    private EmployeeReportingHodProjection mappingHod(Long employeeId, Long hodUserId, Long mappingId) {
        return new EmployeeReportingHodProjection() {
            @Override
            public Long getEmployeeId() {
                return employeeId;
            }

            @Override
            public Long getHodUserId() {
                return hodUserId;
            }

            @Override
            public Long getMappingId() {
                return mappingId;
            }
        };
    }

    private EmployeeEntity employee(
            Long id,
            String name,
            String code,
            String recruitmentType,
            String status) {
        EmployeeEntity employee = new EmployeeEntity();
        employee.setEmployeeId(id);
        employee.setFullName(name);
        employee.setEmployeeCode(code);
        employee.setRecruitmentType(recruitmentType);
        employee.setStatus(status);
        return employee;
    }
}
