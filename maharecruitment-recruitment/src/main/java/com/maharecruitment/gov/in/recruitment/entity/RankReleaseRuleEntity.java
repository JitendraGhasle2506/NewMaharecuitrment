package com.maharecruitment.gov.in.recruitment.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
        name = "rank_release_rule",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_rank_release_rule_rank", columnNames = "rank_number")
        },
        indexes = {
                @Index(name = "idx_rank_release_rule_rank", columnList = "rank_number")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RankReleaseRuleEntity extends RecruitmentAuditable {

    private static final LocalDate DEFAULT_EFFECTIVE_TO_DATE = LocalDate.of(9999, 12, 31);

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "rank_release_rule_id")
    private Long rankReleaseRuleId;

    @Column(name = "rank_number", nullable = false)
    private Integer rankNumber;

    @Column(name = "release_after_days", nullable = false)
    private Integer releaseAfterDays;

    @Column(name = "delay_from_previous_rank_days", nullable = false)
    private Integer delayFromPreviousRankDays;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = Boolean.TRUE;

    @PrePersist
    @PreUpdate
    void validate() {
        if (rankNumber == null || rankNumber < 1) {
            throw new IllegalStateException("Rank number must be at least 1.");
        }

        if (releaseAfterDays == null && delayFromPreviousRankDays == null) {
            throw new IllegalStateException(
                    "Either release-after-days or delay-from-previous-rank-days must be provided.");
        }

        if (releaseAfterDays == null) {
            releaseAfterDays = delayFromPreviousRankDays;
        }
        if (delayFromPreviousRankDays == null) {
            delayFromPreviousRankDays = releaseAfterDays;
        }

        if (releaseAfterDays < 0 || delayFromPreviousRankDays < 0) {
            throw new IllegalStateException("Release delay must be zero or greater.");
        }

        if (effectiveFrom == null) {
            effectiveFrom = LocalDate.now();
        }
        if (effectiveTo == null) {
            effectiveTo = DEFAULT_EFFECTIVE_TO_DATE;
        }
        if (effectiveTo != null && effectiveTo.isBefore(effectiveFrom)) {
            throw new IllegalStateException("effectiveTo cannot be before effectiveFrom.");
        }

        if (isActive == null) {
            isActive = Boolean.TRUE;
        }
    }

    public int getEffectiveReleaseDelayDays() {
        Integer effectiveDelay = delayFromPreviousRankDays != null ? delayFromPreviousRankDays : releaseAfterDays;
        if (effectiveDelay == null) {
            throw new IllegalStateException("No effective release delay configured for rank " + rankNumber);
        }
        return effectiveDelay;
    }
}
