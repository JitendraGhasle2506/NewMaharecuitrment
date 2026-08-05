package com.maharecruitment.gov.in.recruitment.entity;

import com.maharecruitment.gov.in.master.entity.CellMaster;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "cell_reporting_authority_mapping",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_cell_reporting_authority_mapping_cell",
                columnNames = "cell_id"),
        indexes = @Index(
                name = "idx_cell_reporting_authority_user",
                columnList = "authority_user_id"))
@Getter
@Setter
@NoArgsConstructor
public class CellReportingAuthorityMappingEntity extends RecruitmentAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cell_reporting_authority_mapping_id")
    private Long mappingId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cell_id", nullable = false)
    private CellMaster cell;

    @Column(name = "authority_user_id", nullable = false)
    private Long authorityUserId;
}
