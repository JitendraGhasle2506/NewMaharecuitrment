package com.maharecruitment.gov.in.web.service.passwordreset;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.web.properties.PasswordResetProperties;

@Service
public class PasswordResetRateLimiter {

    private final PasswordResetProperties properties;
    private final Map<String, RateLimitState> requestStore = new ConcurrentHashMap<>();

    public PasswordResetRateLimiter(PasswordResetProperties properties) {
        this.properties = properties;
    }

    public void checkOtpRequestAllowed(String identifier, String clientIp, Long userId) {
        Duration window = Duration.ofSeconds(properties.getRequestWindowSeconds());
        List<RateLimitRule> rules = new ArrayList<>();
        rules.add(new RateLimitRule("otp-request:identifier:" + normalize(identifier), properties.getMaxOtpRequests(), window));
        rules.add(new RateLimitRule("otp-request:ip:" + normalize(clientIp), Math.max(3, properties.getMaxOtpRequests()), window));
        if (userId != null) {
            rules.add(new RateLimitRule("otp-request:user:" + userId, properties.getMaxOtpRequests(), window));
        }
        checkAllowed(rules);
    }

    public void checkOtpVerifyAllowed(String identifier, String clientIp) {
        Duration window = Duration.ofSeconds(Math.max(1, properties.getVerifyRateWindowSeconds()));
        checkAllowed(List.of(
                new RateLimitRule("otp-verify:identifier:" + normalize(identifier), properties.getVerifyRateLimit(), window),
                new RateLimitRule("otp-verify:ip:" + normalize(clientIp), properties.getVerifyRateLimit(), window)));
    }

    private void checkAllowed(List<RateLimitRule> rules) {
        Instant now = Instant.now();
        long retryAfterSeconds = 0;
        for (RateLimitRule rule : rules) {
            RateLimitState state = requestStore.computeIfAbsent(rule.key(), ignored -> new RateLimitState());
            synchronized (state) {
                prune(state, now, rule.window());
                if (state.requestTimes.size() >= Math.max(1, rule.limit())) {
                    retryAfterSeconds = Math.max(
                            retryAfterSeconds,
                            secondsUntil(now, state.requestTimes.peekFirst().plus(rule.window())));
                }
            }
        }

        if (retryAfterSeconds > 0) {
            throw new RateLimitExceededException(retryAfterSeconds);
        }

        for (RateLimitRule rule : rules) {
            RateLimitState state = requestStore.computeIfAbsent(rule.key(), ignored -> new RateLimitState());
            synchronized (state) {
                prune(state, now, rule.window());
                state.requestTimes.addLast(now);
            }
        }
    }

    private void prune(RateLimitState state, Instant now, Duration window) {
        Instant cutoff = now.minus(window);
        while (!state.requestTimes.isEmpty() && state.requestTimes.peekFirst().isBefore(cutoff)) {
            state.requestTimes.removeFirst();
        }
    }

    private long secondsUntil(Instant now, Instant target) {
        return Math.max(1, Duration.between(now, target).getSeconds());
    }

    private String normalize(String value) {
        return StringUtils.hasText(value)
                ? value.trim().toLowerCase(Locale.ROOT)
                : "unknown";
    }

    private static final class RateLimitState {
        private final Deque<Instant> requestTimes = new ArrayDeque<>();
    }

    private record RateLimitRule(String key, int limit, Duration window) {
    }
}
