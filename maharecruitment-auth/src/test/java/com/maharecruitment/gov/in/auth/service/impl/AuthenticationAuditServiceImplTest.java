package com.maharecruitment.gov.in.auth.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.maharecruitment.gov.in.auth.entity.LoginLogoutAuditEventType;
import com.maharecruitment.gov.in.auth.entity.LoginLogoutAuditHistory;
import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.LoginLogoutAuditHistoryRepository;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.common.security.AuthenticationAuditService;

class AuthenticationAuditServiceImplTest {

    private static final Instant AUDIT_TIME = Instant.parse("2026-08-13T10:30:00Z");

    @Test
    void recordsLoginWithoutPersistingRawSessionIdentifier() {
        LoginLogoutAuditHistoryRepository auditRepository = mock(LoginLogoutAuditHistoryRepository.class);
        AuthenticationAuditServiceImpl service = service(auditRepository, mock(UserRepository.class));

        service.recordLogin(
                41L,
                "USER@EXAMPLE.COM",
                "raw-session-id",
                "127.0.0.1",
                "Browser\nAgent",
                AuthenticationAuditService.METHOD_PASSWORD,
                AuthenticationAuditService.SOURCE_WEB);

        ArgumentCaptor<LoginLogoutAuditHistory> captor =
                ArgumentCaptor.forClass(LoginLogoutAuditHistory.class);
        verify(auditRepository).save(captor.capture());
        LoginLogoutAuditHistory audit = captor.getValue();
        assertThat(audit.getUserId()).isEqualTo(41L);
        assertThat(audit.getUsername()).isEqualTo("user@example.com");
        assertThat(audit.getEventType()).isEqualTo(LoginLogoutAuditEventType.LOGIN);
        assertThat(audit.getEventTime()).isEqualTo(AUDIT_TIME);
        assertThat(audit.getSessionIdHash()).hasSize(64).doesNotContain("raw-session-id");
        assertThat(audit.getUserAgent()).isEqualTo("Browser Agent");
        assertThat(audit.getAuthenticationMethod()).isEqualTo("PASSWORD");
    }

    @Test
    void recordsLogoutAndResolvesUserIdFromUsername() {
        LoginLogoutAuditHistoryRepository auditRepository = mock(LoginLogoutAuditHistoryRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        User user = new User();
        user.setId(52L);
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        AuthenticationAuditServiceImpl service = service(auditRepository, userRepository);

        service.recordLogout(
                "user@example.com",
                "raw-session-id",
                "10.10.1.2",
                "Browser",
                AuthenticationAuditService.REASON_USER_INITIATED,
                AuthenticationAuditService.SOURCE_WEB);

        ArgumentCaptor<LoginLogoutAuditHistory> captor =
                ArgumentCaptor.forClass(LoginLogoutAuditHistory.class);
        verify(auditRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(52L);
        assertThat(captor.getValue().getEventType()).isEqualTo(LoginLogoutAuditEventType.LOGOUT);
        assertThat(captor.getValue().getLogoutReason()).isEqualTo("USER_INITIATED");
        assertThat(captor.getValue().getAuthenticationMethod()).isNull();
    }

    private AuthenticationAuditServiceImpl service(
            LoginLogoutAuditHistoryRepository auditRepository,
            UserRepository userRepository) {
        when(auditRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        return new AuthenticationAuditServiceImpl(
                auditRepository,
                userRepository,
                Clock.fixed(AUDIT_TIME, ZoneOffset.UTC));
    }
}
