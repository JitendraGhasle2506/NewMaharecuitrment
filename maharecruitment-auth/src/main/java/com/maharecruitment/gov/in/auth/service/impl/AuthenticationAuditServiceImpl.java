package com.maharecruitment.gov.in.auth.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.auth.entity.LoginLogoutAuditEventType;
import com.maharecruitment.gov.in.auth.entity.LoginLogoutAuditHistory;
import com.maharecruitment.gov.in.auth.repository.LoginLogoutAuditHistoryRepository;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.common.security.AuthenticationAuditService;

@Service
public class AuthenticationAuditServiceImpl implements AuthenticationAuditService {

    private static final int MAX_USERNAME_LENGTH = 255;
    private static final int MAX_IP_LENGTH = 64;
    private static final int MAX_USER_AGENT_LENGTH = 500;
    private static final int MAX_METHOD_LENGTH = 20;
    private static final int MAX_REASON_LENGTH = 40;
    private static final int MAX_FAILURE_REASON_LENGTH = 64;
    private static final int MAX_SOURCE_LENGTH = 20;

    private final LoginLogoutAuditHistoryRepository auditRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Autowired
    public AuthenticationAuditServiceImpl(
            LoginLogoutAuditHistoryRepository auditRepository,
            UserRepository userRepository) {
        this(auditRepository, userRepository, Clock.systemUTC());
    }

    AuthenticationAuditServiceImpl(
            LoginLogoutAuditHistoryRepository auditRepository,
            UserRepository userRepository,
            Clock clock) {
        this.auditRepository = auditRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordLoginFailure(
            String username,
            String ipAddress,
            String userAgent,
            String authenticationMethod,
            String failureReason,
            String source) {
        String normalizedUsername = auditUsername(username);
        Long userId = userRepository.findByEmailIgnoreCase(normalizedUsername)
                .map(user -> user.getId())
                .orElse(null);
        LoginLogoutAuditHistory audit = buildEvent(
                userId,
                normalizedUsername,
                LoginLogoutAuditEventType.LOGIN_FAILURE,
                null,
                ipAddress,
                userAgent,
                authenticationMethod,
                null,
                source);
        audit.setFailureReason(required(failureReason, "Failure reason", MAX_FAILURE_REASON_LENGTH)
                .toUpperCase(Locale.ROOT));
        auditRepository.save(audit);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordLogin(
            Long userId,
            String username,
            String sessionId,
            String ipAddress,
            String userAgent,
            String authenticationMethod,
            String source) {
        auditRepository.save(buildEvent(
                userId,
                username,
                LoginLogoutAuditEventType.LOGIN,
                sessionId,
                ipAddress,
                userAgent,
                authenticationMethod,
                null,
                source));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordLogout(
            String username,
            String sessionId,
            String ipAddress,
            String userAgent,
            String logoutReason,
            String source) {
        String normalizedUsername = required(username, "Username", MAX_USERNAME_LENGTH).toLowerCase(Locale.ROOT);
        Long userId = userRepository.findByEmailIgnoreCase(normalizedUsername)
                .map(user -> user.getId())
                .orElse(null);
        auditRepository.save(buildEvent(
                userId,
                normalizedUsername,
                LoginLogoutAuditEventType.LOGOUT,
                sessionId,
                ipAddress,
                userAgent,
                null,
                logoutReason,
                source));
    }

    private LoginLogoutAuditHistory buildEvent(
            Long userId,
            String username,
            LoginLogoutAuditEventType eventType,
            String sessionId,
            String ipAddress,
            String userAgent,
            String authenticationMethod,
            String logoutReason,
            String source) {
        LoginLogoutAuditHistory audit = new LoginLogoutAuditHistory();
        audit.setUserId(userId);
        audit.setUsername(required(username, "Username", MAX_USERNAME_LENGTH).toLowerCase(Locale.ROOT));
        audit.setEventType(eventType);
        audit.setEventTime(Instant.now(clock));
        audit.setSessionIdHash(sessionId == null ? null : hashSessionId(sessionId));
        audit.setIpAddress(optional(ipAddress, MAX_IP_LENGTH));
        audit.setUserAgent(optional(userAgent, MAX_USER_AGENT_LENGTH));
        audit.setAuthenticationMethod(optionalUppercase(authenticationMethod, MAX_METHOD_LENGTH));
        audit.setLogoutReason(optionalUppercase(logoutReason, MAX_REASON_LENGTH));
        audit.setSource(required(source, "Source", MAX_SOURCE_LENGTH).toUpperCase(Locale.ROOT));
        return audit;
    }

    private String hashSessionId(String sessionId) {
        String normalized = required(sessionId, "Session identifier", 256);
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(normalized.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable.", ex);
        }
    }

    private String auditUsername(String username) {
        String normalized = optional(username, MAX_USERNAME_LENGTH);
        return normalized == null ? "[empty]" : normalized.toLowerCase(Locale.ROOT);
    }

    private String required(String value, String label, int maxLength) {
        String normalized = optional(value, maxLength);
        if (normalized == null) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return normalized;
    }

    private String optionalUppercase(String value, int maxLength) {
        String normalized = optional(value, maxLength);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private String optional(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.replaceAll("[\\p{Cntrl}]", " ").trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }
}
