package com.maharecruitment.gov.in.web.dto.mobile;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record MobileProfileContactUpdateRequest(
        @NotNull(message = "Employee ID is required.")
        Long employeeId,

        @Email(message = "Email address must be valid.")
        String email,

        @Pattern(regexp = "^[0-9]{10,15}$", message = "Mobile number must be 10 to 15 digits.")
        String mobileNo) {
}
