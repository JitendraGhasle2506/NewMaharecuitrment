package com.maharecruitment.gov.in.master.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "project_mst")
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

    @Column(name = "department_registration_id")
    private Long departmentRegistrationId;

    @Column(name = "application_id")
    private Long applicationId;

    @PrePersist
    @PreUpdate
    void normalizeFields() {
        if (projectName != null) {
            projectName = projectName.trim();
        }
        if (projectDesc != null) {
            projectDesc = projectDesc.trim();
        }
    }
}
