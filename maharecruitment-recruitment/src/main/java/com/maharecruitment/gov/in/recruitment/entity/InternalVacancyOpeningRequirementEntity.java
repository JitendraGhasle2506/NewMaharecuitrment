package com.maharecruitment.gov.in.recruitment.entity;

import java.math.BigDecimal;

import com.maharecruitment.gov.in.master.entity.ManpowerDesignationMaster;

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
        name = "internal_vacancy_opening_requirement",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_internal_vacancy_opening_requirement_row",
                        columnNames = { "internal_vacancy_opening_id", "designation_id", "level_code" })
        },
        indexes = {
                @Index(
                        name = "idx_internal_vacancy_opening_requirement_opening",
                        columnList = "internal_vacancy_opening_id"),
                @Index(
                        name = "idx_internal_vacancy_opening_requirement_designation",
                        columnList = "designation_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InternalVacancyOpeningRequirementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "internal_vacancy_opening_requirement_id")
    private Long internalVacancyOpeningRequirementId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "internal_vacancy_opening_id", nullable = false)
    private InternalVacancyOpeningEntity opening;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "designation_id", nullable = false)
    private ManpowerDesignationMaster designationMst;

    @Column(name = "level_code", nullable = false, length = 10)
    private String levelCode;

    @Column(name = "monthly_rate", nullable = false, precision = 14, scale = 2)
    private BigDecimal monthlyRate;

    @Column(name = "number_of_vacancy", nullable = false)
    private Long numberOfVacancy;

    @Column(name = "filled_positions", nullable = false)
    private Long filledPositions;

    @PrePersist
    @PreUpdate
    void normalize() {
        if (levelCode != null) {
            levelCode = levelCode.trim().toUpperCase();
        }
        if (filledPositions == null || filledPositions < 0) {
            filledPositions = 0L;
        }
    }
}
