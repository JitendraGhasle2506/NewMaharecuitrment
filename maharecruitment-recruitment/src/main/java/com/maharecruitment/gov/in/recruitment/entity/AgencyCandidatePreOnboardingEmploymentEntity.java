package com.maharecruitment.gov.in.recruitment.entity;

import java.time.LocalDate;

import org.springframework.util.StringUtils;

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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "agency_candidate_pre_onboarding_employment",
        indexes = {
                @Index(name = "idx_pre_onboarding_employment_parent", columnList = "pre_onboarding_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AgencyCandidatePreOnboardingEmploymentEntity extends RecruitmentAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pre_onboarding_employment_id")
    private Long preOnboardingEmploymentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pre_onboarding_id", nullable = false)
    private AgencyCandidatePreOnboardingEntity preOnboarding;

    @Column(name = "company_name", nullable = false, length = 200)
    private String companyName;

    @Column(name = "designation", length = 150)
    private String designation;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @PrePersist
    @PreUpdate
    void normalize() {
        companyName = normalizeText(companyName);
        designation = normalizeText(designation);
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : value;
    }
}
