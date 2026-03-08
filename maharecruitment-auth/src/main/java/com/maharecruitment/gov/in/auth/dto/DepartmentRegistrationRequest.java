package com.maharecruitment.gov.in.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DepartmentRegistrationRequest {

    @NotNull(message = "Department id is required")
    private Long departmentId;

    @NotNull(message = "Sub-department id is required")
    private Long subDeptId;

    @NotBlank(message = "Department name is required")
    @Size(max = 200, message = "Department name must not exceed 200 characters")
    private String departmentName;

    @NotBlank(message = "Office address is required")
    @Size(max = 500, message = "Office address must not exceed 500 characters")
    private String address;

    @NotBlank(message = "Department name for bill is required")
    @Size(max = 200, message = "Bill department name must not exceed 200 characters")
    private String billDepartmentName;

    @NotBlank(message = "GST number is required")
    @Pattern(
            regexp = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$",
            message = "GST number must be valid")
    private String gstNo;

    @NotBlank(message = "PAN number is required")
    @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]$", message = "PAN number must be valid")
    private String panNo;

    @NotBlank(message = "TAN number is required")
    @Pattern(regexp = "^[A-Z]{4}[0-9]{5}[A-Z]$", message = "TAN number must be valid")
    private String tanNo;

    @NotBlank(message = "Billing address is required")
    @Size(max = 500, message = "Billing address must not exceed 500 characters")
    private String billAddress;

    private String gstFilePath;

    private String panFilePath;

    private String tanFilePath;

    @NotNull(message = "Terms and conditions acceptance is required")
    @AssertTrue(message = "You must accept the terms and conditions")
    private Boolean termsConditionAccepted;

    @Valid
    @NotNull(message = "Primary contact is required")
    private DepartmentContactRequest primaryContact;

    @Valid
    @NotNull(message = "Secondary contact is required")
    private DepartmentContactRequest secondaryContact;
}
