package com.maharecruitment.gov.in.common.mahaitprofile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MahaItProfileRequest {

        private Long mahaItProfileId;

        @NotBlank(message = "Profile name is required")
        @Size(max = 150, message = "Profile name must not exceed 150 characters")
        private String profileName = "MahaIT Profile";

        @NotBlank(message = "Company name is required")
        @Size(max = 200, message = "Company name must not exceed 200 characters")
        private String companyName;

        @NotBlank(message = "Company address is required")
        @Size(max = 1000, message = "Company address must not exceed 1000 characters")
        private String companyAddress;

        @NotBlank(message = "CIN number is required")
        @Pattern(regexp = "(?i)^[LU][0-9]{5}[A-Z]{2}[0-9]{4}[A-Z]{3}[0-9]{6}$", message = "CIN number format is invalid")
        @Size(max = 21, message = "CIN number must not exceed 21 characters")
        private String cinNumber;

        @NotBlank(message = "PAN number is required")
        @Pattern(regexp = "(?i)^[A-Z]{5}[0-9]{4}[A-Z]$", message = "PAN number format is invalid")
        private String panNumber;

        @NotBlank(message = "GST number is required")
        @Pattern(regexp = "(?i)^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$", message = "GST number format is invalid")
        private String gstNumber;

        @NotBlank(message = "Bank name is required")
        @Size(max = 150, message = "Bank name must not exceed 150 characters")
        private String bankName;

        @NotBlank(message = "Branch name is required")
        @Size(max = 150, message = "Branch name must not exceed 150 characters")
        private String branchName;

        @NotBlank(message = "Account holder name is required")
        @Size(max = 150, message = "Account holder name must not exceed 150 characters")
        private String accountHolderName;

        @NotBlank(message = "Account number is required")
        @Pattern(regexp = "^[0-9]{6,30}$", message = "Account number must be numeric")
        private String accountNumber;

        @NotBlank(message = "IFSC code is required")
        @Pattern(regexp = "(?i)^[A-Z]{4}0[A-Z0-9]{6}$", message = "IFSC code format is invalid")
        private String ifscCode;

        @NotNull(message = "Active flag is required")
        private Boolean active = true;
}
