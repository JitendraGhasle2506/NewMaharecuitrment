package com.maharecruitment.gov.in.common.audit;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Request-bound, never inheritable: pooled threads must not retain another user's IP. */
public final class AuditRequestContext implements AutoCloseable {
    private static final ThreadLocal<String> CLIENT_IP = new ThreadLocal<>();
    private final String previous;

    private AuditRequestContext(String ipAddress) {
        previous = CLIENT_IP.get();
        CLIENT_IP.set(ipAddress);
    }

    public static AuditRequestContext open(String ipAddress) {
        String literal = ClientIpAddress.literal(ipAddress);
        return new AuditRequestContext(literal == null ? "UNKNOWN" : literal);
    }

    public static String ipAddress(String suppliedAddress) {
        String contextual = CLIENT_IP.get();
        if (contextual != null) return contextual;
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return ClientIpAddress.resolve(attributes.getRequest(), false);
        }
        String supplied = ClientIpAddress.literal(suppliedAddress);
        return supplied == null ? "SYSTEM" : supplied;
    }

    @Override
    public void close() {
        if (previous == null) CLIENT_IP.remove(); else CLIENT_IP.set(previous);
    }
}
