package com.maharecruitment.gov.in.web.dto.login;

import com.maharecruitment.gov.in.web.dto.verification.VerificationChannel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class OtpLoginSendRequest {

    @NotBlank(message = "Username, email, or mobile number is required")
    private String identifier;

    @NotNull(message = "OTP delivery channel is required")
    private VerificationChannel channel;

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
}
