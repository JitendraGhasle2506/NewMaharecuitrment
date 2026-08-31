package com.maharecruitment.gov.in.web.dto.employee;

import java.time.LocalDate;

import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmployeeProfileDTO {

    private Long id;

    private Long employeeId;

    private String fullName;

    private LocalDate dob;

    private String gender;

    private String alternateMobileNo;

    private String email;

    private String panNo;

    @Size(max = 800, message = "Encrypted PAN number is invalid")
    private String panNoEncrypted;

    @Size(max = 100, message = "Encryption key identifier is invalid")
    private String encryptionKeyId;

    private Long timestamp;

    @Pattern(regexp = "^[A-Za-z0-9_-]{22,128}$", message = "Encrypted request nonce is invalid")
    private String nonce;

    private String maritalStatus;

    @Size(max = 100, message = "Spouse / partner name must not exceed 100 characters")
    private String spouseName;

    @PastOrPresent(message = "Marriage date cannot be in the future")
    private LocalDate marriageDate;

    private String bloodGroup;

    private String emergencyContactName;

    private String emergencyContactNo;

    private String currentAddress;

    private String permanentAddress;

    private String employeeCode;

    private String role;

    private String department;

    private String mobileNo;

    private String photoUrl;

    private boolean profileAvailable;

    private int completionPercentage;

    public void clearEncryptedSubmission() {
        panNoEncrypted = null;
        encryptionKeyId = null;
        timestamp = null;
        nonce = null;
        panNo = null;
    }
}
