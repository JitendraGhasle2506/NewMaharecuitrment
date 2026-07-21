package com.maharecruitment.gov.in.web.dto.passwordreset;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ResetPasswordRequest {

    @NotBlank(message = "Reset token is required.")
    @Size(max = 512, message = "Reset token is invalid.")
    private String resetToken;

    @NotBlank(message = "New password is required.")
    @Size(min = 8, max = 100, message = "New password must be between 8 and 100 characters.")
    private String newPassword;

    @NotBlank(message = "Confirm password is required.")
    @Size(max = 100, message = "Confirm password must not exceed 100 characters.")
    private String confirmPassword;

    public String getResetToken() {
        return resetToken;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
