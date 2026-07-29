package com.maharecruitment.gov.in.recruitment.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.master.entity.ProjectMst;
import com.maharecruitment.gov.in.master.repository.ProjectMstRepository;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeReportingMappingEntity;
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

    @InjectMocks
    private ReportingManagerServiceImpl service;

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
    void getManagersByTypePmIncludesFilledProjectManagerPositionEmployee() {
        EmployeeEntity employee = employee(21L, "Prakash More", "EMP21", "INTERNAL", "ACTIVE");
        when(positionRepository.findFilledActiveEmployeesByManagerNames(
                Set.of("PM", "PROJECT MANAGER", "PROJECT MANAGER (PM)"),
                "%PROJECT%MANAGER%",
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
        when(employeeRepository.findActiveEmployeesNotMappedAsStmOrPmManagers())
                .thenReturn(List.of(employee));

        List<Map<String, Object>> result = service.getManagersByType("OTHER");

        assertEquals(1, result.size());
        assertEquals(30L, result.get(0).get("id"));
    }

    @Test
    void otherDoesNotRequireManagerAndStoresDirectHodMapping() {
        EmployeeEntity employee = employee(101L, "Rahul Patil", "EMP101", "INTERNAL", "ACTIVE");
        stubValidHodAndEmployees(7L, employee);
        when(employeeRepository.findByUser_Id(7L)).thenReturn(Optional.empty());
        when(mappingRepository.findByEmployeeIdIn(List.of(101L))).thenReturn(List.of());

        service.saveMapping(7L, "other", null, 99L, List.of(101L));

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
    void stmRequiresManagerEmployeeId() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user(7L, "HOD")));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.saveMapping(7L, "STM", null, null, List.of(101L)));

        assertEquals("Manager selection is required.", exception.getMessage());
    }

    @Test
    void pmRequiresManagerEmployeeId() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user(7L, "HOD")));

        assertThrows(IllegalArgumentException.class,
                () -> service.saveMapping(7L, "PM", null, null, List.of(101L)));
    }

    @Test
    void stmFlowContinuesToStoreSelectedManager() {
        EmployeeEntity manager = employee(50L, "STM Manager", "EMP050", "INTERNAL", "ACTIVE");
        EmployeeEntity employee = employee(101L, "Rahul Patil", "EMP101", "INTERNAL", "ACTIVE");
        stubValidHodAndEmployees(7L, employee);
        when(employeeRepository.findById(50L)).thenReturn(Optional.of(manager));
        when(projectRepository.findById(12L)).thenReturn(Optional.of(project(12L, "Internal Project")));
        when(mappingRepository.findByEmployeeIdIn(List.of(101L))).thenReturn(List.of());

        service.saveMapping(7L, "STM", 50L, 12L, List.of(101L));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<EmployeeReportingMappingEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(mappingRepository).saveAll(captor.capture());
        EmployeeReportingMappingEntity mapping = captor.getValue().get(0);
        assertEquals("STM", mapping.getManagerType());
        assertEquals(50L, mapping.getManagerEmployeeId());
        assertEquals(12L, mapping.getProjectId());
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
    void otherDirectReportingResolverReturnsHodUserIdWithNullManager() {
        EmployeeReportingMappingEntity mapping = new EmployeeReportingMappingEntity();
        mapping.setManagerType("OTHER");
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
        when(mappingRepository.findAll()).thenReturn(List.of(existing));
        when(employeeRepository.findByRecruitmentTypeIgnoreCaseAndStatusIgnoreCaseOrderByFullNameAscEmployeeIdAsc(
                "INTERNAL", "ACTIVE")).thenReturn(List.of(available, hodEmployee, mapped));

        List<Map<String, Object>> result = service.getInternalEmployees(null, 7L, "OTHER");

        assertEquals(List.of(10L), result.stream().map(row -> (Long) row.get("id")).toList());
    }

    @Test
    void getManagersByTypeRejectsUnknownType() {
        assertThrows(IllegalArgumentException.class, () -> service.getManagersByType("UNKNOWN"));
    }

    private void stubValidHodAndEmployees(Long hodUserId, EmployeeEntity... employees) {
        when(userRepository.findById(hodUserId)).thenReturn(Optional.of(user(hodUserId, "HOD")));
        when(employeeRepository.findAllById(any())).thenReturn(List.of(employees));
    }

    private User user(Long id, String name) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        return user;
    }

    private ProjectMst project(Long id, String name) {
        ProjectMst project = new ProjectMst();
        project.setProjectId(id);
        project.setProjectName(name);
        project.setActiveFlag("Y");
        return project;
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
