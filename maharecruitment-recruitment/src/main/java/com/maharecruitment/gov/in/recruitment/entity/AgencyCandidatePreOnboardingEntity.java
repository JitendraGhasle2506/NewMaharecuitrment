package com.maharecruitment.gov.in.recruitment.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.util.StringUtils;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "agency_candidate_pre_onboarding",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_pre_onboarding_interview_detail",
                        columnNames = "recruitment_interview_detail_id")
        },
        indexes = {
                @Index(name = "idx_pre_onboarding_interview_detail", columnList = "recruitment_interview_detail_id"),
                @Index(name = "idx_pre_onboarding_submitted_at", columnList = "submitted_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AgencyCandidatePreOnboardingEntity extends RecruitmentAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pre_onboarding_id")
    private Long preOnboardingId;

    @OneToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "recruitment_interview_detail_id", nullable = true)
    private RecruitmentInterviewDetailEntity interviewDetail;

    @Column(name = "agency_user_id", nullable = false)
    private Long agencyUserId;

    @Column(name = "candidate_name", nullable = false, length = 150)
    private String candidateName;

    @Column(name = "candidate_email", nullable = false, length = 255)
    private String candidateEmail;

    @Column(name = "candidate_mobile", nullable = false, length = 15)
    private String candidateMobile;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "gender", nullable = false, length = 20)
    private String gender;

    @Column(name = "blood_group", nullable = false, length = 20)
    private String bloodGroup;

    @Column(name = "address", nullable = false, length = 1000)
    private String address;

    @Column(name = "emergency_contact_name", nullable = false, length = 100)
    private String emergencyContactName;

    @Column(name = "emergency_contact_relation", nullable = false, length = 50)
    private String emergencyContactRelation;

    @Column(name = "emergency_contact_mobile", nullable = false, length = 15)
    private String emergencyContactMobile;

    @Column(name = "emergency_contact_alt_mobile", length = 15)
    private String emergencyContactAltMobile;

    @Column(name = "joining_date", nullable = false)
    private LocalDate joiningDate;

    @Column(name = "onboarding_date")
    private LocalDate onboardingDate;

    @Column(name = "aadhaar_number", nullable = false, length = 255)
    private String aadhaarNumber;

    @Column(name = "aadhaar_original_name", length = 255)
    private String aadhaarOriginalName;

    @Column(name = "aadhaar_file_path", length = 700)
    private String aadhaarFilePath;

    @Column(name = "aadhaar_file_type", length = 120)
    private String aadhaarFileType;

    @Column(name = "aadhaar_file_size")
    private Long aadhaarFileSize;

    @Column(name = "pan_number", nullable = false, length = 10)
    private String panNumber;

    @Column(name = "pan_original_name", length = 255)
    private String panOriginalName;

    @Column(name = "pan_file_path", length = 700)
    private String panFilePath;

    @Column(name = "pan_file_type", length = 120)
    private String panFileType;

    @Column(name = "pan_file_size")
    private Long panFileSize;

    @Column(name = "total_experience_years", nullable = false)
    private Integer totalExperienceYears = 0;

    @Column(name = "total_experience_months", nullable = false)
    private Integer totalExperienceMonths = 0;

    @Column(name = "experience_doc_original_name", length = 255)
    private String experienceDocOriginalName;

    @Column(name = "experience_doc_file_path", length = 700)
    private String experienceDocFilePath;

    @Column(name = "experience_doc_file_type", length = 120)
    private String experienceDocFileType;

    @Column(name = "experience_doc_file_size")
    private Long experienceDocFileSize;

    @Column(name = "photo_original_name", length = 255)
    private String photoOriginalName;

    @Column(name = "photo_file_path", length = 700)
    private String photoFilePath;

    @Column(name = "photo_file_type", length = 120)
    private String photoFileType;

    @Column(name = "photo_file_size")
    private Long photoFileSize;

    @Column(name = "embedding", columnDefinition = "text")
    private String embedding;

    @Column(name = "doc_educational_cert", nullable = false)
    private Boolean docEducationalCert = false;

    @Column(name = "doc_experience_letter", nullable = false)
    private Boolean docExperienceLetter = false;

    @Column(name = "doc_relieving_letter", nullable = false)
    private Boolean docRelievingLetter = false;

    @Column(name = "doc_payslips", nullable = false)
    private Boolean docPayslips = false;

    @Column(name = "doc_declaration_form", nullable = false)
    private Boolean docDeclarationForm = false;

    @Column(name = "doc_nda", nullable = false)
    private Boolean docNda = false;

    @Column(name = "doc_medical_fitness", nullable = false)
    private Boolean docMedicalFitness = false;

    @Column(name = "doc_address_proof", nullable = false)
    private Boolean docAddressProof = false;

    @Column(name = "doc_passport_photo", nullable = false)
    private Boolean docPassportPhoto = false;

    @Column(name = "doc_aadhaar", nullable = false)
    private Boolean docAadhaar = false;

    @Column(name = "doc_pan", nullable = false)
    private Boolean docPan = false;

    @Column(name = "company_payroll_more_than_three_months", nullable = false)
    private Boolean companyPayrollMoreThanThreeMonths = false;

    @Column(name = "company_payroll_proof_original_name", length = 255)
    private String companyPayrollProofOriginalName;

    @Column(name = "company_payroll_proof_file_path", length = 700)
    private String companyPayrollProofFilePath;

    @Column(name = "company_payroll_proof_file_type", length = 120)
    private String companyPayrollProofFileType;

    @Column(name = "company_payroll_proof_file_size")
    private Long companyPayrollProofFileSize;

    @Column(name = "agency_verified", nullable = false)
    private Boolean agencyVerified = false;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "hr_onboarding_date")
    private LocalDate hrOnboardingDate;

    @Column(name = "hr_onboarding_location", length = 255)
    private String hrOnboardingLocation;

    @Column(name = "hr_verified", nullable = false)
    private Boolean hrVerified = false;

    @Column(name = "hr_user_id")
    private Long hrUserId;

    @Column(name = "onboarded_at")
    private LocalDateTime onboardedAt;

    @OneToMany(mappedBy = "preOnboarding", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("startDate asc, preOnboardingEmploymentId asc")
    private List<AgencyCandidatePreOnboardingEmploymentEntity> previousEmployments = new ArrayList<>();

    public void replacePreviousEmployments(List<AgencyCandidatePreOnboardingEmploymentEntity> employments) {
        previousEmployments.clear();
        if (employments == null || employments.isEmpty()) {
            return;
        }

        employments.forEach(this::addPreviousEmployment);
    }

    public void addPreviousEmployment(AgencyCandidatePreOnboardingEmploymentEntity employment) {
        if (employment == null) {
            return;
        }
        employment.setPreOnboarding(this);
        previousEmployments.add(employment);
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        candidateName = normalizeText(candidateName);
        candidateEmail = normalizeEmail(candidateEmail);
        candidateMobile = normalizeText(candidateMobile);
        gender = normalizeText(gender);
        bloodGroup = normalizeText(bloodGroup);
        address = normalizeText(address);
        emergencyContactName = normalizeText(emergencyContactName);
        emergencyContactRelation = normalizeText(emergencyContactRelation);
        emergencyContactMobile = normalizeText(emergencyContactMobile);
        emergencyContactAltMobile = normalizeText(emergencyContactAltMobile);
        aadhaarNumber = normalizeDigits(aadhaarNumber);
        aadhaarOriginalName = normalizeText(aadhaarOriginalName);
        aadhaarFilePath = normalizeText(aadhaarFilePath);
        aadhaarFileType = normalizeText(aadhaarFileType);
        panNumber = normalizePan(panNumber);
        panOriginalName = normalizeText(panOriginalName);
        panFilePath = normalizeText(panFilePath);
        panFileType = normalizeText(panFileType);
        experienceDocOriginalName = normalizeText(experienceDocOriginalName);
        experienceDocFilePath = normalizeText(experienceDocFilePath);
        experienceDocFileType = normalizeText(experienceDocFileType);
        photoOriginalName = normalizeText(photoOriginalName);
        photoFilePath = normalizeText(photoFilePath);
        photoFileType = normalizeText(photoFileType);
        embedding = normalizeText(embedding);
        companyPayrollProofOriginalName = normalizeText(companyPayrollProofOriginalName);
        companyPayrollProofFilePath = normalizeText(companyPayrollProofFilePath);
        companyPayrollProofFileType = normalizeText(companyPayrollProofFileType);

        totalExperienceYears = totalExperienceYears == null || totalExperienceYears < 0 ? 0 : totalExperienceYears;
        totalExperienceMonths = totalExperienceMonths == null || totalExperienceMonths < 0 ? 0 : totalExperienceMonths;
        docEducationalCert = Boolean.TRUE.equals(docEducationalCert);
        docExperienceLetter = Boolean.TRUE.equals(docExperienceLetter);
        docRelievingLetter = Boolean.TRUE.equals(docRelievingLetter);
        docPayslips = Boolean.TRUE.equals(docPayslips);
        docDeclarationForm = Boolean.TRUE.equals(docDeclarationForm);
        docNda = Boolean.TRUE.equals(docNda);
        docMedicalFitness = Boolean.TRUE.equals(docMedicalFitness);
        docAddressProof = Boolean.TRUE.equals(docAddressProof);
        docPassportPhoto = Boolean.TRUE.equals(docPassportPhoto);
        docAadhaar = Boolean.TRUE.equals(docAadhaar);
        docPan = Boolean.TRUE.equals(docPan);
        companyPayrollMoreThanThreeMonths = Boolean.TRUE.equals(companyPayrollMoreThanThreeMonths);
        agencyVerified = Boolean.TRUE.equals(agencyVerified);
        hrVerified = Boolean.TRUE.equals(hrVerified);
        hrOnboardingLocation = normalizeText(hrOnboardingLocation);

        if (submittedAt == null) {
            submittedAt = LocalDateTime.now();
        }
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : value;
    }

    private String normalizeEmail(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase() : value;
    }

    private String normalizeDigits(String value) {
        return StringUtils.hasText(value) ? value.trim().replaceAll("\\s+", "") : value;
    }

    private String normalizePan(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase() : value;
    }
}
