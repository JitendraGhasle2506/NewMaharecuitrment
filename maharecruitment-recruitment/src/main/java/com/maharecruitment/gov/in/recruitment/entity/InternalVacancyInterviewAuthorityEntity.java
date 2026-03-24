package com.maharecruitment.gov.in.recruitment.entity;

import com.maharecruitment.gov.in.auth.entity.User;

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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "internal_vacancy_interview_authority",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_internal_vacancy_interview_authority",
                        columnNames = { "internal_vacancy_opening_id", "user_id" })
        },
        indexes = {
                @Index(
                        name = "idx_internal_vacancy_interview_authority_opening",
                        columnList = "internal_vacancy_opening_id"),
                @Index(
                        name = "idx_internal_vacancy_interview_authority_user",
                        columnList = "user_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InternalVacancyInterviewAuthorityEntity extends RecruitmentAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "internal_vacancy_interview_authority_id")
    private Long internalVacancyInterviewAuthorityId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "internal_vacancy_opening_id", nullable = false)
    private InternalVacancyOpeningEntity opening;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
