package com.maharecruitment.gov.in.auth.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.auth.util.UserValidationUtil;

@Service
public class LoginAttemptBucketService {

    private final UserRepository userRepository;
    private final int maximumFailures;
    private final Duration blockDuration;
    private final Clock clock;
    private final ConcurrentMap<Long, FailureBucket> bucketMap = new ConcurrentHashMap<>();

    @Autowired
    public LoginAttemptBucketService(
            UserRepository userRepository,
            @Value("${security.login-protection.max-consecutive-failures:3}") int maximumFailures,
            @Value("${security.login-protection.block-duration:24h}") Duration blockDuration) {
        this(userRepository, maximumFailures, blockDuration, Clock.systemUTC());
    }

    LoginAttemptBucketService(
            UserRepository userRepository,
            int maximumFailures,
            Duration blockDuration,
            Clock clock) {
        if (maximumFailures < 1) {
            throw new IllegalArgumentException("Maximum login failures must be at least one.");
        }
        if (blockDuration == null || blockDuration.isZero() || blockDuration.isNegative()) {
            throw new IllegalArgumentException("Login block duration must be positive.");
        }
        this.userRepository = userRepository;
        this.maximumFailures = maximumFailures;
        this.blockDuration = blockDuration;
        this.clock = clock;
    }

    public LoginAttemptResult recordFailure(String identifier) {
        Optional<User> user = findUser(identifier);
        if (user.isEmpty() || user.get().getId() == null) {
            return LoginAttemptResult.notTracked(maximumFailures);
        }

        Long userId = user.get().getId();
        Instant now = clock.instant();
        AtomicReference<FailureBucket> updatedBucket = new AtomicReference<>();
        bucketMap.compute(userId, (key, currentBucket) -> {
            FailureBucket nextBucket;
            if (currentBucket == null || currentBucket.isExpired(now)) {
                nextBucket = maximumFailures == 1
                        ? new FailureBucket(1, now.plus(blockDuration))
                        : FailureBucket.firstFailure();
            } else if (currentBucket.isBlocked(now)) {
                nextBucket = currentBucket;
            } else {
                int failures = currentBucket.consecutiveFailures() + 1;
                nextBucket = failures >= maximumFailures
                        ? new FailureBucket(maximumFailures, now.plus(blockDuration))
                        : new FailureBucket(failures, null);
            }
            updatedBucket.set(nextBucket);
            return nextBucket;
        });

        FailureBucket bucket = updatedBucket.get();
        int remainingAttempts = Math.max(0, maximumFailures - bucket.consecutiveFailures());
        return new LoginAttemptResult(
                userId,
                bucket.consecutiveFailures(),
                remainingAttempts,
                bucket.blockedUntil());
    }

    public boolean isBlocked(Long userId) {
        if (userId == null) {
            return false;
        }

        FailureBucket bucket = bucketMap.get(userId);
        if (bucket == null) {
            return false;
        }

        Instant now = clock.instant();
        if (bucket.isExpired(now)) {
            bucketMap.remove(userId, bucket);
            return false;
        }
        return bucket.isBlocked(now);
    }

    public void reset(Long userId) {
        if (userId != null) {
            bucketMap.remove(userId);
        }
    }

    private Optional<User> findUser(String identifier) {
        if (!StringUtils.hasText(identifier)) {
            return Optional.empty();
        }

        String normalizedIdentifier = identifier.trim();
        if (normalizedIdentifier.matches("^[0-9]{10}$")) {
            return userRepository.findByMobileNo(normalizedIdentifier);
        }
        if (normalizedIdentifier.contains("@")) {
            return userRepository.findByEmailIgnoreCase(UserValidationUtil.normalizeEmail(normalizedIdentifier));
        }
        return Optional.empty();
    }

    public record LoginAttemptResult(
            Long userId,
            int consecutiveFailures,
            int remainingAttempts,
            Instant blockedUntil) {

        static LoginAttemptResult notTracked(int maximumFailures) {
            return new LoginAttemptResult(null, 0, maximumFailures, null);
        }

        public boolean isBlocked() {
            return blockedUntil != null;
        }
    }

    private record FailureBucket(int consecutiveFailures, Instant blockedUntil) {

        static FailureBucket firstFailure() {
            return new FailureBucket(1, null);
        }

        boolean isBlocked(Instant now) {
            return blockedUntil != null && now.isBefore(blockedUntil);
        }

        boolean isExpired(Instant now) {
            return blockedUntil != null && !now.isBefore(blockedUntil);
        }
    }
}
