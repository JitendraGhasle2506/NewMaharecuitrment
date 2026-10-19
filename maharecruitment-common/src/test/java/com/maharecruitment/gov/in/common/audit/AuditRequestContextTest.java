package com.maharecruitment.gov.in.common.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class AuditRequestContextTest {
    @ParameterizedTest
    @ValueSource(strings = {"192.0.2.1", "::1", "2001:db8::42", "::ffff:192.0.2.4"})
    void acceptsLiteralIpv4AndIpv6(String ip) {
        assertThat(ClientIpAddress.literal(ip)).isEqualTo(ip);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "example.com", "999.1.1.1", "1.2.3", "2001:invalid::1", "192.0.2.1\r\nInjected", "UNKNOWN"})
    void rejectsHostnamesAndMalformedAddresses(String ip) {
        assertThat(ClientIpAddress.literal(ip)).isNull();
    }

    @Test void usesForwardedHeadersOnlyWhenTrustedAndFallsBackForMalformedValues() {
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.1");
        request.addHeader("X-Forwarded-For", "2001:db8::42, 10.0.0.1");
        assertThat(ClientIpAddress.resolve(request, false)).isEqualTo("192.0.2.1");
        assertThat(ClientIpAddress.resolve(request, true)).isEqualTo("2001:db8::42");
        request.removeHeader("X-Forwarded-For");
        request.addHeader("X-Forwarded-For", "forged.example");
        request.addHeader("X-Real-IP", "198.51.100.2");
        assertThat(ClientIpAddress.resolve(request, true)).isEqualTo("198.51.100.2");
        request.removeHeader("X-Real-IP");
        assertThat(ClientIpAddress.resolve(request, true)).isEqualTo("192.0.2.1");
    }

    @Test void isolatesThreadsRestoresNestedScopesAndClearsAfterExceptions() throws Exception {
        assertThat(AuditRequestContext.ipAddress(null)).isEqualTo("SYSTEM");
        try (var outer = AuditRequestContext.open("192.0.2.1")) {
            assertThat(AuditRequestContext.ipAddress("198.51.100.1")).isEqualTo("192.0.2.1");
            try {
                try (var nested = AuditRequestContext.open("::1")) {
                    assertThat(AuditRequestContext.ipAddress(null)).isEqualTo("::1");
                    throw new IllegalStateException("test");
                }
            } catch (IllegalStateException expected) { }
            assertThat(AuditRequestContext.ipAddress(null)).isEqualTo("192.0.2.1");
            try (var executor = Executors.newSingleThreadExecutor()) {
                assertThat(executor.submit(() -> AuditRequestContext.ipAddress(null)).get()).isEqualTo("SYSTEM");
            }
        }
        assertThat(AuditRequestContext.ipAddress(null)).isEqualTo("SYSTEM");
    }

    @Test void requestContextFallbackDoesNotTrustHeadersOrSuppliedAddress() {
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.4");
        request.addHeader("X-Forwarded-For", "198.51.100.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        try {
            assertThat(AuditRequestContext.ipAddress("203.0.113.9")).isEqualTo("192.0.2.4");
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
        assertThat(AuditRequestContext.ipAddress("203.0.113.9")).isEqualTo("203.0.113.9");
    }
}
