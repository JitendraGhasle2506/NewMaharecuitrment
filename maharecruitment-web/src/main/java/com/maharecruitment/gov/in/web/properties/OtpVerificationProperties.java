package com.maharecruitment.gov.in.web.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "otp.verification")
public class OtpVerificationProperties {

    private int expirySeconds = 600;

    private int maxAttempts = 5;

    private int otpLength = 6;

    private int resendCooldownSeconds = 60;

    private int maxSendAttempts = 5;

    private int sendWindowSeconds = 600;

    public int getExpirySeconds() {
        return expirySeconds;
    }

    public void setExpirySeconds(int expirySeconds) {
        this.expirySeconds = expirySeconds;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public int getOtpLength() {
        return otpLength;
    }

    public void setOtpLength(int otpLength) {
        this.otpLength = otpLength;
    }

    public int getResendCooldownSeconds() {
        return resendCooldownSeconds;
    }

    public void setResendCooldownSeconds(int resendCooldownSeconds) {
        this.resendCooldownSeconds = resendCooldownSeconds;
    }

    public int getMaxSendAttempts() {
        return maxSendAttempts;
    }

    public void setMaxSendAttempts(int maxSendAttempts) {
        this.maxSendAttempts = maxSendAttempts;
    }

    public int getSendWindowSeconds() {
        return sendWindowSeconds;
    }

    public void setSendWindowSeconds(int sendWindowSeconds) {
        this.sendWindowSeconds = sendWindowSeconds;
    }
}
