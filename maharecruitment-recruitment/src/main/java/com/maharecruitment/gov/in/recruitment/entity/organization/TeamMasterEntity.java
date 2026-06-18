package com.maharecruitment.gov.in.recruitment.entity.organization;

import com.maharecruitment.gov.in.master.entity.ProjectMst;
import com.maharecruitment.gov.in.master.entity.CellMaster;
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
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "team_master",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_team_master_cell_team",
                        columnNames = { "cell_id", "team_name" })
        },
        indexes = {
                @Index(name = "idx_team_master_project_status", columnList = "project_id,status"),
                @Index(name = "idx_team_master_cell_id", columnList = "cell_id"),
                @Index(name = "idx_team_master_parent", columnList = "parent_team_id"),
                @Index(name = "idx_team_master_type", columnList = "team_type")
        })
@Getter
@Setter
@NoArgsConstructor
public class TeamMasterEntity extends RecruitmentAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_id")
    private Long teamId;

    @NotBlank(message = "Team name is required")
    @Column(name = "team_name", nullable = false, length = 150)
    private String teamName;

    @NotNull(message = "Team type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "team_type", nullable = false, length = 30)
    private OrganizationTeamType teamType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_team_id")
    private TeamMasterEntity parentTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private ProjectMst project;

    @NotNull(message = "Cell is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cell_id", nullable = false)
    private CellMaster cell;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrganizationRecordStatus status = OrganizationRecordStatus.ACTIVE;

    @PrePersist
    @PreUpdate
    void normalizeFields() {
        if (teamName != null) {
            teamName = teamName.trim();
        }
        if (displayOrder == null) {
            displayOrder = 0;
        }
        if (status == null) {
            status = OrganizationRecordStatus.ACTIVE;
        }
    }
}
