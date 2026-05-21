package com.maharecruitment.gov.in.web.dto.login;

import com.maharecruitment.gov.in.web.dto.verification.VerificationChannel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class OtpLoginForm {

    @NotBlank(message = "Username, email, or mobile number is required")
    private String identifier;

    @NotNull(message = "OTP delivery channel is required")
    private VerificationChannel channel;

    @NotBlank(message = "OTP is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "Invalid/Incorrect OTP")
    private String otp;

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public VerificationChannel getChannel() {
        return channel;
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
}
