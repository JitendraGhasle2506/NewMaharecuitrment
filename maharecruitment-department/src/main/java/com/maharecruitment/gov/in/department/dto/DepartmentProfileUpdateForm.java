package com.maharecruitment.gov.in.department.dto;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DepartmentProfileUpdateForm {

    @NotBlank(message = "Office address is required.")
    @Size(max = 500, message = "Office address must not exceed 500 characters.")
    private String officeAddress;

    @NotBlank(message = "Department name for bill is required.")
    @Size(max = 200, message = "Department name for bill must not exceed 200 characters.")
    private String billDepartmentName;

    @NotBlank(message = "GST number is required.")
    @Pattern(
            regexp = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$",
            message = "GST number must be valid.")
    private String gstNumber;

    @NotBlank(message = "PAN number is required.")
    @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]$", message = "PAN number must be valid.")
    private String panNumber;

    @NotBlank(message = "TAN number is required.")
    @Pattern(regexp = "^[A-Z]{4}[0-9]{5}[A-Z]$", message = "TAN number must be valid.")
    private String tanNumber;

    @NotBlank(message = "Billing address is required.")
    @Size(max = 500, message = "Billing address must not exceed 500 characters.")
    private String billingAddress;

    @NotBlank(message = "Primary contact name is required.")
    @Size(max = 150, message = "Primary contact name must not exceed 150 characters.")
    private String primaryContactName;

    @NotBlank(message = "Primary designation is required.")
    @Size(max = 150, message = "Primary designation must not exceed 150 characters.")
    private String primaryDesignation;

    @NotBlank(message = "Primary mobile number is required.")
    @Pattern(regexp = "^[0-9]{10}$", message = "Primary mobile number must be 10 digits.")
    private String primaryMobileNumber;

    @NotBlank(message = "Primary email is required.")
    @Email(message = "Primary email must be valid.")
    @Size(max = 150, message = "Primary email must not exceed 150 characters.")
    private String primaryEmailAddress;

    @NotBlank(message = "Secondary contact name is required.")
    @Size(max = 150, message = "Secondary contact name must not exceed 150 characters.")
    private String secondaryContactName;

    @NotBlank(message = "Secondary designation is required.")
    @Size(max = 150, message = "Secondary designation must not exceed 150 characters.")
    private String secondaryDesignation;

    @NotBlank(message = "Secondary mobile number is required.")
    @Pattern(regexp = "^[0-9]{10}$", message = "Secondary mobile number must be 10 digits.")
    private String secondaryMobileNumber;

    @NotBlank(message = "Secondary email is required.")
    @Email(message = "Secondary email must be valid.")
    @Size(max = 150, message = "Secondary email must not exceed 150 characters.")
    private String secondaryEmailAddress;

    private MultipartFile gstDocumentFile;

    private MultipartFile panDocumentFile;

    private MultipartFile tanDocumentFile;

    private String departmentName;

    private String subDepartmentName;

    private String existingGstDocumentName;

    private String existingPanDocumentName;

    private String existingTanDocumentName;
}
