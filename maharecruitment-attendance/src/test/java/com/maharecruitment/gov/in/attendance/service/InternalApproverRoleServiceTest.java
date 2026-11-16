package com.maharecruitment.gov.in.attendance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import com.maharecruitment.gov.in.attendance.entity.InternalApproverRoleAuditLogEntity;
import com.maharecruitment.gov.in.attendance.entity.InternalApproverRoleEntity;
import com.maharecruitment.gov.in.attendance.repository.AttendanceRequestAuditLogRepository;
import com.maharecruitment.gov.in.attendance.repository.InternalApproverRoleAuditLogRepository;
import com.maharecruitment.gov.in.attendance.repository.InternalApproverRoleRepository;
import com.maharecruitment.gov.in.attendance.service.InternalApproverRoleService.RoleOption;
import com.maharecruitment.gov.in.auth.entity.Role;
import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.RoleRepository;
import com.maharecruitment.gov.in.auth.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class InternalApproverRoleServiceTest {

    private static final Role HOD = role(17L, "ROLE_HOD");
    private static final Role MD = role(18L, "ROLE_MD");
    private static final Role PM = role(10L, "ROLE_PM");

    @Mock private InternalApproverRoleRepository approverRoleRepository;
    @Mock private InternalApproverRoleAuditLogRepository changeLogRepository;
    @Mock private AttendanceRequestAuditLogRepository requestAuditLogRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private UserRepository userRepository;
    @Captor private ArgumentCaptor<List<InternalApproverRoleEntity>> savedRoles;
    @Captor private ArgumentCaptor<InternalApproverRoleAuditLogEntity> logEntry;

    private InternalApproverRoleService service;

    @BeforeEach
    void setUp() {
        service = new InternalApproverRoleService(approverRoleRepository, changeLogRepository,
                requestAuditLogRepository, roleRepository, userRepository);
    }

    @Test
    void roleOptionsMarkTheCurrentApproverRoles() {
        when(approverRoleRepository.findAll()).thenReturn(List.of(new InternalApproverRoleEntity(17L, null)));
        when(roleRepository.findAllByOrderByNameAsc()).thenReturn(List.of(role(15L, "ROLE_EMPLOYEE"), HOD));

        assertThat(service.getRoleOptions()).containsExactly(
                new RoleOption(15L, "ROLE_EMPLOYEE", "EMPLOYEE", false),
                new RoleOption(17L, "ROLE_HOD", "HOD", true));
    }

    @Test
    void savingAddsAndRemovesRolesAndLogsWhoChangedWhatAndWhy() {
        currentlyConfigured(17L, 18L);
        User admin = new User();
        admin.setId(2L);
        admin.setName("Admin User");
        admin.setEmail("admin@example.test");
        when(userRepository.findByEmailIgnoreCase("admin@example.test")).thenReturn(Optional.of(admin));

        var result = service.updateApproverRoles(List.of(17L, 10L), "  PMs approve internal leave  ",
                "admin@example.test");

        assertThat(result.added()).containsExactly("ROLE_PM");
        assertThat(result.removed()).containsExactly("ROLE_MD");
        verify(approverRoleRepository).deleteAllByIdInBatch(List.of(18L));
        verify(approverRoleRepository).saveAll(savedRoles.capture());
        assertThat(savedRoles.getValue()).singleElement().satisfies(granted -> {
            assertThat(granted.getRoleId()).isEqualTo(10L);
            assertThat(granted.getGrantedByUserId()).isEqualTo(2L);
        });
        verify(changeLogRepository).save(logEntry.capture());
        InternalApproverRoleAuditLogEntity entry = logEntry.getValue();
        assertThat(entry.getActionType()).isEqualTo("UPDATED");
        assertThat(entry.getRolesAdded()).isEqualTo("ROLE_PM");
        assertThat(entry.getRolesRemoved()).isEqualTo("ROLE_MD");
        assertThat(entry.getRolesBefore()).isEqualTo("ROLE_HOD,ROLE_MD");
        assertThat(entry.getRolesAfter()).isEqualTo("ROLE_HOD,ROLE_PM");
        assertThat(entry.getRemarks()).isEqualTo("PMs approve internal leave");
        assertThat(entry.getActorUserId()).isEqualTo(2L);
        assertThat(entry.getActorName()).isEqualTo("Admin User");
        assertThat(entry.getActorLoginId()).isEqualTo("admin@example.test");
    }

    @Test
    void savingTheSameSelectionChangesAndLogsNothing() {
        currentlyConfigured(17L);

        var result = service.updateApproverRoles(List.of(17L), "no change", "admin@example.test");

        assertThat(result.changed()).isFalse();
        verify(approverRoleRepository, never()).deleteAllByIdInBatch(any());
        verify(approverRoleRepository, never()).saveAll(any());
        verifyNoInteractions(changeLogRepository, userRepository);
    }

    @Test
    void emptySelectionIsRejectedBecauseNobodyCouldApprove() {
        assertThatThrownBy(() -> service.updateApproverRoles(List.of(), null, "admin@example.test"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one role");
        assertThatThrownBy(() -> service.updateApproverRoles(null, null, "admin@example.test"))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(approverRoleRepository, changeLogRepository);
    }

    @Test
    void unknownRoleOrOverlongReasonIsRejectedWithoutChanges() {
        when(roleRepository.findAllById(any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.updateApproverRoles(List.of(99L), null, "admin@example.test"))
                .hasMessageContaining("no longer exists");
        assertThatThrownBy(() -> service.updateApproverRoles(List.of(17L), "x".repeat(501), "admin@example.test"))
                .hasMessageContaining("500 characters");
        verifyNoInteractions(approverRoleRepository, changeLogRepository);
    }

    @Test
    void logsArePagedWithACappedPageSize() {
        service.getChangeLog(-1, 500);
        service.getInternalDecisionLog(2, 10);

        verify(changeLogRepository).findAllByOrderByChangedAtDescIdDesc(PageRequest.of(0, 50));
        verify(requestAuditLogRepository).findInternalEmployeeDecisions(PageRequest.of(2, 10));
    }

    private void currentlyConfigured(Long... roleIds) {
        when(approverRoleRepository.findAll()).thenReturn(Stream.of(roleIds)
                .map(id -> new InternalApproverRoleEntity(id, null))
                .toList());
        when(roleRepository.findAllById(any())).thenAnswer(invocation -> {
            Collection<Long> ids = invocation.getArgument(0);
            return Stream.of(HOD, MD, PM).filter(role -> ids.contains(role.getId())).toList();
        });
    }

    private static Role role(Long id, String name) {
        Role role = new Role();
        role.setId(id);
        role.setName(name);
        return role;
    }
}
