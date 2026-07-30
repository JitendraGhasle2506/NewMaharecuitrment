package com.maharecruitment.gov.in.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AgencyUserProvisioningRequest {

    @NotBlank(message = "Agency contact name is required")
    @Size(max = 255, message = "Agency contact name must not exceed 255 characters")
    private String name;

    @NotBlank(message = "Agency official email is required")
    @Email(message = "Agency official email must be valid")
    @Size(max = 255, message = "Agency official email must not exceed 255 characters")
    private String email;

    @NotBlank(message = "Agency contact mobile number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Agency contact mobile number must be 10 digits")
    private String mobileNo;

    private Long agencyId;

    @Email(message = "Previous agency official email must be valid")
    @Size(max = 255, message = "Previous agency official email must not exceed 255 characters")
    private String previousEmail;
}
