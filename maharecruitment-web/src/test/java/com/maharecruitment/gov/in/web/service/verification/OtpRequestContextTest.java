package com.maharecruitment.gov.in.web.service.verification;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class OtpRequestContextTest {

    @Test
    void ignoresForwardedIpHeadersByDefault() {
        MockHttpServletRequest request = request();
        request.addHeader("X-Forwarded-For", "198.51.100.44");
        request.addHeader("X-Real-IP", "198.51.100.45");

        OtpRequestContext context = OtpRequestContext.from(request);

        assertThat(context.normalizedClientIp()).isEqualTo("203.0.113.10");
    }

    @Test
    void usesForwardedIpOnlyWhenExplicitlyTrusted() {
        MockHttpServletRequest request = request();
        request.addHeader("X-Forwarded-For", "198.51.100.44, 10.0.0.5");

        OtpRequestContext context = OtpRequestContext.from(request, true);

        assertThat(context.normalizedClientIp()).isEqualTo("198.51.100.44");
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/verifications/otp/send");
        request.setRemoteAddr("203.0.113.10");
        return request;
    }
}
