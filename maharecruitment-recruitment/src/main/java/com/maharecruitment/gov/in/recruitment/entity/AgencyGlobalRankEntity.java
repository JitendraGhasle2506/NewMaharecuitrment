package com.maharecruitment.gov.in.recruitment.entity;

import java.time.LocalDateTime;

import com.maharecruitment.gov.in.master.entity.AgencyMaster;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "agency_global_rank",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_agency_global_rank_agency", columnNames = "agency_id")
        },
        indexes = {
                @Index(name = "idx_agency_global_rank_agency", columnList = "agency_id"),
                @Index(name = "idx_agency_global_rank_rank", columnList = "rank_number")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AgencyGlobalRankEntity extends RecruitmentAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "agency_global_rank_id")
    private Long agencyGlobalRankId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agency_id", nullable = false)
    private AgencyMaster agency;

    @Column(name = "rank_number", nullable = false)
    private Integer rankNumber;

    @Column(name = "assigned_date", nullable = false)
    private LocalDateTime assignedDate;

    @PrePersist
    @PreUpdate
    void normalize() {
        if (rankNumber == null || rankNumber < 1) {
            throw new IllegalStateException("Global rank number must be at least 1.");
        }
        if (assignedDate == null) {
            assignedDate = LocalDateTime.now();
        }
    }
}
