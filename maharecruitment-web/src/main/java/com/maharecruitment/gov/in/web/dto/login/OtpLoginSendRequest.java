package com.maharecruitment.gov.in.web.dto.login;

import java.util.Locale;

import com.maharecruitment.gov.in.web.dto.verification.VerificationChannel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class OtpLoginSendRequest {

    @NotBlank(message = "Username, email, or mobile number is required")
    private String identifier;

    @NotBlank(message = "Select Email OTP or Mobile OTP")
    @Pattern(regexp = "EMAIL|MOBILE", message = "Select Email OTP or Mobile OTP")
    private String channel;

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public VerificationChannel getChannel() {
        if (channel == null || channel.isBlank()) {
            return null;
        }

        try {
            return VerificationChannel.valueOf(channel);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public void setChannel(String channel) {
        this.channel = channel == null ? null : channel.trim().toUpperCase(Locale.ROOT);
    }

    public String getChannelValue() {
        return channel;
    }
}
