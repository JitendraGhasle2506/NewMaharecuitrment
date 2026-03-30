package com.maharecruitment.gov.in.web.dto.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserProfileForm {

    @NotBlank(message = "Name is required")
    @Size(max = 150, message = "Name must not exceed 150 characters")
    private String name;

    @Pattern(regexp = "^$|^[0-9]{10,15}$", message = "Mobile number must be 10 to 15 digits")
    private String mobileNo;
}
