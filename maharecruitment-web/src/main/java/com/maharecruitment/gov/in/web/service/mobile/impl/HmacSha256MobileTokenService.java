package com.maharecruitment.gov.in.web.service.mobile.impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maharecruitment.gov.in.web.properties.MobileAuthProperties;
import com.maharecruitment.gov.in.web.service.mobile.MobileAuthenticatedUser;
import com.maharecruitment.gov.in.web.service.mobile.MobileTokenClaims;
import com.maharecruitment.gov.in.web.service.mobile.MobileTokenConfigurationException;
import com.maharecruitment.gov.in.web.service.mobile.MobileTokenIssue;
import com.maharecruitment.gov.in.web.service.mobile.MobileTokenService;
import com.maharecruitment.gov.in.web.service.mobile.MobileTokenValidationException;

@Service
public class HmacSha256MobileTokenService implements MobileTokenService {

    private static final Logger log = LoggerFactory.getLogger(HmacSha256MobileTokenService.class);
    private static final String HMAC_SHA_256 = "HmacSHA256";
    private static final String TOKEN_TYPE = "Bearer";
    private static final int MIN_SECRET_BYTES = 32;
    private static final int GENERATED_SECRET_BYTES = 64;
    private static final int MAX_TOKEN_LENGTH = 4096;
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final MobileAuthProperties properties;
    private final Clock clock;
    private final SecretKey signingKey;

    @Autowired
    public HmacSha256MobileTokenService(ObjectMapper objectMapper, MobileAuthProperties properties) {
        this(objectMapper, properties, Clock.systemUTC());
    }

