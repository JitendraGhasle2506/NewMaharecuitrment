package com.maharecruitment.gov.in.web.service.verification;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.web.dto.verification.VerificationChannel;
import com.maharecruitment.gov.in.web.properties.OtpVerificationProperties;

@Service
public class OtpRateLimiter {

    private final OtpVerificationProperties properties;
    private final Map<String, RateLimitState> requestStore = new HashMap<>();
    private final ReentrantLock reservationLock = new ReentrantLock();

    public OtpRateLimiter(OtpVerificationProperties properties) {
        this.properties = properties;
    }

    public SendReservation checkSendAllowed(
            String purpose,
            VerificationChannel channel,
            String reference,
            OtpRequestContext context) {
        Duration sendWindow = Duration.ofMinutes(Math.max(1, properties.getResendWindowMinutes()));
        return checkAllowed(
                "send",
                List.of(
                        new RateLimitRule(
                                buildReferenceKey("send", purpose, channel, reference),
                                Math.max(1, properties.getResendLimit()),
                                sendWindow,
                                Duration.ofSeconds(Math.max(0, properties.getResendCooldownSeconds()))),
                        new RateLimitRule(
                                buildIpKey("send", context),
                                Math.max(properties.getSendIpLimit(), properties.getResendLimit()),
                                sendWindow,
                                Duration.ZERO)));
    }

    public void checkVerifyAllowed(
            String purpose,
            VerificationChannel channel,
            String reference,
            OtpRequestContext context) {
        checkAllowed(
                "verify",
                List.of(
                        new RateLimitRule(
                                buildReferenceKey("verify", purpose, channel, reference),
                                Math.max(1, properties.getVerifyRateLimit()),
                                Duration.ofSeconds(Math.max(1, properties.getVerifyRateWindowSeconds())),
                                Duration.ZERO),
                        new RateLimitRule(
                                buildIpKey("verify", context),
                                Math.max(1, properties.getVerifyRateLimit()),
                                Duration.ofSeconds(Math.max(1, properties.getVerifyRateWindowSeconds())),
                                Duration.ZERO)));
    }

    public void releaseSendReservation(SendReservation reservation) {
        if (reservation == null) {
            return;
        }
        reservationLock.lock();
        try {
            reservation.attempts.forEach(attempt -> {
                RateLimitState state = attempt.state();
                state.requestTimes.removeLastOccurrence(attempt.reservedAt());
                if (state.requestTimes.isEmpty()) {
                    requestStore.remove(attempt.key(), state);
                }
            });
        } finally {
            reservationLock.unlock();
        }
    }

    private SendReservation checkAllowed(String action, List<RateLimitRule> rules) {
        reservationLock.lock();
        try {
            Instant now = Instant.now();
            List<RateLimitCheck> checks = rules.stream()
                    .map(rule -> new RateLimitCheck(
                            rule,
                            requestStore.computeIfAbsent(rule.key(), ignored -> new RateLimitState())))
                    .toList();

            long retryAfterSeconds = 0;
            for (RateLimitCheck check : checks) {
                RateLimitRule rule = check.rule();
                RateLimitState state = check.state();
                prune(state, now, rule.window());
                if (state.requestTimes.size() >= rule.limit()) {
                    Instant oldestRequest = state.requestTimes.peekFirst();
                    retryAfterSeconds = Math.max(
                            retryAfterSeconds,
                            secondsUntil(now, oldestRequest.plus(rule.window())));
                }
                Instant latestRequest = state.requestTimes.peekLast();
                if (latestRequest != null && !rule.minimumInterval().isZero()) {
                    Instant nextAllowedAt = latestRequest.plus(rule.minimumInterval());
                    if (now.isBefore(nextAllowedAt)) {
                        retryAfterSeconds = Math.max(
                                retryAfterSeconds,
                                secondsUntil(now, nextAllowedAt));
                    }
                }
            }

            if (retryAfterSeconds > 0) {
                throw new OtpRateLimitException(
                        "OTP " + action + " rate limit exceeded.",
                        retryAfterSeconds);
            }

            List<ReservedAttempt> reservedAttempts = new java.util.ArrayList<>(checks.size());
            for (RateLimitCheck check : checks) {
                RateLimitRule rule = check.rule();
                RateLimitState state = check.state();
                prune(state, now, rule.window());
                state.requestTimes.addLast(now);
                reservedAttempts.add(new ReservedAttempt(rule.key(), state, now));
            }
            return new SendReservation(List.copyOf(reservedAttempts));
        } finally {
            reservationLock.unlock();
        }
    }

    private String buildReferenceKey(
            String action,
            String purpose,
            VerificationChannel channel,
            String reference) {
        String normalizedPurpose = normalize(purpose);
        String normalizedChannel = channel == null ? "unknown" : channel.name().toLowerCase(Locale.ROOT);
        String normalizedReference = normalize(reference);
        return action + ":ref:" + normalizedPurpose + ":" + normalizedChannel + ":" + normalizedReference;
    }

    private String buildIpKey(String action, OtpRequestContext context) {
        String clientIp = context == null ? "unknown" : normalize(context.normalizedClientIp());
        return action + ":ip:" + clientIp;
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

    private record RateLimitRule(String key, int limit, Duration window, Duration minimumInterval) {
    }

    private record RateLimitCheck(RateLimitRule rule, RateLimitState state) {
    }

    private record ReservedAttempt(String key, RateLimitState state, Instant reservedAt) {
    }

    public static final class SendReservation {
        private final List<ReservedAttempt> attempts;

        private SendReservation(List<ReservedAttempt> attempts) {
            this.attempts = attempts;
        }
    }
}
