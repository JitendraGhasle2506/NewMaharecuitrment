package com.maharecruitment.gov.in.web.service.verification;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.web.dto.verification.VerificationChannel;
import com.maharecruitment.gov.in.web.properties.OtpVerificationProperties;

@Service
public class OtpRateLimiter {

    private final OtpVerificationProperties properties;
    private final Map<String, RateLimitState> requestStore = new ConcurrentHashMap<>();

    public OtpRateLimiter(OtpVerificationProperties properties) {
        this.properties = properties;
    }

    public void checkSendAllowed(
            String purpose,
            VerificationChannel channel,
            String reference,
            OtpRequestContext context) {
        checkAllowed(
                "send",
                buildKeys("send", purpose, channel, reference, context),
                Math.max(1, properties.getResendLimit()),
                Duration.ofMinutes(Math.max(1, properties.getResendWindowMinutes())));
    }

    public void checkVerifyAllowed(
            String purpose,
            VerificationChannel channel,
            String reference,
            OtpRequestContext context) {
        checkAllowed(
                "verify",
                buildKeys("verify", purpose, channel, reference, context),
                Math.max(1, properties.getVerifyRateLimit()),
                Duration.ofSeconds(Math.max(1, properties.getVerifyRateWindowSeconds())));
    }

    private void checkAllowed(String action, List<String> keys, int limit, Duration window) {
        Instant now = Instant.now();
        List<RateLimitState> states = keys.stream()
                .map(key -> requestStore.computeIfAbsent(key, ignored -> new RateLimitState()))
                .toList();

        long retryAfterSeconds = 0;
        for (RateLimitState state : states) {
            synchronized (state) {
                prune(state, now, window);
                if (state.requestTimes.size() >= limit) {
                    Instant oldestRequest = state.requestTimes.peekFirst();
                    retryAfterSeconds = Math.max(
                            retryAfterSeconds,
                            secondsUntil(now, oldestRequest.plus(window)));
                }
            }
        }

        if (retryAfterSeconds > 0) {
            throw new OtpRateLimitException(
                    "OTP " + action + " rate limit exceeded.",
                    retryAfterSeconds);
        }

        for (RateLimitState state : states) {
            synchronized (state) {
                prune(state, now, window);
                state.requestTimes.addLast(now);
            }
        }
    }

    private List<String> buildKeys(
            String action,
            String purpose,
            VerificationChannel channel,
            String reference,
            OtpRequestContext context) {
        Set<String> keys = new LinkedHashSet<>();
        String normalizedPurpose = normalize(purpose);
        String normalizedChannel = channel == null ? "unknown" : channel.name().toLowerCase(Locale.ROOT);
        String normalizedReference = normalize(reference);
        String clientIp = context == null ? "unknown" : normalize(context.normalizedClientIp());

        keys.add(action + ":ip:" + clientIp);
        keys.add(action + ":ref:" + normalizedPurpose + ":" + normalizedChannel + ":" + normalizedReference);
        return new ArrayList<>(keys);
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
}
