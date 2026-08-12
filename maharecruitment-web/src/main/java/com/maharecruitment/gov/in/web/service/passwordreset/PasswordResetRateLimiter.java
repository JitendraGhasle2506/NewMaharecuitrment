package com.maharecruitment.gov.in.web.service.passwordreset;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.web.properties.PasswordResetProperties;
import com.maharecruitment.gov.in.web.service.verification.OtpRateLimitException;
import com.maharecruitment.gov.in.web.service.verification.OtpRateLimiter;
import com.maharecruitment.gov.in.web.service.verification.PersistentOtpRateLimitStore;

@Service
public class PasswordResetRateLimiter {

    private final PasswordResetProperties properties;
    private final PersistentOtpRateLimitStore persistentStore;

    public PasswordResetRateLimiter(
            PasswordResetProperties properties,
            PersistentOtpRateLimitStore persistentStore) {
        this.properties = properties;
        this.persistentStore = persistentStore;
    }

    public void checkOtpRequestAllowed(String identifier, String clientIp, Long userId) {
        Duration window = Duration.ofSeconds(properties.getRequestWindowSeconds());
        List<OtpRateLimiter.RateLimitRule> rules = new ArrayList<>();
        rules.add(rule("otp-request:identifier:" + normalize(identifier), properties.getMaxOtpRequests(), window));
        rules.add(rule("otp-request:ip:" + normalize(clientIp), Math.max(3, properties.getMaxOtpRequests()), window));
        if (userId != null) {
            rules.add(rule("otp-request:user:" + userId, properties.getMaxOtpRequests(), window));
        }
        checkAllowed("password-reset-send", rules);
    }

    public void checkOtpVerifyAllowed(String identifier, String clientIp) {
        Duration window = Duration.ofSeconds(Math.max(1, properties.getVerifyRateWindowSeconds()));
        checkAllowed("password-reset-verify", List.of(
                rule("otp-verify:identifier:" + normalize(identifier), properties.getVerifyRateLimit(), window),
                rule("otp-verify:ip:" + normalize(clientIp), properties.getVerifyRateLimit(), window)));
    }

    private void checkAllowed(String action, List<OtpRateLimiter.RateLimitRule> rules) {
        try {
            persistentStore.reserve(action, rules);
        } catch (OtpRateLimitException ex) {
            throw new RateLimitExceededException(ex.getRetryAfterSeconds());
        }
    }

    private OtpRateLimiter.RateLimitRule rule(String key, int limit, Duration window) {
        return new OtpRateLimiter.RateLimitRule(key, Math.max(1, limit), window, Duration.ZERO);
    }

    private String normalize(String value) {
        return StringUtils.hasText(value)
                ? value.trim().toLowerCase(Locale.ROOT)
                : "unknown";
    }

}
