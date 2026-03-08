package com.maharecruitment.gov.in.web.dto.verification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class OtpSendRequest {

    @NotBlank(message = "Purpose is required")
    private String purpose;

    @NotNull(message = "Channel is required")
    private VerificationChannel channel;

    @NotBlank(message = "Reference is required")
    private String reference;

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public VerificationChannel getChannel() {
        return channel;
    }

    public void setChannel(VerificationChannel channel) {
        this.channel = channel;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }
}
