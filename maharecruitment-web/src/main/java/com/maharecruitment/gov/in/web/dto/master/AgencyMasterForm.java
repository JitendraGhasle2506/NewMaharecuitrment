package com.maharecruitment.gov.in.web.dto.master;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.maharecruitment.gov.in.master.entity.AgencyBankAccountType;
import com.maharecruitment.gov.in.master.entity.AgencyEntityType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AgencyMasterForm {

    @NotBlank(message = "Agency name is required")
    @Size(max = 200, message = "Agency name must not exceed 200 characters")
    private String agencyName;

    @NotBlank(message = "Agency official email id is required")
    @Email(message = "Agency official email id must be valid")
    @Size(max = 255, message = "Agency official email id must not exceed 255 characters")
    private String officialEmail;

    @NotBlank(message = "Telephone number is required")
    @Pattern(regexp = "^[0-9]{10,15}$", message = "Telephone number must be numeric and between 10 to 15 digits")
    private String telephoneNumber;

    @NotBlank(message = "Agency type is required")
    @Size(max = 100, message = "Agency type must not exceed 100 characters")
    private String agencyType;

    @NotBlank(message = "Agency official location address is required")
    @Size(max = 500, message = "Agency official location address must not exceed 500 characters")
    private String officialAddress;

    @NotBlank(message = "Permanent office location is required")
    @Size(max = 500, message = "Permanent office location must not exceed 500 characters")
    private String permanentAddress;

    @NotNull(message = "Entity type is required")
    private AgencyEntityType entityType;

    @NotBlank(message = "PAN number is required")
    @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]$", message = "PAN number format is invalid")
    private String panNumber;

    @NotNull(message = "PAN Copy File is Required")
    private MultipartFile panCopyFile;

    @NotBlank(message = "Certificate of incorporation number is required")
    @Size(max = 100, message = "Certificate number must not exceed 100 characters")
    private String certificateNumber;

    @NotNull(message = "Certificate Document File is required")
    private MultipartFile certificateDocumentFile;

    @NotBlank(message = "GST number is required")
    @Pattern(
            regexp = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$",
            message = "GST number format is invalid")
    private String gstNumber;

    @NotNull(message = "GST File is Required")
    private MultipartFile gstDocumentFile;

    @NotBlank(message = "Contact person name is required")
    @Pattern(regexp = "^[A-Za-z .'-]+$", message = "Contact person name must contain alphabetic characters only")
    @Size(max = 150, message = "Contact person name must not exceed 150 characters")
    private String contactPersonName;

    @NotBlank(message = "Contact person mobile number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Contact person mobile number must be 10 digits")
    private String contactPersonMobileNo;

    @NotNull(message = "MSME registration flag is required")
    private Boolean msmeRegistered;

    private boolean sameAsOfficialAddress;

    @Valid
    private List<AgencyEscalationMatrixForm> escalationMatrixEntries = new ArrayList<>();

    @NotBlank(message = "Bank name is required")
    @Size(max = 150, message = "Bank name must not exceed 150 characters")
    private String bankName;

    @NotBlank(message = "Bank branch is required")
    @Size(max = 150, message = "Bank branch must not exceed 150 characters")
    private String bankBranch;

    @NotBlank(message = "Bank account number is required")
    @Pattern(regexp = "^[0-9]{9,30}$", message = "Bank account number must be numeric")
    private String bankAccountNumber;

    @NotNull(message = "Bank account type is required")
    private AgencyBankAccountType bankAccountType;

    @NotBlank(message = "IFSC code is required")
    @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$", message = "IFSC code format is invalid")
    private String ifscCode;

    @NotNull(message = "Cancelled Cheque File is required")
    private MultipartFile cancelledChequeFile;

    private String existingPanCopyPath;
    private String existingCertificateDocumentPath;
    private String existingGstDocumentPath;
    private String existingCancelledChequePath;
}
