package com.maharecruitment.gov.in.web.dto.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordChangeForm {

    @NotBlank(message = "Current password is required")
    @Size(max = 100, message = "Current password must not exceed 100 characters")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 100, message = "New password must be between 8 and 100 characters")
    private String newPassword;

    @NotBlank(message = "Confirm password is required")
    @Size(max = 100, message = "Confirm password must not exceed 100 characters")
    private String confirmPassword;
}
