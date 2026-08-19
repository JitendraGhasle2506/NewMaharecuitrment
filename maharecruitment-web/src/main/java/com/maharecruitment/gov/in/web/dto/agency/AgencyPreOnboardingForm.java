package com.maharecruitment.gov.in.web.dto.agency;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import com.maharecruitment.gov.in.common.util.SensitiveDataMaskingUtil;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AgencyPreOnboardingForm {

    private Long preOnboardingId;

    private Long recruitmentInterviewDetailId;

    private Long recruitmentNotificationId;

    private String requestId;

    private String projectName;

    private String department;

    private String subDeptName;

    private String designation;

    private String levelCode;

    private String agencyName;

    private String name;

    private String email;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be exactly 10 digits")
    private String mobile;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dob;

    @NotBlank(message = "Gender is required")
    private String gender;

    @NotBlank(message = "Blood group is required")
    private String bloodGroup;

    private String address;

    @NotBlank(message = "Emergency contact name is required")
    @Pattern(regexp = "^[a-zA-Z\\s]*$", message = "Emergency contact name should not have special characters")
    private String emergencyContactName;

    @NotBlank(message = "Emergency contact relation is required")
    private String emergencyContactRelation;

    @NotBlank(message = "Emergency contact mobile is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Emergency contact mobile number must be exactly 10 digits")
    private String emergencyContactMobile;

    @Pattern(regexp = "^$|^[0-9]{10}$", message = "Emergency contact alternate mobile number must be exactly 10 digits")
    private String emergencyContactAltMobile;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate joiningDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate onboardingDate;

    private String aadhaar;

    private String pan;

    @Size(max = 800, message = "Encrypted Aadhaar number is invalid")
    private String aadhaarEncrypted;

    @Size(max = 800, message = "Encrypted PAN number is invalid")
    private String panEncrypted;

    @Size(max = 100, message = "Encryption key identifier is invalid")
    private String encryptionKeyId;

    private Long timestamp;

    @Pattern(regexp = "^[A-Za-z0-9_-]{22,128}$", message = "Encrypted request nonce is invalid")
    private String nonce;

    private Integer totalExperienceYears = 0;

    private Integer totalExperienceMonths = 0;

    private boolean docEducationalCert;

    private boolean docExperienceLetter;

    private boolean docRelievingLetter;

    private boolean docPayslips;

    private boolean docDeclarationForm;

    private boolean docNda;

    private boolean docMedicalFitness;

    private boolean docAddressProof;

    private boolean docPassportPhoto;

    private boolean docAadhaar;

    private boolean docPan;

    private boolean agencyFlag;

    private boolean companyPayrollMoreThanThreeMonths;

    private MultipartFile aadhaarFile;

    private MultipartFile panFile;

    private MultipartFile experienceDoc;

    private MultipartFile companyPayrollProof;

    private MultipartFile uploadImage;

    private String existingAadhaarFileName;

    private String existingAadhaarFilePath;

    private String existingPanFileName;

    private String existingPanFilePath;

    private String existingExperienceDocFileName;

    private String existingExperienceDocFilePath;

    private String existingCompanyPayrollProofFileName;

    private String existingCompanyPayrollProofFilePath;

    private String existingPhotoFileName;

    private String existingPhotoFilePath;

    private BigDecimal minExperienceYears;

    private boolean hrFlow;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate hrOnboardingDate;

    private String hrOnboardingLocation;

    private List<Long> selectedLocationIds = new ArrayList<>();

    private boolean hrVerified;

    private List<AgencyPreOnboardingEmploymentForm> previousEmployments = new ArrayList<>();

    public String getMaskedAadhaar() {
        return SensitiveDataMaskingUtil.maskAadhaar(aadhaar);
    }

    public String getMaskedPan() {
        return SensitiveDataMaskingUtil.maskPan(pan);
    }

    public void clearEncryptedSubmission() {
        aadhaarEncrypted = null;
        panEncrypted = null;
        encryptionKeyId = null;
        timestamp = null;
        nonce = null;
        aadhaar = null;
        pan = null;
    }
}
