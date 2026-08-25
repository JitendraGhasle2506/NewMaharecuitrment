package com.maharecruitment.gov.in.recruitment.entity;

import java.util.ArrayList;
import java.util.List;

import com.maharecruitment.gov.in.master.entity.ProjectMst;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.JoinTable;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "internal_vacancy_opening",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_internal_vacancy_opening_request_id", columnNames = "request_id")
        },
        indexes = {
                @Index(name = "idx_internal_vacancy_opening_request_id", columnList = "request_id"),
                @Index(name = "idx_internal_vacancy_opening_project", columnList = "project_id"),
                @Index(name = "idx_internal_vacancy_opening_status", columnList = "status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InternalVacancyOpeningEntity extends RecruitmentAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "internal_vacancy_opening_id")
    private Long internalVacancyOpeningId;

    @Column(name = "request_id", nullable = false, length = 32)
    private String requestId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectMst projectMst;

    @Enumerated(EnumType.STRING)
    @Column(name = "hiring_request_type", length = 30)
    private InternalVacancyHiringRequestType hiringRequestType = InternalVacancyHiringRequestType.NEW_CANDIDATE;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "internal_vacancy_replacement_employee",
            joinColumns = @JoinColumn(name = "internal_vacancy_opening_id"),
            inverseJoinColumns = @JoinColumn(name = "employee_id"),
            uniqueConstraints = @UniqueConstraint(
                    name = "uk_internal_vacancy_replacement_employee",
                    columnNames = { "internal_vacancy_opening_id", "employee_id" }))
    @OrderBy("fullName ASC, employeeId ASC")
    private List<EmployeeEntity> replacementEmployees = new ArrayList<>();

    @Column(name = "e_office_approval_file_name", length = 255)
    private String eOfficeApprovalFileName;

    @Column(name = "e_office_approval_file_path", length = 1000)
    private String eOfficeApprovalFilePath;

    @Column(name = "e_office_approval_content_type", length = 100)
    private String eOfficeApprovalContentType;

    @Column(name = "e_office_approval_file_size")
    private Long eOfficeApprovalFileSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InternalVacancyOpeningStatus status = InternalVacancyOpeningStatus.DRAFT;

    @Column(name = "remarks", length = 1000)
    private String remarks;

    @Column(name = "created_by_email", nullable = false, length = 255)
    private String createdByEmail;

    @Column(name = "updated_by_email", nullable = false, length = 255)
    private String updatedByEmail;

    @OneToMany(mappedBy = "opening", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("internalVacancyOpeningRequirementId ASC")
    private List<InternalVacancyOpeningRequirementEntity> requirements = new ArrayList<>();

    @OneToMany(mappedBy = "opening", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("internalVacancyInterviewRoleId ASC")
    private List<InternalVacancyInterviewRoleEntity> interviewRoles = new ArrayList<>();

    @OneToMany(mappedBy = "opening", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("internalVacancyInterviewAuthorityId ASC")
    private List<InternalVacancyInterviewAuthorityEntity> interviewAuthorities = new ArrayList<>();

    @OneToMany(mappedBy = "opening", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("internalVacancyInterviewEmployeeId ASC")
    private List<InternalVacancyInterviewEmployeeEntity> interviewEmployees = new ArrayList<>();

    public void replaceRequirements(List<InternalVacancyOpeningRequirementEntity> requirementEntities) {
        requirements.clear();
        if (requirementEntities == null || requirementEntities.isEmpty()) {
            return;
        }

        for (InternalVacancyOpeningRequirementEntity requirement : requirementEntities) {
            addRequirement(requirement);
        }
    }

    public void addRequirement(InternalVacancyOpeningRequirementEntity requirement) {
        if (requirement == null) {
            return;
        }
        requirement.setOpening(this);
        requirements.add(requirement);
    }

    public void replaceInterviewRoles(List<InternalVacancyInterviewRoleEntity> roleAssignments) {
        interviewRoles.clear();
        if (roleAssignments == null || roleAssignments.isEmpty()) {
            return;
        }

        for (InternalVacancyInterviewRoleEntity roleAssignment : roleAssignments) {
            addInterviewRole(roleAssignment);
        }
    }

    public void addInterviewRole(InternalVacancyInterviewRoleEntity roleAssignment) {
        if (roleAssignment == null) {
            return;
        }
        roleAssignment.setOpening(this);
        interviewRoles.add(roleAssignment);
    }

    public void replaceInterviewAuthorities(List<InternalVacancyInterviewAuthorityEntity> authorityAssignments) {
        interviewAuthorities.clear();
        if (authorityAssignments == null || authorityAssignments.isEmpty()) {
            return;
        }

        for (InternalVacancyInterviewAuthorityEntity authorityAssignment : authorityAssignments) {
            addInterviewAuthority(authorityAssignment);
        }
    }

    public void addInterviewAuthority(InternalVacancyInterviewAuthorityEntity authorityAssignment) {
        if (authorityAssignment == null) {
            return;
        }
        authorityAssignment.setOpening(this);
        interviewAuthorities.add(authorityAssignment);
    }

    public void replaceInterviewEmployees(List<InternalVacancyInterviewEmployeeEntity> employeeAssignments) {
        interviewEmployees.clear();
        if (employeeAssignments == null || employeeAssignments.isEmpty()) {
            return;
        }

        for (InternalVacancyInterviewEmployeeEntity employeeAssignment : employeeAssignments) {
            addInterviewEmployee(employeeAssignment);
        }
    }

    public void addInterviewEmployee(InternalVacancyInterviewEmployeeEntity employeeAssignment) {
        if (employeeAssignment == null) {
            return;
        }
        employeeAssignment.setOpening(this);
        interviewEmployees.add(employeeAssignment);
    }

    public void replaceReplacementEmployees(List<EmployeeEntity> employees) {
        replacementEmployees.clear();
        if (employees != null) {
            replacementEmployees.addAll(employees);
        }
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (requestId != null) {
            requestId = requestId.trim().toUpperCase();
        }
        if (remarks != null) {
            remarks = remarks.trim();
        }
        if (hiringRequestType == null) {
            hiringRequestType = InternalVacancyHiringRequestType.NEW_CANDIDATE;
        }
        if (eOfficeApprovalFileName != null) {
            eOfficeApprovalFileName = eOfficeApprovalFileName.trim();
        }
        if (eOfficeApprovalFilePath != null) {
            eOfficeApprovalFilePath = eOfficeApprovalFilePath.trim();
        }
        if (eOfficeApprovalContentType != null) {
            eOfficeApprovalContentType = eOfficeApprovalContentType.trim().toLowerCase();
        }
        if (createdByEmail != null) {
            createdByEmail = createdByEmail.trim().toLowerCase();
        }
        if (updatedByEmail != null) {
            updatedByEmail = updatedByEmail.trim().toLowerCase();
        }
        if (status == null) {
            status = InternalVacancyOpeningStatus.DRAFT;
        }
    }
}
