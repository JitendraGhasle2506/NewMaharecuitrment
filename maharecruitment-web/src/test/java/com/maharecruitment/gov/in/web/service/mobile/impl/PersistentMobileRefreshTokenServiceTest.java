package com.maharecruitment.gov.in.web.service.mobile.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.web.entity.mobile.MobileRefreshTokenEntity;
import com.maharecruitment.gov.in.web.properties.MobileAuthProperties;
import com.maharecruitment.gov.in.web.repository.mobile.MobileRefreshTokenRepository;
import com.maharecruitment.gov.in.web.service.mobile.MobileRefreshSession;
import com.maharecruitment.gov.in.web.service.mobile.MobileRefreshTokenIssue;
import com.maharecruitment.gov.in.web.service.mobile.MobileTokenValidationException;

@ExtendWith(MockitoExtension.class)
class PersistentMobileRefreshTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-01T10:00:00Z");

    @Mock
    private MobileRefreshTokenRepository refreshTokenRepository;

    @Test
    void issueRefreshTokenStoresOnlyTokenHash() {
        List<MobileRefreshTokenEntity> savedTokens = captureSavedTokens();

        MobileRefreshTokenIssue token = service().issueRefreshToken(user());

        assertThat(token.refreshToken()).isNotBlank();
        assertThat(token.expiresInSeconds()).isEqualTo(Duration.ofDays(30).toSeconds());
        assertThat(token.expiresAt()).isEqualTo(NOW.plus(Duration.ofDays(30)));
        assertThat(savedTokens).hasSize(1);
        assertThat(savedTokens.getFirst().getTokenHash())
                .hasSize(64)
                .doesNotContain(token.refreshToken());
        assertThat(savedTokens.getFirst().getUser().getId()).isEqualTo(10L);
    }

    @Test
    void rotateRefreshTokenRevokesOldTokenAndReturnsReplacement() {
        List<MobileRefreshTokenEntity> savedTokens = captureSavedTokens();
        PersistentMobileRefreshTokenService service = service();
        MobileRefreshTokenIssue original = service.issueRefreshToken(user());
        MobileRefreshTokenEntity current = savedTokens.getFirst();
        when(refreshTokenRepository.findByTokenHash(current.getTokenHash())).thenReturn(Optional.of(current));

        MobileRefreshSession session = service.rotateRefreshToken(original.refreshToken());

        assertThat(session.user().getId()).isEqualTo(10L);
        assertThat(session.refreshToken().refreshToken()).isNotEqualTo(original.refreshToken());
        assertThat(current.getRevokedAt()).isEqualTo(NOW);
        assertThat(current.getReplacedByTokenHash()).hasSize(64);
        assertThat(savedTokens).hasSize(3);
    }

    @Test
    void reusedRefreshTokenRevokesActiveTokensForUser() {
        MobileRefreshTokenEntity revokedToken = new MobileRefreshTokenEntity();
        revokedToken.setUser(user());
        revokedToken.setTokenHash("0".repeat(64));
        revokedToken.setIssuedAt(NOW.minusSeconds(60));
        revokedToken.setExpiresAt(NOW.plusSeconds(60));
        revokedToken.setRevokedAt(NOW.minusSeconds(30));
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(revokedToken));

        assertThatThrownBy(() -> service().rotateRefreshToken("already-used-refresh-token"))
                .isInstanceOf(MobileTokenValidationException.class);

        verify(refreshTokenRepository).revokeActiveTokensForUser(10L, NOW);
    }

    @Test
    void revokeRefreshTokenRevokesOnlyMatchingTokenByDefault() {
        List<MobileRefreshTokenEntity> savedTokens = captureSavedTokens();
        PersistentMobileRefreshTokenService service = service();
        MobileRefreshTokenIssue token = service.issueRefreshToken(user());
        MobileRefreshTokenEntity current = savedTokens.getFirst();
        when(refreshTokenRepository.findByTokenHash(current.getTokenHash())).thenReturn(Optional.of(current));

        service.revokeRefreshToken(token.refreshToken(), false);

        assertThat(current.getRevokedAt()).isEqualTo(NOW);
        assertThat(savedTokens).hasSize(2);
        assertThat(savedTokens.get(1)).isSameAs(current);
    }

    @Test
    void revokeRefreshTokenCanRevokeAllActiveTokensForUser() {
        List<MobileRefreshTokenEntity> savedTokens = captureSavedTokens();
        PersistentMobileRefreshTokenService service = service();
        MobileRefreshTokenIssue token = service.issueRefreshToken(user());
        MobileRefreshTokenEntity current = savedTokens.getFirst();
        when(refreshTokenRepository.findByTokenHash(current.getTokenHash())).thenReturn(Optional.of(current));

        service.revokeRefreshToken(token.refreshToken(), true);

        verify(refreshTokenRepository).revokeActiveTokensForUser(10L, NOW);
        assertThat(current.getRevokedAt()).isNull();
        assertThat(savedTokens).hasSize(1);
    }

    private List<MobileRefreshTokenEntity> captureSavedTokens() {
        List<MobileRefreshTokenEntity> savedTokens = new ArrayList<>();
        when(refreshTokenRepository.save(any(MobileRefreshTokenEntity.class))).thenAnswer(invocation -> {
            MobileRefreshTokenEntity entity = invocation.getArgument(0);
            savedTokens.add(entity);
            return entity;
        });
        return savedTokens;
    }

    private PersistentMobileRefreshTokenService service() {
        MobileAuthProperties properties = new MobileAuthProperties();
        properties.setRefreshTokenTtl(Duration.ofDays(30));
        return new PersistentMobileRefreshTokenService(
                refreshTokenRepository,
                properties,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private User user() {
        User user = new User();
        user.setId(10L);
        user.setName("Test User");
        user.setEmail("user@example.com");
        user.setActive(true);
        return user;
    }
}
