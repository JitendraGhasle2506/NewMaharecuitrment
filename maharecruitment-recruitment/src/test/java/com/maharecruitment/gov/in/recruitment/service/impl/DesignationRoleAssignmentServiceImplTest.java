package com.maharecruitment.gov.in.recruitment.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maharecruitment.gov.in.auth.entity.Role;
import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.RoleRepository;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.master.entity.ManpowerDesignationMaster;
import com.maharecruitment.gov.in.master.repository.ManpowerDesignationMasterRepository;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.recruitment.service.model.DesignationRoleAssignmentResult;
import com.maharecruitment.gov.in.recruitment.service.model.DesignationRoleAssignmentView;

@ExtendWith(MockitoExtension.class)
class DesignationRoleAssignmentServiceImplTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private ManpowerDesignationMasterRepository designationRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRepository userRepository;

    private DesignationRoleAssignmentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DesignationRoleAssignmentServiceImpl(
                employeeRepository,
                designationRepository,
                roleRepository,
                userRepository);
    }

    @Test
    void getAssignmentsShowsConfiguredRoleAndPendingEmployee() {
        Role employeeRole = role(1L, "ROLE_EMPLOYEE");
        Role stmRole = role(2L, "ROLE_STM");
        ManpowerDesignationMaster designation = designation(10L, "Senior Technical Manager", "ROLE_STM");
        EmployeeEntity employee = employee(100L, "EMP100", "Sanjay Patil", designation,
                user(7L, true, employeeRole));
        when(designationRepository.findByActiveFlagIgnoreCaseOrderByDesignationNameAsc("Y"))
                .thenReturn(List.of(designation));
        when(employeeRepository.findActiveEmployeesForDesignationRoleAssignment("ACTIVE", "Y"))
                .thenReturn(List.of(employee));
        when(roleRepository.findAllByOrderByNameAsc()).thenReturn(List.of(employeeRole, stmRole));

        DesignationRoleAssignmentView result = service.getAssignments(null).get(0);

        assertEquals("ROLE_STM", result.configuredRoleName());
        assertTrue(result.configuredRoleAvailable());
        assertEquals(1, result.activeEmployeeCount());
        assertEquals(1, result.pendingUserCount());
        assertFalse(result.employees().get(0).configuredRoleAssigned());
    }

    @Test
    void configureAndAssignAddsRoleWithoutRemovingExistingRoles() {
        Role employeeRole = role(1L, "ROLE_EMPLOYEE");
        Role stmRole = role(2L, "ROLE_STM");
        ManpowerDesignationMaster designation = designation(10L, "Senior Technical Manager", null);
        User linkedUser = user(7L, true, employeeRole);
        EmployeeEntity linkedEmployee = employee(100L, "EMP100", "Sanjay Patil", designation, linkedUser);
        EmployeeEntity unlinkedEmployee = employee(101L, "EMP101", "Unlinked User", designation, null);
        when(designationRepository.findByDesignationIdAndActiveFlagIgnoreCase(10L, "Y"))
                .thenReturn(Optional.of(designation));
        when(roleRepository.findByNameIgnoreCase("ROLE_STM")).thenReturn(Optional.of(stmRole));
        when(employeeRepository.findByDesignation_DesignationIdAndStatusIgnoreCaseOrderByFullNameAscEmployeeIdAsc(
                10L,
                "ACTIVE"))
                .thenReturn(List.of(linkedEmployee, unlinkedEmployee));

        DesignationRoleAssignmentResult result = service.configureAndAssign(10L, "STM");

        assertEquals("ROLE_STM", designation.getRoleName());
        assertEquals(List.of("ROLE_EMPLOYEE", "ROLE_STM"),
                linkedUser.getRoles().stream().map(Role::getName).toList());
        assertEquals(1, result.assignedUsers());
        assertEquals(1, result.missingUserAccounts());
        verify(designationRepository).save(designation);
        verify(userRepository).saveAll(List.of(linkedUser));
    }

    @Test
    void getAssignmentsMarksUnavailableDesignationRoleForCorrection() {
        ManpowerDesignationMaster designation = designation(10L, "Legacy Designation", "ROLE_UNKNOWN");
        when(designationRepository.findByActiveFlagIgnoreCaseOrderByDesignationNameAsc("Y"))
                .thenReturn(List.of(designation));
        when(employeeRepository.findActiveEmployeesForDesignationRoleAssignment("ACTIVE", "Y"))
                .thenReturn(List.of());
        when(roleRepository.findAllByOrderByNameAsc()).thenReturn(List.of(role(1L, "ROLE_EMPLOYEE")));

        DesignationRoleAssignmentView result = service.getAssignments(null).get(0);

        assertEquals("ROLE_UNKNOWN", result.configuredRoleName());
        assertFalse(result.configuredRoleAvailable());
    }

    private ManpowerDesignationMaster designation(Long id, String name, String roleName) {
        return ManpowerDesignationMaster.builder()
                .designationId(id)
                .designationName(name)
                .category("Management")
                .roleName(roleName)
                .activeFlag("Y")
                .build();
    }

    private EmployeeEntity employee(
            Long id,
            String code,
            String name,
            ManpowerDesignationMaster designation,
            User user) {
        EmployeeEntity employee = new EmployeeEntity();
        employee.setEmployeeId(id);
        employee.setEmployeeCode(code);
        employee.setFullName(name);
        employee.setStatus("ACTIVE");
        employee.setDesignation(designation);
        employee.setUser(user);
        return employee;
    }

    private User user(Long id, boolean active, Role... roles) {
        User user = new User();
        user.setId(id);
        user.setName("User " + id);
        user.setActive(active);
        user.setRoles(List.of(roles));
        return user;
    }

    private Role role(Long id, String name) {
        Role role = new Role();
        role.setId(id);
        role.setName(name);
        return role;
    }
}
