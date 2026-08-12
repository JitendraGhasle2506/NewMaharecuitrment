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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.web.dto.verification.VerificationChannel;
import com.maharecruitment.gov.in.web.properties.OtpVerificationProperties;

@Service
public class OtpRateLimiter {

    private final OtpVerificationProperties properties;
    private final PersistentOtpRateLimitStore persistentStore;
    private final Map<String, RateLimitState> requestStore = new HashMap<>();
    private final ReentrantLock reservationLock = new ReentrantLock();

    @Autowired
    public OtpRateLimiter(
            OtpVerificationProperties properties,
            PersistentOtpRateLimitStore persistentStore) {
        this.properties = properties;
        this.persistentStore = persistentStore;
    }

    /**
     * Test-only fallback that preserves deterministic unit tests without a database.
     */
    public OtpRateLimiter(OtpVerificationProperties properties) {
        this.properties = properties;
        this.persistentStore = null;
    }

    public void checkSendAllowed(
            String purpose,
            VerificationChannel channel,
            String reference,
            OtpRequestContext context) {
        Duration sendWindow = Duration.ofMinutes(Math.max(1, properties.getResendWindowMinutes()));
        Duration recipientWindow = Duration.ofMinutes(
                Math.max(1, properties.getSendRecipientWindowMinutes()));
        Duration cooldown = Duration.ofSeconds(Math.max(0, properties.getResendCooldownSeconds()));
        List<RateLimitRule> rules = new java.util.ArrayList<>();
        rules.add(new RateLimitRule(
                buildReferenceKey("send", purpose, channel, reference),
                Math.max(1, properties.getResendLimit()),
                sendWindow,
                cooldown));
        buildDeliveryRecipientKeys(reference, channel).forEach(recipientKey -> rules.add(new RateLimitRule(
                "send:recipient:" + recipientKey,
                Math.max(1, properties.getSendRecipientLimit()),
                recipientWindow,
                cooldown)));
        rules.add(new RateLimitRule(
                buildIpKey("send", context),
                Math.max(1, properties.getSendIpLimit()),
                sendWindow,
                Duration.ZERO));
        checkAllowed("send", List.copyOf(rules));
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

    private void checkAllowed(String action, List<RateLimitRule> rules) {
        if (persistentStore != null) {
            persistentStore.reserve(action, rules);
            return;
        }
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

            for (RateLimitCheck check : checks) {
                RateLimitRule rule = check.rule();
                RateLimitState state = check.state();
                prune(state, now, rule.window());
                state.requestTimes.addLast(now);
            }
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

    private List<String> buildDeliveryRecipientKeys(String reference, VerificationChannel channel) {
        VerificationChannel effectiveChannel = channel == null ? null : channel.canonical();
        if (effectiveChannel == VerificationChannel.BOTH) {
            OtpDeliveryReferences.BothReference both = OtpDeliveryReferences.parseBoth(reference);
            return List.of("email:" + normalize(both.email()), "sms:" + normalize(both.mobileNumber()));
        }
        if (effectiveChannel == VerificationChannel.EMAIL) {
            return List.of("email:" + normalize(reference));
        }
        if (effectiveChannel == VerificationChannel.SMS) {
            return List.of("sms:" + normalize(reference));
        }
        return List.of("unknown:" + normalize(reference));
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

    static record RateLimitRule(String key, int limit, Duration window, Duration minimumInterval) {
    }

    private record RateLimitCheck(RateLimitRule rule, RateLimitState state) {
    }

}
