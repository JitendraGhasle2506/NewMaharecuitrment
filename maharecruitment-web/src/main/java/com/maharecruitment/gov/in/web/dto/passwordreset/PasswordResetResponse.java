package com.maharecruitment.gov.in.web.dto.passwordreset;

public class PasswordResetResponse {

    private boolean success;
    private String message;
    private String resetToken;
    private Long expiresInSeconds;
    private String maskedDestination;

    public PasswordResetResponse() {
    }

    public PasswordResetResponse(
            boolean success,
            String message,
            String resetToken,
            Long expiresInSeconds,
            String maskedDestination) {
        this.success = success;
        this.message = message;
        this.resetToken = resetToken;
        this.expiresInSeconds = expiresInSeconds;
        this.maskedDestination = maskedDestination;
    }

    public static PasswordResetResponse accepted(String message) {
        return new PasswordResetResponse(true, message, null, null, null);
    }

    public static PasswordResetResponse tokenIssued(String message, String resetToken, long expiresInSeconds) {
        return new PasswordResetResponse(true, message, resetToken, expiresInSeconds, null);
    }

    public static PasswordResetResponse completed(String message) {
        return new PasswordResetResponse(true, message, null, null, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getResetToken() {
        return resetToken;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    public Long getExpiresInSeconds() {
        return expiresInSeconds;
    }

    public void setExpiresInSeconds(Long expiresInSeconds) {
        this.expiresInSeconds = expiresInSeconds;
    }

    public String getMaskedDestination() {
        return maskedDestination;
    }

    public void setMaskedDestination(String maskedDestination) {
        this.maskedDestination = maskedDestination;
    }
}
