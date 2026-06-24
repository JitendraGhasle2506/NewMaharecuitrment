package com.maharecruitment.gov.in.web.service.verification;

import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;

public record OtpRequestContext(String clientIp) {

    private static final String UNKNOWN_CLIENT = "unknown";

    public static OtpRequestContext from(HttpServletRequest request) {
        if (request == null) {
            return new OtpRequestContext(UNKNOWN_CLIENT);
        }

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            String firstForwardedIp = forwardedFor.split(",", 2)[0].trim();
            if (StringUtils.hasText(firstForwardedIp)) {
                return new OtpRequestContext(firstForwardedIp);
            }
        }

        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return new OtpRequestContext(realIp.trim());
        }

        String remoteAddr = request.getRemoteAddr();
        return new OtpRequestContext(StringUtils.hasText(remoteAddr) ? remoteAddr.trim() : UNKNOWN_CLIENT);
    }

    public String normalizedClientIp() {
        return StringUtils.hasText(clientIp) ? clientIp.trim() : UNKNOWN_CLIENT;
    }
}
