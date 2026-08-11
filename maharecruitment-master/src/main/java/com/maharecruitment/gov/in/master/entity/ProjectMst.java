package com.maharecruitment.gov.in.master.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "project_mst", indexes = {
        @Index(name = "idx_project_mst_cell_id", columnList = "cell_id"),
        @Index(name = "idx_project_mst_department_id", columnList = "department_id"),
        @Index(name = "idx_project_mst_sub_department_id", columnList = "sub_department_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectMst extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "project_id")
    private Long projectId;

    @NotBlank(message = "Project Name is required")
    @Column(name = "project_name", nullable = false, length = 100)
    private String projectName;

    @Column(name = "project_code", length = 30)
    private String projectCode;

    @Column(name = "project_desc", length = 100)
    private String projectDesc;

    @NotNull(message = "Project Type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "project_type", nullable = false, length = 80)
    private ProjectType projectType;

    @NotNull(message = "Project scope is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "project_scope_type", nullable = false, length = 20)
    private ProjectScopeType projectScopeType;

    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "sub_department_id")
    private Long subDepartmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", insertable = false, updatable = false)
    private DepartmentMst department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_department_id", insertable = false, updatable = false)
    private SubDepartment subDepartment;

    @Column(name = "application_id")
    private Long applicationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cell_id")
    private CellMaster cell;

    @Column(name = "active_flag", nullable = false, length = 1)
    private String activeFlag = "Y";

    @PrePersist
    @PreUpdate
    void normalizeFields() {
        if (projectName != null) {
            projectName = projectName.trim();
        }
        if (projectCode != null) {
            projectCode = projectCode.trim().toUpperCase();
        }
        if (projectDesc != null) {
            projectDesc = projectDesc.trim();
        }
        activeFlag = "N".equalsIgnoreCase(activeFlag) ? "N" : "Y";
    }
}
