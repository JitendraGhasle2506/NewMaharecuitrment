package com.maharecruitment.gov.in.attendance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maharecruitment.gov.in.attendance.repository.InternalApproverRoleRepository;
import com.maharecruitment.gov.in.auth.entity.Role;
import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;

@ExtendWith(MockitoExtension.class)
class InternalEmployeeApprovalPolicyTest {

    @Mock private EmployeeRepository employeeRepository;
    @Mock private UserRepository userRepository;
    @Mock private InternalApproverRoleRepository approverRoleRepository;

    private InternalEmployeeApprovalPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new InternalEmployeeApprovalPolicy(employeeRepository, userRepository, approverRoleRepository);
    }

    @Test
    void approverNeedsOneOfTheRolesSetByTheAdministrator() {
        when(approverRoleRepository.findApproverRoleNames()).thenReturn(List.of("ROLE_HOD", "ROLE_PM"));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user(7L, " role_pm ")));
        when(userRepository.findById(8L)).thenReturn(Optional.of(user(8L, "ROLE_MD")));

        assertThat(policy.hasInternalApproverRole(7L)).isTrue();
        assertThat(policy.hasInternalApproverRole(8L)).isFalse();
        assertThat(policy.hasInternalApproverRole(null)).isFalse();
    }

    @Test
    void aChangeOnTheAdminScreenAppliesToTheNextCheck() {
        when(approverRoleRepository.findApproverRoleNames()).thenReturn(List.of("ROLE_PM"), List.of("ROLE_HOD"));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user(7L, "ROLE_PM")));

        assertThat(policy.hasInternalApproverRole(7L)).isTrue();
        assertThat(policy.hasInternalApproverRole(7L)).isFalse();
    }

    @Test
    void withNoRoleSetNobodyApprovesAndTheReasonSaysSo() {
        when(approverRoleRepository.findApproverRoleNames()).thenReturn(List.of());

        assertThat(policy.hasInternalApproverRole(7L)).isFalse();
        assertThat(policy.viewOnlyReason()).contains("No role is set", "administrator");
        verifyNoInteractions(userRepository);
    }

    @Test
    void labelsUseShortRoleNames() {
        assertThat(InternalEmployeeApprovalPolicy.approverRoleLabel(List.of("ROLE_HOD"))).isEqualTo("HOD");
        assertThat(InternalEmployeeApprovalPolicy.approverRoleLabel(List.of("ROLE_CFO", "ROLE_HOD", "ROLE_PM")))
                .isEqualTo("CFO, HOD or PM");
        assertThat(InternalEmployeeApprovalPolicy.approverRoleLabel(List.of())).isEmpty();
    }

    @Test
    void onlyInternalEmployeesRequestsNeedAConfiguredRole() {
        when(employeeRepository.findById(101L)).thenReturn(Optional.of(employee("INTERNAL")));
        when(employeeRepository.findById(102L)).thenReturn(Optional.of(employee("MAHAIT")));
        when(approverRoleRepository.findApproverRoleNames()).thenReturn(List.of("ROLE_HOD"));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user(7L, "ROLE_EMPLOYEE")));

        assertThatThrownBy(() -> policy.requireCanDecide(7L, 101L, "leave"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot approve or reject")
                .hasMessageContaining("Only HOD can approve internal employee requests.");
        assertThatCode(() -> policy.requireCanDecide(7L, 102L, "leave")).doesNotThrowAnyException();
    }

    private static User user(Long id, String roleName) {
        Role role = new Role();
        role.setName(roleName);
        User user = new User();
        user.setId(id);
        user.setRoles(List.of(role));
        return user;
    }

    private static EmployeeEntity employee(String recruitmentType) {
        EmployeeEntity employee = new EmployeeEntity();
        employee.setRecruitmentType(recruitmentType);
        return employee;
    }
}
