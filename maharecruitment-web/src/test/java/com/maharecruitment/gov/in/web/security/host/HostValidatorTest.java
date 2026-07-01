package com.maharecruitment.gov.in.web.security.host;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class HostValidatorTest {

    private HostValidator validator;

    @BeforeEach
    void setUp() {
        HostProperties properties = new HostProperties();
        properties.setAllowedHosts(List.of(
                "portal.example.gov.in",
                "www.portal.example.gov.in",
                "localhost",
                "127.0.0.1"));
        properties.setAllowedPorts(Set.of(80, 443, 8443));
        validator = new HostValidator(properties);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "portal.example.gov.in",
            "www.portal.example.gov.in",
            "localhost",
            "LOCALHOST",
            "127.0.0.1",
            "portal.example.gov.in:443",
            "localhost:8443"
    })
    void acceptsAllowlistedHosts(String hostHeader) {
        assertThat(validator.validate(hostHeader).valid()).isTrue();
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("invalidHostHeaders")
    void rejectsInvalidHostHeaders(String description, String hostHeader) {
        assertThat(validator.validate(hostHeader).valid())
                .as(description)
                .isFalse();
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("invalidHostHeaderLists")
    void rejectsInvalidHostHeaderLists(String description, List<String> hostHeaders) {
        assertThat(validator.validate(hostHeaders).valid())
                .as(description)
                .isFalse();
    }

    private static Stream<Arguments> invalidHostHeaders() {
        return Stream.of(
                Arguments.of("unallowlisted host", "evil.example.com"),
                Arguments.of("multiple hosts in one header", "portal.example.gov.in,evil.example.com"),
                Arguments.of("empty host", ""),
                Arguments.of("blank host", "   "),
                Arguments.of("CRLF injection", "portal.example.gov.in\r\nX-Forwarded-Host: evil.example.com"),
                Arguments.of("unicode spoofing", "раураl.example.gov.in"),
                Arguments.of("embedded space", "portal example.gov.in"),
                Arguments.of("script payload", "<script>alert(1)</script>"),
                Arguments.of("invalid port", "portal.example.gov.in:99999"),
                Arguments.of("port not allowlisted", "portal.example.gov.in:8080"),
                Arguments.of("trailing dot", "portal.example.gov.in."),
                Arguments.of("double dot", "portal..example.gov.in"),
                Arguments.of("very long host", "a".repeat(244) + ".example.gov.in"),
                Arguments.of("decimal IP variation", "2130706433"),
                Arguments.of("hex IP variation", "0x7f000001"),
                Arguments.of("padded IPv4 variation", "127.000.000.001"),
                Arguments.of("IPv6 literal", "[::1]"));
    }

    private static Stream<Arguments> invalidHostHeaderLists() {
        return Stream.of(
                Arguments.of("missing Host header", List.of()),
                Arguments.of("multiple Host headers", List.of("portal.example.gov.in", "evil.example.com")));
    }
}
