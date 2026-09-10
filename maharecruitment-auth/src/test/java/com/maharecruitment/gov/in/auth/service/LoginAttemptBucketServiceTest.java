package com.maharecruitment.gov.in.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserRepository;

class LoginAttemptBucketServiceTest {

    @Test
    void blocksSameUserForTwentyFourHoursAfterThreeFailuresAcrossAliases() {
        UserRepository userRepository = mock(UserRepository.class);
        User user = user();
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(userRepository.findByMobileNo("9876543210")).thenReturn(Optional.of(user));
        MutableClock clock = new MutableClock(Instant.parse("2026-09-10T08:00:00Z"));
        LoginAttemptBucketService service = new LoginAttemptBucketService(
                userRepository,
                3,
                Duration.ofHours(24),
                clock);

        assertThat(service.recordFailure("USER@example.com").remainingAttempts()).isEqualTo(2);
        assertThat(service.recordFailure("9876543210").remainingAttempts()).isEqualTo(1);
        LoginAttemptBucketService.LoginAttemptResult thirdFailure = service.recordFailure("user@example.com");

        assertThat(thirdFailure.isBlocked()).isTrue();
        assertThat(thirdFailure.blockedUntil()).isEqualTo(Instant.parse("2026-09-11T08:00:00Z"));
        assertThat(service.isBlocked(user.getId())).isTrue();

        clock.advance(Duration.ofHours(24));
        assertThat(service.isBlocked(user.getId())).isFalse();
    }

    @Test
    void successfulLoginClearsConsecutiveFailures() {
        UserRepository userRepository = mock(UserRepository.class);
        User user = user();
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        LoginAttemptBucketService service = new LoginAttemptBucketService(
                userRepository,
                3,
                Duration.ofHours(24),
                Clock.systemUTC());

        service.recordFailure("user@example.com");
        service.recordFailure("user@example.com");
        service.reset(user.getId());

        assertThat(service.recordFailure("user@example.com").consecutiveFailures()).isEqualTo(1);
        assertThat(service.isBlocked(user.getId())).isFalse();
    }

    @Test
    void doesNotAllocateBucketsForUnknownIdentifiers() {
        UserRepository userRepository = mock(UserRepository.class);
        when(userRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());
        LoginAttemptBucketService service = new LoginAttemptBucketService(
                userRepository,
                3,
                Duration.ofHours(24),
                Clock.systemUTC());

        LoginAttemptBucketService.LoginAttemptResult result = service.recordFailure("missing@example.com");

        assertThat(result.userId()).isNull();
        assertThat(result.isBlocked()).isFalse();
        assertThat(result.remainingAttempts()).isEqualTo(3);
    }

    private User user() {
        User user = new User();
        user.setId(42L);
        user.setEmail("user@example.com");
        user.setMobileNo("9876543210");
        return user;
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }
    }
}
