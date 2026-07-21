package com.maharecruitment.gov.in.web.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.maharecruitment.gov.in.web.service.passwordreset.PasswordResetDeliveryChannel;

@Configuration
@ConfigurationProperties(prefix = "security.password-reset")
public class PasswordResetProperties {

    private int otpValidityMinutes = 5;
    private int maxAttempts = 5;
    private int resendCooldownSeconds = 60;
    private int tokenValidityMinutes = 10;
    private int maxOtpRequests = 3;
    private int requestWindowMinutes = 15;
    private int verifyRateLimit = 10;
    private int verifyRateWindowSeconds = 60;
    private String cleanupCron = "0 */15 * * * *";
    private PasswordResetDeliveryChannel deliveryChannel = PasswordResetDeliveryChannel.EMAIL;

    public int getOtpValidityMinutes() {
        return otpValidityMinutes;
    }

    public void setOtpValidityMinutes(int otpValidityMinutes) {
        this.otpValidityMinutes = otpValidityMinutes;
    }

    public int getOtpValiditySeconds() {
        return Math.max(1, otpValidityMinutes) * 60;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public int getResendCooldownSeconds() {
        return resendCooldownSeconds;
    }

    public void setResendCooldownSeconds(int resendCooldownSeconds) {
        this.resendCooldownSeconds = resendCooldownSeconds;
    }

    public int getTokenValidityMinutes() {
        return tokenValidityMinutes;
    }

    public void setTokenValidityMinutes(int tokenValidityMinutes) {
        this.tokenValidityMinutes = tokenValidityMinutes;
    }

    public int getTokenValiditySeconds() {
        return Math.max(1, tokenValidityMinutes) * 60;
    }

    public int getMaxOtpRequests() {
        return maxOtpRequests;
    }

    public void setMaxOtpRequests(int maxOtpRequests) {
        this.maxOtpRequests = maxOtpRequests;
    }

    public int getRequestWindowMinutes() {
        return requestWindowMinutes;
    }

    public void setRequestWindowMinutes(int requestWindowMinutes) {
        this.requestWindowMinutes = requestWindowMinutes;
    }

    public int getRequestWindowSeconds() {
        return Math.max(1, requestWindowMinutes) * 60;
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

    public String getCleanupCron() {
        return cleanupCron;
    }

    public void setCleanupCron(String cleanupCron) {
        this.cleanupCron = cleanupCron;
    }

    public PasswordResetDeliveryChannel getDeliveryChannel() {
        return deliveryChannel;
    }

    public void setDeliveryChannel(PasswordResetDeliveryChannel deliveryChannel) {
        this.deliveryChannel = deliveryChannel;
    }
}
