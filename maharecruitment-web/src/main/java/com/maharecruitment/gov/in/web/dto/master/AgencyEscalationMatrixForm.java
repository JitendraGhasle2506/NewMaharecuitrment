package com.maharecruitment.gov.in.web.dto.master;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AgencyEscalationMatrixForm {

    @NotBlank(message = "Escalation contact name is required")
    @Pattern(regexp = "^[A-Za-z .'-]+$", message = "Escalation contact name must contain alphabetic characters only")
    @Size(max = 150, message = "Escalation contact name must not exceed 150 characters")
    private String contactName;

    @NotBlank(message = "Escalation contact mobile number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Escalation contact mobile number must be 10 digits")
    private String mobileNumber;

    @NotBlank(message = "Escalation level is required")
    @Pattern(regexp = "^(L1|L2|L3)$", message = "Escalation level must be one of L1, L2 or L3")
    private String level;

    @NotBlank(message = "Escalation designation is required")
    @Size(max = 100, message = "Escalation designation must not exceed 100 characters")
    private String designation;

    @NotBlank(message = "Escalation company email id is required")
    @Email(message = "Escalation company email id must be valid")
    @Size(max = 255, message = "Escalation company email id must not exceed 255 characters")
    private String companyEmailId;
}
