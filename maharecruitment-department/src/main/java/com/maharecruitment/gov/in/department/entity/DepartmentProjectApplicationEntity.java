package com.maharecruitment.gov.in.department.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.maharecruitment.gov.in.auth.entity.Auditable;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "department_project_application",
        indexes = {
                @Index(name = "idx_dep_project_app_request_id", columnList = "request_id"),
                @Index(name = "idx_dep_project_app_dep_reg", columnList = "department_registration_id"),
                @Index(name = "idx_dep_project_app_status", columnList = "application_status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentProjectApplicationEntity extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "department_project_application_id")
    private Long departmentProjectApplicationId;

    @Column(name = "department_id", nullable = false)
    private Long departmentId;

    @Column(name = "department_registration_id", nullable = false)
    private Long departmentRegistrationId;

    @Column(name = "request_id", nullable = false, unique = true, length = 32)
    private String requestId;

    @Column(name = "sub_department_id")
    private Long subDepartmentId;

    @Column(name = "project_name", nullable = false, length = 200)
    private String projectName;

    @Column(name = "project_code", length = 100)
    private String projectCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "application_type", nullable = false, length = 40)
    private DepartmentApplicationType applicationType;

    @Convert(converter = DepartmentApplicationStatusConverter.class)
    @Column(name = "application_status", nullable = false, length = 30)
    private DepartmentApplicationStatus applicationStatus;

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @Column(name = "total_estimated_cost", precision = 14, scale = 2)
    private BigDecimal totalEstimatedCost;

    @Column(name = "mahait_contact", length = 100)
    private String mahaitContact;

    @Column(name = "work_order_original_name", length = 255)
    private String workOrderOriginalName;

    @Column(name = "work_order_file_path", length = 500)
    private String workOrderFilePath;

    @Column(name = "work_order_file_type", length = 120)
    private String workOrderFileType;

    @Column(name = "work_order_file_size")
    private Long workOrderFileSize;

    @Column(name = "remarks", length = 1000)
    private String remarks;

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("departmentProjectResourceRequirementId ASC")
    private List<DepartmentProjectResourceRequirementEntity> resourceRequirements = new ArrayList<>();

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("actionTimestamp DESC")
    private List<DepartmentProjectApplicationActivityEntity> activityLog = new ArrayList<>();

    public void replaceResourceRequirements(List<DepartmentProjectResourceRequirementEntity> requirements) {
        resourceRequirements.clear();
        if (requirements == null) {
            return;
        }

        for (DepartmentProjectResourceRequirementEntity requirement : requirements) {
            addResourceRequirement(requirement);
        }
    }

    public void addResourceRequirement(DepartmentProjectResourceRequirementEntity requirement) {
        if (requirement == null) {
            return;
        }
        requirement.setApplication(this);
        resourceRequirements.add(requirement);
    }

    public void addActivity(DepartmentProjectApplicationActivityEntity activity) {
        if (activity == null) {
            return;
        }
        activity.setApplication(this);
        activityLog.add(activity);
    }

    @PrePersist
    @PreUpdate
    void normalizeFields() {
        if (projectName != null) {
            projectName = projectName.trim();
        }
        if (projectCode != null) {
            projectCode = projectCode.trim().toUpperCase();
        }
        if (mahaitContact != null) {
            mahaitContact = mahaitContact.trim();
        }
        if (remarks != null) {
            remarks = remarks.trim();
        }
        if (workOrderOriginalName != null) {
            workOrderOriginalName = workOrderOriginalName.trim();
        }
        if (workOrderFileType != null) {
            workOrderFileType = workOrderFileType.trim();
        }

        active = !Boolean.FALSE.equals(active);
        if (applicationStatus == null) {
            applicationStatus = DepartmentApplicationStatus.DRAFT;
        }
    }
}
