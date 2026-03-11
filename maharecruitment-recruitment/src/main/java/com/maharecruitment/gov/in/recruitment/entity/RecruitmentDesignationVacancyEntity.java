package com.maharecruitment.gov.in.recruitment.entity;

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
        name = "recruitment_designation_vacancy",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_recruitment_vacancy_notification_designation_level",
                        columnNames = { "recruitment_notification_id", "designation_id", "level_code" })
        },
        indexes = {
                @Index(name = "idx_recruitment_vacancy_notification", columnList = "recruitment_notification_id"),
                @Index(name = "idx_recruitment_vacancy_designation", columnList = "designation_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecruitmentDesignationVacancyEntity extends RecruitmentAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recruitment_designation_vacancy_id")
    private Long recruitmentDesignationVacancyId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recruitment_notification_id", nullable = false)
    private RecruitmentNotificationEntity notification;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "designation_id", nullable = false)
    private ManpowerDesignationMaster designationMst;

    @Column(name = "level_code", nullable = false, length = 50)
    private String levelCode;

    @Column(name = "number_of_vacancy", nullable = false)
    private Long numberOfVacancy;

    @Column(name = "job_description", length = 1000)
    private String jobDescription;

    @Column(name = "filled_post", nullable = false)
    private Long fillPost = 0L;

    @PrePersist
    @PreUpdate
    void normalize() {
        if (levelCode != null) {
            levelCode = levelCode.trim().toUpperCase();
        }
        if (jobDescription != null) {
            jobDescription = jobDescription.trim();
        }
        if (fillPost == null || fillPost < 0) {
            fillPost = 0L;
        }
    }
}
