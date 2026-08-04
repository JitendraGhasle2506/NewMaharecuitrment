package com.maharecruitment.gov.in.recruitment.entity.organization;

import java.time.LocalDate;

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
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "employee_team_mapping",
        indexes = {
                @Index(name = "idx_employee_team_mapping_employee", columnList = "employee_id"),
                @Index(name = "idx_employee_team_mapping_team_status", columnList = "team_id,status"),
                @Index(name = "idx_employee_team_mapping_position_status", columnList = "position_id,status"),
                @Index(name = "idx_employee_team_mapping_effective", columnList = "effective_date")
        })
@Getter
@Setter
@NoArgsConstructor
public class EmployeeTeamMappingEntity extends RecruitmentAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mapping_id")
    private Long mappingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private EmployeeEntity employee;

    @NotNull(message = "Team is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private TeamMasterEntity team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "position_id")
    private PositionMasterEntity position;

    @NotNull(message = "Effective date is required")
    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrganizationRecordStatus status = OrganizationRecordStatus.ACTIVE;

    @PrePersist
    void initializeDefaults() {
        if (effectiveDate == null) {
            effectiveDate = LocalDate.now();
        }
        if (status == null) {
            status = OrganizationRecordStatus.ACTIVE;
        }
    }
}
