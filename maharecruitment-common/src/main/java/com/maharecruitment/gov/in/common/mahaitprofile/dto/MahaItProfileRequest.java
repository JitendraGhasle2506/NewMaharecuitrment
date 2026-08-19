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

        private String cinNumber;

        private String panNumber;

        private String gstNumber;

        @Size(max = 800, message = "Encrypted CIN number is invalid")
        private String cinNumberEncrypted;

        @Size(max = 800, message = "Encrypted PAN number is invalid")
        private String panNumberEncrypted;

        @Size(max = 800, message = "Encrypted GST number is invalid")
        private String gstNumberEncrypted;

        @NotBlank(message = "Bank name is required")
        @Size(max = 150, message = "Bank name must not exceed 150 characters")
        private String bankName;

        @NotBlank(message = "Branch name is required")
        @Size(max = 150, message = "Branch name must not exceed 150 characters")
        private String branchName;

        @NotBlank(message = "Account holder name is required")
        @Size(max = 150, message = "Account holder name must not exceed 150 characters")
        private String accountHolderName;

        private String accountNumber;

        @Size(max = 800, message = "Encrypted account number is invalid")
        private String accountNumberEncrypted;

        @Size(max = 100, message = "Encryption key identifier is invalid")
        private String encryptionKeyId;

        private Long timestamp;

        @Pattern(regexp = "^[A-Za-z0-9_-]{22,128}$", message = "Encrypted request nonce is invalid")
        private String nonce;

        private String ifscCode;

        @Size(max = 800, message = "Encrypted IFSC code is invalid")
        private String ifscCodeEncrypted;

        @NotNull(message = "Active flag is required")
        private Boolean active = true;

        public void clearEncryptedSubmission() {
                cinNumberEncrypted = null;
                panNumberEncrypted = null;
                gstNumberEncrypted = null;
                accountNumberEncrypted = null;
                ifscCodeEncrypted = null;
                encryptionKeyId = null;
                timestamp = null;
                nonce = null;
        }
}
