package com.maharecruitment.gov.in.web.service.mobile.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.web.entity.mobile.MobileRefreshTokenEntity;
import com.maharecruitment.gov.in.web.properties.MobileAuthProperties;
import com.maharecruitment.gov.in.web.repository.mobile.MobileRefreshTokenRepository;
import com.maharecruitment.gov.in.web.service.mobile.MobileRefreshSession;
import com.maharecruitment.gov.in.web.service.mobile.MobileRefreshTokenIssue;
import com.maharecruitment.gov.in.web.service.mobile.MobileRefreshTokenService;
import com.maharecruitment.gov.in.web.service.mobile.MobileTokenValidationException;

@Service
public class PersistentMobileRefreshTokenService implements MobileRefreshTokenService {

    private static final int REFRESH_TOKEN_BYTES = 32;
    private static final int MAX_REFRESH_TOKEN_LENGTH = 512;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final MobileRefreshTokenRepository refreshTokenRepository;
    private final MobileAuthProperties properties;
    private final Clock clock;

    @Autowired
    public PersistentMobileRefreshTokenService(
            MobileRefreshTokenRepository refreshTokenRepository,
            MobileAuthProperties properties) {
        this(refreshTokenRepository, properties, Clock.systemUTC());
    }

    PersistentMobileRefreshTokenService(
            MobileRefreshTokenRepository refreshTokenRepository,
            MobileAuthProperties properties,
            Clock clock) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public MobileRefreshTokenIssue issueRefreshToken(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("Authenticated mobile user is required.");
        }
        return saveNewRefreshToken(user, clock.instant());
    }

    @Override
    @Transactional
    public MobileRefreshSession rotateRefreshToken(String refreshToken) {
        String tokenHash = hash(refreshToken);
        MobileRefreshTokenEntity current = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new MobileTokenValidationException("Invalid refresh token."));

        Instant now = clock.instant();
        if (current.getRevokedAt() != null) {
            revokeActiveTokens(current.getUser(), now);
            throw new MobileTokenValidationException("Refresh token was already used.");
        }
        if (!now.isBefore(current.getExpiresAt())) {
            throw new MobileTokenValidationException("Refresh token has expired.");
        }

        MobileRefreshTokenIssue replacement = saveNewRefreshToken(current.getUser(), now);
        current.setRevokedAt(now);
        current.setReplacedByTokenHash(hash(replacement.refreshToken()));
        refreshTokenRepository.save(current);

        return new MobileRefreshSession(current.getUser(), replacement);
    }

    @Override
    @Transactional
    public void revokeRefreshToken(String refreshToken, boolean revokeAllSessions) {
        String tokenHash = hash(refreshToken);
        MobileRefreshTokenEntity current = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new MobileTokenValidationException("Invalid refresh token."));

        Instant now = clock.instant();
        if (current.getRevokedAt() != null || !now.isBefore(current.getExpiresAt())) {
            return;
        }

        if (revokeAllSessions) {
            revokeActiveTokens(current.getUser(), now);
            return;
        }

        current.setRevokedAt(now);
        refreshTokenRepository.save(current);
    }

    @Override
    @Transactional
    public void revokeActiveTokensForUser(User user) {
        revokeActiveTokens(user, clock.instant());
    }

    private MobileRefreshTokenIssue saveNewRefreshToken(User user, Instant issuedAt) {
        Duration ttl = properties.getRefreshTokenTtl();
        Instant expiresAt = issuedAt.plus(ttl);
        String rawToken = generateToken();

        MobileRefreshTokenEntity entity = new MobileRefreshTokenEntity();
        entity.setUser(user);
        entity.setTokenHash(hash(rawToken));
        entity.setIssuedAt(issuedAt);
        entity.setExpiresAt(expiresAt);
        refreshTokenRepository.save(entity);

        return new MobileRefreshTokenIssue(rawToken, expiresAt, ttl.toSeconds());
    }

    private void revokeActiveTokens(User user, Instant revokedAt) {
        if (user != null && user.getId() != null) {
            refreshTokenRepository.revokeActiveTokensForUser(user.getId(), revokedAt);
        }
    }

    private String generateToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        if (!StringUtils.hasText(token) || token.length() > MAX_REFRESH_TOKEN_LENGTH) {
            throw new MobileTokenValidationException("Invalid refresh token.");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedValue = digest.digest(token.trim().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashedValue);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available.", ex);
        }
    }
}
