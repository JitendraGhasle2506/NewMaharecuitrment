package com.maharecruitment.gov.in.web.dto.verification;

import jakarta.validation.constraints.NotBlank;
public class OtpSendRequest {

    @NotBlank(message = "Reference is required")
    private String reference;

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }
}
