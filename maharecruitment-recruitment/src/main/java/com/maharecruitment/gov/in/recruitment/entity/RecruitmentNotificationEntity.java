package com.maharecruitment.gov.in.recruitment.entity;

import java.util.ArrayList;
import java.util.List;

import com.maharecruitment.gov.in.master.entity.ProjectMst;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
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
        name = "recruitment_notification",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_recruitment_notification_request_id", columnNames = "request_id")
        },
        indexes = {
                @Index(name = "idx_recruitment_notification_request_id", columnList = "request_id"),
                @Index(name = "idx_recruitment_notification_dep_reg", columnList = "department_registration_id"),
                @Index(name = "idx_recruitment_notification_internal_vacancy", columnList = "internal_vacancy_opening_id"),
                @Index(name = "idx_recruitment_notification_status", columnList = "status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecruitmentNotificationEntity extends RecruitmentAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recruitment_notification_id")
    private Long recruitmentNotificationId;

    @Column(name = "request_id", nullable = false, length = 32)
    private String requestId;

    @Column(name = "department_registration_id")
    private Long departmentRegistrationId;

    @Column(name = "department_project_application_id")
    private Long departmentProjectApplicationId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "internal_vacancy_opening_id")
    private InternalVacancyOpeningEntity internalVacancyOpening;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectMst projectMst;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private RecruitmentNotificationStatus status = RecruitmentNotificationStatus.PENDING_ALLOCATION;

    @OneToMany(mappedBy = "notification", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("recruitmentDesignationVacancyId ASC")
    private List<RecruitmentDesignationVacancyEntity> designationVacancies = new ArrayList<>();

    @OneToMany(mappedBy = "recruitmentNotification", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("rankNumber ASC")
    private List<RecruitmentNotificationAgencyRankEntity> agencyRanks = new ArrayList<>();

    @OneToMany(mappedBy = "recruitmentNotification", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("notifiedAt DESC")
    private List<AgencyNotificationTrackingEntity> agencyTrackingEntries = new ArrayList<>();

    public void replaceDesignationVacancies(List<RecruitmentDesignationVacancyEntity> vacancies) {
        designationVacancies.clear();
        if (vacancies == null || vacancies.isEmpty()) {
            return;
        }

        for (RecruitmentDesignationVacancyEntity vacancy : vacancies) {
            addDesignationVacancy(vacancy);
        }
    }

    public void addDesignationVacancy(RecruitmentDesignationVacancyEntity vacancy) {
        if (vacancy == null) {
            return;
        }
        vacancy.setNotification(this);
        designationVacancies.add(vacancy);
    }

    public void replaceAgencyRanks(List<RecruitmentNotificationAgencyRankEntity> ranks) {
        agencyRanks.clear();
        if (ranks == null || ranks.isEmpty()) {
            return;
        }

        for (RecruitmentNotificationAgencyRankEntity rank : ranks) {
            addAgencyRank(rank);
        }
    }

    public void addAgencyRank(RecruitmentNotificationAgencyRankEntity rank) {
        if (rank == null) {
            return;
        }
        rank.setRecruitmentNotification(this);
        agencyRanks.add(rank);
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (requestId != null) {
            requestId = requestId.trim().toUpperCase();
        }
        if (status == null) {
            status = RecruitmentNotificationStatus.PENDING_ALLOCATION;
        }
    }
}
