package com.maharecruitment.gov.in.web.service.verification;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;

import com.maharecruitment.gov.in.web.dto.verification.VerificationChannel;
import com.maharecruitment.gov.in.web.properties.OtpVerificationProperties;

class OtpRateLimiterTest {

    @Test
    void sameIpCanSendOtpForDifferentReferencesBeyondResendLimit() {
        OtpVerificationProperties properties = new OtpVerificationProperties();
        properties.setResendLimit(3);
        properties.setSendIpLimit(100);
        properties.setResendWindowMinutes(5);
        OtpRateLimiter limiter = new OtpRateLimiter(properties);
        OtpRequestContext context = new OtpRequestContext("127.0.0.1");

        assertThatCode(() -> {
            limiter.checkSendAllowed("login", VerificationChannel.SMS, "7020186501", context);
            limiter.checkSendAllowed("login", VerificationChannel.SMS, "7020186502", context);
            limiter.checkSendAllowed("login", VerificationChannel.SMS, "7020186503", context);
            limiter.checkSendAllowed("login", VerificationChannel.SMS, "7020186504", context);
        }).doesNotThrowAnyException();
    }

    @Test
    void sameReferenceStillUsesStrictResendLimit() {
        OtpVerificationProperties properties = new OtpVerificationProperties();
        properties.setResendLimit(2);
        properties.setSendIpLimit(100);
        properties.setResendWindowMinutes(5);
        properties.setResendCooldownSeconds(0);
        OtpRateLimiter limiter = new OtpRateLimiter(properties);
        OtpRequestContext context = new OtpRequestContext("127.0.0.1");

        limiter.checkSendAllowed("login", VerificationChannel.SMS, "7020186501", context);
        limiter.checkSendAllowed("login", VerificationChannel.SMS, "7020186501", context);

        assertThatThrownBy(() ->
                limiter.checkSendAllowed("login", VerificationChannel.SMS, "7020186501", context))
                .isInstanceOf(OtpRateLimitException.class)
                .hasMessageContaining("OTP send rate limit exceeded")
                .satisfies(exception -> assertThat(((OtpRateLimitException) exception).getResponseCode())
                        .isEqualTo(OtpResponseCodes.OTP_RESEND_LIMIT_EXCEEDED));
    }

    @Test
    void ipLimitCanStillStopHighVolumeFromOneClient() {
        OtpVerificationProperties properties = new OtpVerificationProperties();
        properties.setResendLimit(1);
        properties.setSendIpLimit(2);
        properties.setResendWindowMinutes(5);
        OtpRateLimiter limiter = new OtpRateLimiter(properties);
        OtpRequestContext context = new OtpRequestContext("127.0.0.1");

        limiter.checkSendAllowed("login", VerificationChannel.SMS, "7020186501", context);
        limiter.checkSendAllowed("login", VerificationChannel.SMS, "7020186502", context);

        assertThatThrownBy(() ->
                limiter.checkSendAllowed("login", VerificationChannel.SMS, "7020186503", context))
                .isInstanceOf(OtpRateLimitException.class)
                .hasMessageContaining("OTP send rate limit exceeded");
    }

    @Test
    void concurrentEmailRequestsReserveOnlyOneSendDuringCooldown() throws Exception {
        OtpVerificationProperties properties = new OtpVerificationProperties();
        properties.setResendLimit(20);
        properties.setSendIpLimit(100);
        properties.setResendWindowMinutes(5);
        properties.setResendCooldownSeconds(30);
        OtpRateLimiter limiter = new OtpRateLimiter(properties);
        OtpRequestContext context = new OtpRequestContext("127.0.0.1");
        int concurrentRequests = 12;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentRequests);
        CountDownLatch start = new CountDownLatch(1);

        try {
            List<Future<Boolean>> results = new ArrayList<>();
            for (int index = 0; index < concurrentRequests; index++) {
                results.add(executor.submit(() -> {
                    start.await();
                    try {
                        limiter.checkSendAllowed(
                                "department-registration",
                                VerificationChannel.EMAIL,
                                "user@example.com",
                                context);
                        return true;
                    } catch (OtpRateLimitException ex) {
                        return false;
                    }
                }));
            }

            start.countDown();
            long acceptedRequests = 0;
            for (Future<Boolean> result : results) {
                if (result.get()) {
                    acceptedRequests++;
                }
            }
            assertThat(acceptedRequests).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void recipientCooldownCannotBeBypassedFromAnotherSystem() {
        OtpVerificationProperties properties = new OtpVerificationProperties();
        properties.setResendLimit(20);
        properties.setSendRecipientLimit(20);
        properties.setSendIpLimit(100);
        properties.setResendCooldownSeconds(120);
        OtpRateLimiter limiter = new OtpRateLimiter(properties);

        limiter.checkSendAllowed(
                "login-authentication",
                VerificationChannel.EMAIL,
                "user@example.com",
                new OtpRequestContext("192.0.2.10"));

        assertThatThrownBy(() -> limiter.checkSendAllowed(
                "department-registration-primary-contact",
                VerificationChannel.EMAIL,
                "user@example.com",
                new OtpRequestContext("198.51.100.20")))
                .isInstanceOf(OtpRateLimitException.class)
                .satisfies(exception -> {
                    OtpRateLimitException rateLimitException = (OtpRateLimitException) exception;
                    assertThat(rateLimitException.getResponseCode())
                            .isEqualTo(OtpResponseCodes.OTP_RESEND_COOLDOWN);
                    assertThat(rateLimitException.getRetryAfterSeconds()).isBetween(1L, 120L);
                });
    }

    @Test
    void recipientLimitCannotBeBypassedByChangingPurposeOrChannel() {
        OtpVerificationProperties properties = new OtpVerificationProperties();
        properties.setResendLimit(20);
        properties.setSendRecipientLimit(2);
        properties.setSendRecipientWindowMinutes(15);
        properties.setSendIpLimit(100);
        properties.setResendCooldownSeconds(0);
        OtpRateLimiter limiter = new OtpRateLimiter(properties);
        OtpRequestContext context = new OtpRequestContext("127.0.0.1");

        limiter.checkSendAllowed(
                "login-authentication",
                VerificationChannel.EMAIL,
                "user@example.com",
                context);
        limiter.checkSendAllowed(
                "password-reset",
                VerificationChannel.BOTH,
                OtpDeliveryReferences.both("user@example.com", "7020186501"),
                context);

        assertThatThrownBy(() -> limiter.checkSendAllowed(
                "department-registration-primary-contact",
                VerificationChannel.EMAIL,
                "user@example.com",
                context))
                .isInstanceOf(OtpRateLimitException.class)
                .hasMessageContaining("OTP send rate limit exceeded");
    }
}
