package com.maharecruitment.gov.in.web.dto.admin;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserForm {

    @NotBlank(message = "Name is required")
    @Size(max = 150, message = "Name must not exceed 150 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @Pattern(regexp = "^$|^[0-9]{10,15}$", message = "Mobile number must be 10 to 15 digits")
    private String mobileNo;

    @Size(max = 100, message = "Password must not exceed 100 characters")
    private String password;

    private Long departmentRegistrationId;

    private List<Long> roleIds = new ArrayList<>();
}
