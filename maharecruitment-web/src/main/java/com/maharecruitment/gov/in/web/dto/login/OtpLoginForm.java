package com.maharecruitment.gov.in.web.dto.login;

import com.maharecruitment.gov.in.web.dto.verification.VerificationChannel;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class OtpLoginForm {

    @NotBlank(message = "Email or mobile number is required")
    private String identifier;

    private VerificationChannel channel;

    @NotBlank(message = "OTP is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "Invalid/Incorrect OTP")
    private String otp;

    private String captchaId;

    private String captchaAnswer;

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier == null ? null : identifier.trim();
    }

    public VerificationChannel getChannel() {
        VerificationChannel inferredChannel = LoginIdentifierSupport.inferChannel(identifier);
        return inferredChannel == null ? channel : inferredChannel;
    }

    public void setChannel(VerificationChannel channel) {
        this.channel = channel;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public String getCaptchaId() {
        return captchaId;
    }

    public void setCaptchaId(String captchaId) {
        this.captchaId = captchaId;
    }

    public String getCaptchaAnswer() {
        return captchaAnswer;
    }

    public void setCaptchaAnswer(String captchaAnswer) {
        this.captchaAnswer = captchaAnswer;
    }

    @AssertTrue(message = "Enter a valid email address or 10 digit mobile number")
    public boolean isIdentifierFormatValid() {
        if (identifier == null || identifier.isBlank()) {
            return true;
        }
        return LoginIdentifierSupport.isEmailOrMobile(identifier);
    }
}
