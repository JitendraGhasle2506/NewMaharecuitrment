package com.maharecruitment.gov.in.web.dto.passwordreset;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PasswordResetOtpRequest {

    @NotBlank(message = "Username, email, mobile number, or employee code is required.")
    @Size(max = 255, message = "Identifier must not exceed 255 characters.")
    private String identifier;

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
}
