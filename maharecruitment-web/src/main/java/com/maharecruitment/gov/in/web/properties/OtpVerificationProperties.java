package com.maharecruitment.gov.in.web.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "otp")
public class OtpVerificationProperties {

    private int expiryMinutes = 5;

    private int maxAttempts = 5;

    private int lockDurationMinutes = 15;

    private int resendLimit = 3;

    private int sendIpLimit = 10;

    private int sendRecipientLimit = 3;

    private int sendRecipientWindowMinutes = 15;

    private int resendWindowMinutes = 15;

    private int verifyRateLimit = 10;

    private int verifyRateWindowSeconds = 60;

    private int captchaThreshold = 3;

    private int otpLength = 6;

    private int resendCooldownSeconds = 30;

    public int getExpirySeconds() {
        return Math.max(1, expiryMinutes) * 60;
    }

    public int getExpiryMinutes() {
        return expiryMinutes;
    }

    public void setExpiryMinutes(int expiryMinutes) {
        this.expiryMinutes = expiryMinutes;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public int getLockDurationMinutes() {
        return lockDurationMinutes;
    }

    public void setLockDurationMinutes(int lockDurationMinutes) {
        this.lockDurationMinutes = lockDurationMinutes;
    }

    public int getResendLimit() {
        return resendLimit;
    }

    public void setResendLimit(int resendLimit) {
        this.resendLimit = resendLimit;
    }

    public int getSendIpLimit() {
        return sendIpLimit;
    }

    public void setSendIpLimit(int sendIpLimit) {
        this.sendIpLimit = sendIpLimit;
    }

    public int getSendRecipientLimit() {
        return sendRecipientLimit;
    }

    public void setSendRecipientLimit(int sendRecipientLimit) {
        this.sendRecipientLimit = sendRecipientLimit;
    }

    public int getSendRecipientWindowMinutes() {
        return sendRecipientWindowMinutes;
    }

    public void setSendRecipientWindowMinutes(int sendRecipientWindowMinutes) {
        this.sendRecipientWindowMinutes = sendRecipientWindowMinutes;
    }

    public int getResendWindowMinutes() {
        return resendWindowMinutes;
    }

    public void setResendWindowMinutes(int resendWindowMinutes) {
        this.resendWindowMinutes = resendWindowMinutes;
    }

    public int getVerifyRateLimit() {
        return verifyRateLimit;
    }

    public void setVerifyRateLimit(int verifyRateLimit) {
        this.verifyRateLimit = verifyRateLimit;
    }

    public int getVerifyRateWindowSeconds() {
        return verifyRateWindowSeconds;
    }

    public void setVerifyRateWindowSeconds(int verifyRateWindowSeconds) {
        this.verifyRateWindowSeconds = verifyRateWindowSeconds;
    }

    public int getCaptchaThreshold() {
        return captchaThreshold;
    }

    public void setCaptchaThreshold(int captchaThreshold) {
        this.captchaThreshold = captchaThreshold;
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
        return resendLimit;
    }

    public int getSendWindowSeconds() {
        return Math.max(1, resendWindowMinutes) * 60;
    }

    public int getLockDurationSeconds() {
        return Math.max(1, lockDurationMinutes) * 60;
    }
}