    HmacSha256MobileTokenService(ObjectMapper objectMapper, MobileAuthProperties properties, Clock clock) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.clock = clock;
        this.signingKey = new SecretKeySpec(resolveSigningKeyBytes(properties), HMAC_SHA_256);
    }

    @Override
    public MobileTokenIssue issueToken(MobileAuthenticatedUser user) {
        if (user == null || !StringUtils.hasText(user.email())) {
            throw new IllegalArgumentException("Authenticated mobile user is required.");
        }

        Instant issuedAt = clock.instant();
        Duration ttl = properties.getAccessTokenTtl();
        Instant expiresAt = issuedAt.plus(ttl);

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("iss", properties.getIssuer());
        payload.put("sub", user.email());
        payload.put("uid", user.userId());
        payload.put("name", user.name());
        payload.put("email", user.email());
        payload.put("mobile_no", user.mobileNo());
        payload.put("roles", user.roles());
        payload.put("iat", issuedAt.getEpochSecond());
        payload.put("exp", expiresAt.getEpochSecond());
        payload.put("jti", UUID.randomUUID().toString());

        String signingInput = base64Url(toJson(header)) + "." + base64Url(toJson(payload));
        String accessToken = signingInput + "." + base64Url(sign(signingInput));

        return new MobileTokenIssue(accessToken, TOKEN_TYPE, issuedAt, expiresAt, ttl.toSeconds());
    }

    @Override
    public MobileTokenClaims validateToken(String token) {
        if (!StringUtils.hasText(token) || token.length() > MAX_TOKEN_LENGTH) {
            throw new MobileTokenValidationException("Invalid token.");
        }

        String[] parts = token.split("\\.", -1);
        if (parts.length != 3 || parts[0].isBlank() || parts[1].isBlank() || parts[2].isBlank()) {
            throw new MobileTokenValidationException("Invalid token.");
        }

        Map<String, Object> header = readJsonPart(parts[0]);
        if (!"HS256".equals(header.get("alg"))) {
            throw new MobileTokenValidationException("Unsupported token algorithm.");
        }

        String signingInput = parts[0] + "." + parts[1];
        byte[] expectedSignature = sign(signingInput);
        byte[] actualSignature = decodeBase64Url(parts[2]);
        if (!MessageDigest.isEqual(expectedSignature, actualSignature)) {
            throw new MobileTokenValidationException("Invalid token signature.");
        }

        Map<String, Object> payload = readJsonPart(parts[1]);
        String issuer = requireText(payload.get("iss"), "iss");
        if (!properties.getIssuer().equals(issuer)) {
            throw new MobileTokenValidationException("Invalid token issuer.");
        }

        String subject = requireText(payload.get("sub"), "sub");
        Instant issuedAt = Instant.ofEpochSecond(requireLong(payload.get("iat"), "iat"));
        Instant expiresAt = Instant.ofEpochSecond(requireLong(payload.get("exp"), "exp"));
        if (!clock.instant().isBefore(expiresAt)) {
            throw new MobileTokenValidationException("Token has expired.");
        }

        return new MobileTokenClaims(
                subject,
                optionalLong(payload.get("uid")),
                roles(payload.get("roles")),
                issuedAt,
                expiresAt,
                requireText(payload.get("jti"), "jti"));
    }

    private byte[] resolveSigningKeyBytes(MobileAuthProperties properties) {
        String configuredSecret = properties.getJwtSecret();
        if (StringUtils.hasText(configuredSecret)) {
            byte[] secretBytes = decodeConfiguredSecret(configuredSecret.trim());
            if (secretBytes.length < MIN_SECRET_BYTES) {
                throw new MobileTokenConfigurationException(
                        "app.mobile-auth.jwt-secret must contain at least 32 bytes of entropy.");
            }
            return secretBytes;
        }

        byte[] generatedSecret = new byte[GENERATED_SECRET_BYTES];
        new SecureRandom().nextBytes(generatedSecret);
        log.warn("app.mobile-auth.jwt-secret is not configured. Generated an in-memory mobile JWT signing key; "
                + "tokens will become invalid after restart.");
        return generatedSecret;
    }

    private byte[] decodeConfiguredSecret(String secret) {
        byte[] base64UrlDecoded = tryDecode(secret, Base64.getUrlDecoder());
        if (base64UrlDecoded.length >= MIN_SECRET_BYTES) {
            return base64UrlDecoded;
        }

        byte[] base64Decoded = tryDecode(secret, Base64.getDecoder());
        if (base64Decoded.length >= MIN_SECRET_BYTES) {
            return base64Decoded;
        }

        return secret.getBytes(StandardCharsets.UTF_8);
    }

    private byte[] tryDecode(String value, Base64.Decoder decoder) {
        try {
            return decoder.decode(value);
        } catch (IllegalArgumentException ex) {
            return new byte[0];
        }
    }

    private byte[] toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize mobile token.", ex);
        }
    }

    private String base64Url(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private byte[] decodeBase64Url(String value) {
        try {
            return Base64.getUrlDecoder().decode(value);
        } catch (IllegalArgumentException ex) {
            throw new MobileTokenValidationException("Invalid token encoding.", ex);
        }
    }

    private byte[] sign(String signingInput) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA_256);
            mac.init(signingKey);
            return mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Unable to sign mobile token.", ex);
        }
    }

    private Map<String, Object> readJsonPart(String value) {
        try {
            return objectMapper.readValue(decodeBase64Url(value), MAP_TYPE);
        } catch (IOException ex) {
            throw new MobileTokenValidationException("Invalid token JSON.", ex);
        }
    }

    private String requireText(Object value, String claim) {
        if (value instanceof String text && StringUtils.hasText(text)) {
            return text;
        }
        throw new MobileTokenValidationException("Missing token claim: " + claim);
    }

    private Long optionalLong(Object value) {
        if (value == null) {
            return null;
        }
        return requireLong(value, "uid");
    }

    private long requireLong(Object value, String claim) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && StringUtils.hasText(text)) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ex) {
                throw new MobileTokenValidationException("Invalid token claim: " + claim, ex);
            }
        }
        throw new MobileTokenValidationException("Missing token claim: " + claim);
    }

    private List<String> roles(Object value) {
        if (!(value instanceof Collection<?> collection)) {
            return List.of();
        }

        return collection.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }
}
