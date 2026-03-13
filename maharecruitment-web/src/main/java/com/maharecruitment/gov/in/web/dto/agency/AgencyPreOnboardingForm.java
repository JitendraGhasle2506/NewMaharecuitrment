package com.maharecruitment.gov.in.web.dto.agency;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

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

    private String mobile;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dob;

    private String address;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate joiningDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate onboardingDate;

    private String aadhaar;

    private String pan;

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

    private MultipartFile aadhaarFile;

    private MultipartFile panFile;

    private MultipartFile experienceDoc;

    private MultipartFile uploadImage;

    private String existingAadhaarFileName;

    private String existingAadhaarFilePath;

    private String existingPanFileName;

    private String existingPanFilePath;

    private String existingExperienceDocFileName;

    private String existingExperienceDocFilePath;

    private String existingPhotoFileName;

    private String existingPhotoFilePath;

    private BigDecimal minExperienceYears;

    private boolean hrFlow;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate hrOnboardingDate;

    private String hrOnboardingLocation;

    private boolean hrVerified;

    private List<AgencyPreOnboardingEmploymentForm> previousEmployments = new ArrayList<>();
}
