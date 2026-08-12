package com.maharecruitment.gov.in.web.service.verification;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PersistentOtpRateLimitStore {

    private static final String RESERVE_SQL = """
            insert into otp_rate_limit_bucket
                (bucket_key, window_started_at, request_count, last_request_at, expires_at)
            values (?, ?, 1, ?, ?)
            on conflict (bucket_key) do update set
                window_started_at = case
                    when otp_rate_limit_bucket.window_started_at <= ? then excluded.window_started_at
                    else otp_rate_limit_bucket.window_started_at
                end,
                request_count = case
                    when otp_rate_limit_bucket.window_started_at <= ? then 1
                    else otp_rate_limit_bucket.request_count + 1
                end,
                last_request_at = excluded.last_request_at,
                expires_at = excluded.expires_at
            where (otp_rate_limit_bucket.window_started_at <= ?
                    or otp_rate_limit_bucket.request_count < ?)
              and (otp_rate_limit_bucket.last_request_at is null
                    or otp_rate_limit_bucket.last_request_at <= ?)
            returning window_started_at, request_count, last_request_at
            """;

    private static final String LOAD_SQL = """
            select window_started_at, request_count, last_request_at
            from otp_rate_limit_bucket
            where bucket_key = ?
            """;

    private final JdbcTemplate jdbcTemplate;
    private final AtomicInteger cleanupCounter = new AtomicInteger();

    public PersistentOtpRateLimitStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reserve(String action, List<OtpRateLimiter.RateLimitRule> rules) {
        Instant now = Instant.now();
        for (OtpRateLimiter.RateLimitRule rule : rules) {
            reserve(action, rule, now);
        }
        if ((cleanupCounter.incrementAndGet() & 255) == 0) {
            jdbcTemplate.update("delete from otp_rate_limit_bucket where expires_at < ?", Timestamp.from(now));
        }
    }

    private void reserve(String action, OtpRateLimiter.RateLimitRule rule, Instant now) {
        Duration window = nonZero(rule.window());
        Duration minimumInterval = rule.minimumInterval().isNegative()
                ? Duration.ZERO
                : rule.minimumInterval();
        Instant windowResetCutoff = now.minus(window);
        Instant cooldownCutoff = now.minus(minimumInterval);
        Instant expiresAt = now.plus(window).plus(minimumInterval);
        String bucketKey = sha256(rule.key());

        List<BucketState> reserved = jdbcTemplate.query(
                RESERVE_SQL,
                (resultSet, rowNumber) -> new BucketState(
                        resultSet.getTimestamp("window_started_at").toInstant(),
                        resultSet.getInt("request_count"),
                        resultSet.getTimestamp("last_request_at").toInstant()),
                bucketKey,
                Timestamp.from(now),
                Timestamp.from(now),
                Timestamp.from(expiresAt),
                Timestamp.from(windowResetCutoff),
                Timestamp.from(windowResetCutoff),
                Timestamp.from(windowResetCutoff),
                Math.max(1, rule.limit()),
                Timestamp.from(cooldownCutoff));
        if (!reserved.isEmpty()) {
            return;
        }

        BucketState state = jdbcTemplate.query(
                LOAD_SQL,
                resultSet -> resultSet.next()
                        ? new BucketState(
                                resultSet.getTimestamp("window_started_at").toInstant(),
                                resultSet.getInt("request_count"),
                                resultSet.getTimestamp("last_request_at").toInstant())
                        : null,
                bucketKey);
        long retryAfterSeconds = calculateRetryAfter(now, state, rule);
        boolean requestLimitExceeded = state != null && state.requestCount() >= Math.max(1, rule.limit());
        boolean cooldownExceeded = state != null
                && !rule.minimumInterval().isZero()
                && !rule.minimumInterval().isNegative()
                && now.isBefore(state.lastRequestAt().plus(rule.minimumInterval()));
        throw new OtpRateLimitException(
                "OTP " + action + " rate limit exceeded.",
                Math.max(1, retryAfterSeconds),
                OtpRateLimiter.responseCode(action, requestLimitExceeded, cooldownExceeded));
    }

    private long calculateRetryAfter(
            Instant now,
            BucketState state,
            OtpRateLimiter.RateLimitRule rule) {
        if (state == null) {
            return 1;
        }
        long retryAfter = 1;
        if (state.requestCount() >= Math.max(1, rule.limit())) {
            retryAfter = Math.max(
                    retryAfter,
                    secondsUntil(now, state.windowStartedAt().plus(nonZero(rule.window()))));
        }
        if (!rule.minimumInterval().isZero() && !rule.minimumInterval().isNegative()) {
            retryAfter = Math.max(
                    retryAfter,
                    secondsUntil(now, state.lastRequestAt().plus(rule.minimumInterval())));
        }
        return retryAfter;
    }

    private Duration nonZero(Duration duration) {
        return duration == null || duration.isZero() || duration.isNegative()
                ? Duration.ofSeconds(1)
                : duration;
    }

    private long secondsUntil(Instant now, Instant target) {
        return Math.max(1, Duration.between(now, target).getSeconds());
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Unable to hash OTP rate-limit key.", ex);
        }
    }

    private record BucketState(Instant windowStartedAt, int requestCount, Instant lastRequestAt) {
    }
}
