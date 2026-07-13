package com.maharecruitment.gov.in.web.dto.login;

import java.util.Locale;

import com.maharecruitment.gov.in.web.dto.verification.VerificationChannel;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;

public class OtpLoginSendRequest {

    @NotBlank(message = "Email or mobile number is required")
    private String identifier;

    private String channel;

    private String deliveryChannel;

    private String purpose;

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
