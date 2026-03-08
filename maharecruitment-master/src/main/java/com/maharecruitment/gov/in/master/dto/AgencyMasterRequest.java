package com.maharecruitment.gov.in.master.dto;

import java.util.ArrayList;
import java.util.List;

import com.maharecruitment.gov.in.master.entity.AgencyBankAccountType;
import com.maharecruitment.gov.in.master.entity.AgencyEntityType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AgencyMasterRequest {

    @NotBlank(message = "MSG701: Agency name is required")
    @Size(max = 200, message = "Agency name must not exceed 200 characters")
    private String agencyName;

    @NotBlank(message = "MSG702: Agency official email id is required")
    @Email(message = "MSG702: Agency official email id must be valid")
    @Size(max = 255, message = "Agency official email id must not exceed 255 characters")
    private String officialEmail;

    @NotBlank(message = "MSG703: Telephone number is required")
    @Pattern(regexp = "^[0-9]{10,15}$", message = "MSG703: Telephone number must be numeric and between 10 to 15 digits")
    private String telephoneNumber;

    @NotBlank(message = "MSG704: Agency type is required")
    @Size(max = 100, message = "Agency type must not exceed 100 characters")
    private String agencyType;

    @NotBlank(message = "MSG705: Agency official location address is required")
    @Size(max = 500, message = "Agency official location address must not exceed 500 characters")
    private String officialAddress;

    @NotBlank(message = "MSG706: Permanent office location is required")
    @Size(max = 500, message = "Permanent office location must not exceed 500 characters")
    private String permanentAddress;

    @NotNull(message = "MSG707: Entity type is required")
    private AgencyEntityType entityType;

    @NotBlank(message = "MSG708: PAN number is required")
    @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]$", message = "MSG708: PAN number format is invalid")
    private String panNumber;

    @NotBlank(message = "MSG709: PAN copy path is required")
    @Size(max = 500, message = "PAN copy path must not exceed 500 characters")
    private String panCopyPath;

    @NotBlank(message = "MSG710: Certificate of incorporation number is required")
    @Size(max = 100, message = "Certificate number must not exceed 100 characters")
    private String certificateNumber;

    @NotBlank(message = "MSG711: Certificate document path is required")
    @Size(max = 500, message = "Certificate document path must not exceed 500 characters")
    private String certificateDocumentPath;

    @NotBlank(message = "MSG712: GST number is required")
    @Pattern(
            regexp = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$",
            message = "MSG712: GST number format is invalid")
    private String gstNumber;

    @NotBlank(message = "MSG713: GST document path is required")
    @Size(max = 500, message = "GST document path must not exceed 500 characters")
    private String gstDocumentPath;

    @NotBlank(message = "MSG714: Contact person name is required")
    @Pattern(regexp = "^[A-Za-z .'-]+$", message = "MSG714: Contact person name must contain alphabetic characters only")
    @Size(max = 150, message = "Contact person name must not exceed 150 characters")
    private String contactPersonName;

    @NotBlank(message = "MSG715: Contact person mobile number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "MSG715: Contact person mobile number must be 10 digits")
    private String contactPersonMobileNo;

    @NotNull(message = "MSG716: MSME registration flag is required")
    private Boolean msmeRegistered;

    @NotEmpty(message = "MSG717: At least one escalation matrix entry is required")
    @Valid
    private List<AgencyEscalationMatrixRequest> escalationMatrixEntries = new ArrayList<>();

    @NotBlank(message = "MSG718: Bank name is required")
    @Size(max = 150, message = "Bank name must not exceed 150 characters")
    private String bankName;

    @NotBlank(message = "MSG719: Bank branch is required")
    @Size(max = 150, message = "Bank branch must not exceed 150 characters")
    private String bankBranch;

    @NotBlank(message = "MSG720: Bank account number is required")
    @Pattern(regexp = "^[0-9]{9,30}$", message = "MSG720: Bank account number must be numeric")
    private String bankAccountNumber;

    @NotNull(message = "MSG721: Bank account type is required")
    private AgencyBankAccountType bankAccountType;

    @NotBlank(message = "MSG722: IFSC code is required")
    @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$", message = "MSG722: IFSC code format is invalid")
    private String ifscCode;

    @NotBlank(message = "MSG723: Cancelled cheque path is required")
    @Size(max = 500, message = "Cancelled cheque path must not exceed 500 characters")
    private String cancelledChequePath;
}
