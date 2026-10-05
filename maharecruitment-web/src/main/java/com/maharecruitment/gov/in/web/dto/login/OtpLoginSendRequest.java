package com.maharecruitment.gov.in.web.dto.login;

import java.util.Locale;

import com.maharecruitment.gov.in.web.dto.verification.VerificationChannel;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class OtpLoginSendRequest {

    @NotBlank(message = "Email or mobile number is required")
    private String identifier;

    private String channel;

    private String deliveryChannel;

    private String purpose;

    @NotBlank(message = "CAPTCHA is required")
    @Size(max = 36, message = "CAPTCHA identifier is invalid")
    private String loginCaptchaId;

    @NotBlank(message = "CAPTCHA answer is required")
    @Pattern(regexp = "[A-Za-z0-9]{6}", message = "Enter all 6 CAPTCHA characters")
    private String loginCaptchaAnswer;

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier == null ? null : identifier.trim();
    }

    public VerificationChannel getChannel() {
        VerificationChannel inferredChannel = LoginIdentifierSupport.inferChannel(identifier);
        if (inferredChannel != null) {
            return inferredChannel;
        }

        String selectedChannel = selectedChannel();
        if (selectedChannel == null || selectedChannel.isBlank()) {
            return null;
        }

        try {
            return VerificationChannel.valueOf(selectedChannel);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public void setChannel(String channel) {
        this.channel = channel == null ? null : channel.trim().toUpperCase(Locale.ROOT);
    }

    public String getDeliveryChannel() {
        return deliveryChannel;
    }

    public void setDeliveryChannel(String deliveryChannel) {
        this.deliveryChannel = deliveryChannel == null ? null : deliveryChannel.trim().toUpperCase(Locale.ROOT);
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose == null ? null : purpose.trim().toUpperCase(Locale.ROOT);
    }

    public String getChannelValue() {
        VerificationChannel channel = getChannel();
        return channel == null ? selectedChannel() : channel.name();
    }

    public String getLoginCaptchaId() {
        return loginCaptchaId;
    }

    public void setLoginCaptchaId(String loginCaptchaId) {
        this.loginCaptchaId = loginCaptchaId == null ? null : loginCaptchaId.trim();
    }

    public String getLoginCaptchaAnswer() {
        return loginCaptchaAnswer;
    }

    public void setLoginCaptchaAnswer(String loginCaptchaAnswer) {
        this.loginCaptchaAnswer = loginCaptchaAnswer == null ? null : loginCaptchaAnswer.trim();
    }

    @AssertTrue(message = "Enter a valid email address or 10 digit mobile number")
    public boolean isIdentifierFormatValid() {
        if (identifier == null || identifier.isBlank()) {
            return true;
        }
        return LoginIdentifierSupport.isEmailOrMobile(identifier);
    }

    private String selectedChannel() {
        return deliveryChannel != null && !deliveryChannel.isBlank() ? deliveryChannel : channel;
    }
}
