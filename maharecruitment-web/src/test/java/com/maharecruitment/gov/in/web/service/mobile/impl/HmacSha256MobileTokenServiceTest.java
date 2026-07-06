package com.maharecruitment.gov.in.web.service.mobile.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maharecruitment.gov.in.web.properties.MobileAuthProperties;
import com.maharecruitment.gov.in.web.service.mobile.MobileAuthenticatedUser;
import com.maharecruitment.gov.in.web.service.mobile.MobileTokenIssue;
import com.maharecruitment.gov.in.web.service.mobile.MobileTokenService;
import com.maharecruitment.gov.in.web.service.mobile.MobileTokenValidationException;

class HmacSha256MobileTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-01T10:00:00Z");

    @Test
    void issueTokenCanBeValidated() {
        HmacSha256MobileTokenService tokenService = tokenService();

        MobileTokenIssue token = tokenService.issueToken(new MobileAuthenticatedUser(
                7L,
                "Test User",
                "user@example.com",
                "9876543210",
                List.of("ROLE_EMPLOYEE")));

        var claims = tokenService.validateToken(token.accessToken());

        assertThat(token.tokenType()).isEqualTo("Bearer");
        assertThat(token.expiresInSeconds()).isEqualTo(900);
        assertThat(token.expiresAt()).isEqualTo(NOW.plusSeconds(900));
        assertThat(claims.subject()).isEqualTo("user@example.com");
        assertThat(claims.userId()).isEqualTo(7L);
        assertThat(claims.roles()).containsExactly("ROLE_EMPLOYEE");
    }

    @Test
    void validateTokenRejectsTamperedSignature() {
        HmacSha256MobileTokenService tokenService = tokenService();
        MobileTokenIssue token = tokenService.issueToken(new MobileAuthenticatedUser(
                7L,
                "Test User",
                "user@example.com",
                "9876543210",
                List.of("ROLE_EMPLOYEE")));

        String[] tokenParts = token.accessToken().split("\\.", -1);
        char replacement = tokenParts[2].charAt(0) == 'A' ? 'B' : 'A';
        String tamperedSignature = replacement + tokenParts[2].substring(1);
        String tamperedToken = tokenParts[0] + "." + tokenParts[1] + "." + tamperedSignature;

        assertThatThrownBy(() -> tokenService.validateToken(tamperedToken))
                .isInstanceOf(MobileTokenValidationException.class);
    }

    @Test
    void springCanCreateTokenServiceWithProductionConstructor() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(ObjectMapper.class, () -> new ObjectMapper());
            context.registerBean(MobileAuthProperties.class, this::properties);
            context.register(HmacSha256MobileTokenService.class);

            context.refresh();

            assertThat(context.getBean(MobileTokenService.class))
                    .isInstanceOf(HmacSha256MobileTokenService.class);
        }
    }

    private HmacSha256MobileTokenService tokenService() {
        return new HmacSha256MobileTokenService(
                new ObjectMapper(),
                properties(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private MobileAuthProperties properties() {
        MobileAuthProperties properties = new MobileAuthProperties();
        properties.setIssuer("test-issuer");
        properties.setAccessTokenTtl(Duration.ofMinutes(15));
        properties.setJwtSecret("0123456789abcdef0123456789abcdef");
        return properties;
    }
}
