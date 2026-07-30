package com.maharecruitment.gov.in.web.service.verification;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        OtpRateLimiter limiter = new OtpRateLimiter(properties);
        OtpRequestContext context = new OtpRequestContext("127.0.0.1");

        limiter.checkSendAllowed("login", VerificationChannel.SMS, "7020186501", context);
        limiter.checkSendAllowed("login", VerificationChannel.SMS, "7020186501", context);

        assertThatThrownBy(() ->
                limiter.checkSendAllowed("login", VerificationChannel.SMS, "7020186501", context))
                .isInstanceOf(OtpRateLimitException.class)
                .hasMessageContaining("OTP send rate limit exceeded");
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
}
