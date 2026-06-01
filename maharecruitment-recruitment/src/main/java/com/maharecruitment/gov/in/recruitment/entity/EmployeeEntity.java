package com.maharecruitment.gov.in.recruitment.entity;

import java.time.LocalDate;

import com.maharecruitment.gov.in.auth.entity.DepartmentRegistrationEntity;
import com.maharecruitment.gov.in.master.entity.AgencyMaster;
import com.maharecruitment.gov.in.master.entity.ManpowerDesignationMaster;
import com.maharecruitment.gov.in.master.entity.SubDepartment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "employee_master", indexes = {
        @Index(name = "idx_employee_code", columnList = "employee_code"),
        @Index(name = "idx_employee_email", columnList = "email")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeEntity extends RecruitmentAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "employee_id")
    private Long employeeId;

    @Column(name = "employee_code", unique = true, nullable = true, length = 50)
    private String employeeCode;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "request_id", length = 50)
    private String requestId;

    @Column(name = "recruitment_type", length = 20)
    private String recruitmentType; // INTERNAL or EXTERNAL

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "mobile", nullable = false, length = 15)
    private String mobile;

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

    @Column(name = "mahait_onboarding_date", nullable = false)
    private LocalDate onboardingDate;

    @Column(name = "resignation_date")
    private LocalDate resignationDate;

    @Column(name = "pan_number", nullable = false, length = 10)
    private String panNumber;

    @Column(name = "aadhaar_number", nullable = false, length = 12)
    private String aadhaarNumber;

    @Column(name = "company_payroll_more_than_three_months", nullable = false)
    private Boolean companyPayrollMoreThanThreeMonths = false;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pre_onboarding_id")
    private AgencyCandidatePreOnboardingEntity preOnboarding;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agency_id")
    private AgencyMaster agency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_registration_id")
    private DepartmentRegistrationEntity departmentRegistration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_department_id")
    private SubDepartment subDepartment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "designation_id")
    private ManpowerDesignationMaster designation;

    @Column(name = "level_code", length = 50)
    private String levelCode;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";
}
