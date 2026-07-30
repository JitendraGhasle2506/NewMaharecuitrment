package com.maharecruitment.gov.in.web.dto.passwordreset;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class PasswordResetOtpVerifyRequest {

    @NotBlank(message = "Username, email, mobile number, or employee code is required.")
    @Size(max = 255, message = "Identifier must not exceed 255 characters.")
    private String identifier;

    @NotBlank(message = "OTP is required.")
    @Pattern(regexp = "\\d{6}", message = "OTP must be exactly 6 digits.")
    private String otp;

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }
}
