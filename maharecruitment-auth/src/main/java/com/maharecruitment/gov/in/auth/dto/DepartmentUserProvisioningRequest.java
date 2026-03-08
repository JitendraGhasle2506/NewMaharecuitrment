package com.maharecruitment.gov.in.auth.dto;

import com.maharecruitment.gov.in.auth.entity.DepartmentRegistrationEntity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DepartmentUserProvisioningRequest {

    @NotBlank(message = "User name is required")
    @Size(max = 255, message = "User name must not exceed 255 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be 10 digits")
    private String mobileNo;

    @NotNull(message = "Department registration is required")
    private DepartmentRegistrationEntity departmentRegistration;
}
