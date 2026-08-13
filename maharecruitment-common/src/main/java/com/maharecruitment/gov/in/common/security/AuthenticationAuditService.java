package com.maharecruitment.gov.in.common.security;

/** Persists security audit events without coupling security handlers to JPA. */
public interface AuthenticationAuditService {

    String SOURCE_WEB = "WEB";
    String METHOD_PASSWORD = "PASSWORD";
    String METHOD_OTP = "OTP";
    String REASON_USER_INITIATED = "USER_INITIATED";

    void recordLoginFailure(
            String username,
            String ipAddress,
            String userAgent,
            String authenticationMethod,
            String failureReason,
            String source);

    void recordLogin(
            Long userId,
            String username,
            String sessionId,
            String ipAddress,
            String userAgent,
            String authenticationMethod,
            String source);

    void recordLogout(
            String username,
            String sessionId,
            String ipAddress,
            String userAgent,
            String logoutReason,
            String source);
}
