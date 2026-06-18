package com.maharecruitment.gov.in.recruitment.entity.organization;

import com.maharecruitment.gov.in.master.entity.ManpowerDesignationMaster;
import com.maharecruitment.gov.in.master.entity.ProjectMst;
import com.maharecruitment.gov.in.master.entity.ResourceLevelExperience;
import com.maharecruitment.gov.in.master.entity.CellMaster;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentAuditable;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "position_master",
        indexes = {
                @Index(name = "idx_position_master_project_status", columnList = "project_id,status"),
                @Index(name = "idx_position_master_cell_status", columnList = "cell_id,status"),
                @Index(name = "idx_position_master_team", columnList = "team_id"),
                @Index(name = "idx_position_master_reporting", columnList = "reporting_position_id"),
                @Index(name = "idx_position_master_employee", columnList = "employee_id"),
                @Index(name = "idx_position_master_position_status", columnList = "position_status")
        })
@Getter
@Setter
@NoArgsConstructor
public class PositionMasterEntity extends RecruitmentAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "position_id")
    private Long positionId;

    @NotBlank(message = "Position name is required")
    @Column(name = "position_name", nullable = false, length = 150)
    private String positionName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private ProjectMst project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cell_id")
    private CellMaster cell;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private TeamMasterEntity team;

    @NotNull(message = "Designation is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "designation_id", nullable = false)
    private ManpowerDesignationMaster designation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporting_position_id")
    private PositionMasterEntity reportingPosition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private EmployeeEntity employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "level_code", referencedColumnName = "level_code")
    private ResourceLevelExperience resourceLevel;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "position_status", nullable = false, length = 20)
    private PositionStatus positionStatus = PositionStatus.VACANT;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrganizationRecordStatus status = OrganizationRecordStatus.ACTIVE;

    @PrePersist
    @PreUpdate
    void normalizeFields() {
        if (positionName != null) {
            positionName = positionName.trim();
        }
        if (displayOrder == null) {
            displayOrder = 0;
        }
        positionStatus = employee == null ? PositionStatus.VACANT : PositionStatus.FILLED;
        if (status == null) {
            status = OrganizationRecordStatus.ACTIVE;
        }
    }
}
