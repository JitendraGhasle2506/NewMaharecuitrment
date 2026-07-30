package com.maharecruitment.gov.in.recruitment.entity;

import java.time.LocalDateTime;

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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "recruitment_internal_level_two_feedback",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_internal_level_two_feedback_schedule_user",
                        columnNames = {
                                "recruitment_internal_level_two_schedule_id",
                                "reviewer_user_id"
                        })
        },
        indexes = {
                @Index(
                        name = "idx_internal_level_two_feedback_schedule",
                        columnList = "recruitment_internal_level_two_schedule_id"),
                @Index(
                        name = "idx_internal_level_two_feedback_user",
                        columnList = "reviewer_user_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecruitmentInternalLevelTwoFeedbackEntity extends RecruitmentAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recruitment_internal_level_two_feedback_id")
    private Long recruitmentInternalLevelTwoFeedbackId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recruitment_internal_level_two_schedule_id", nullable = false)
    private RecruitmentInternalLevelTwoScheduleEntity schedule;

    @Column(name = "reviewer_user_id", nullable = false)
    private Long reviewerUserId;

    @Column(name = "reviewer_name", nullable = false, length = 150)
    private String reviewerName;

    @Column(name = "reviewer_role_label", nullable = false, length = 100)
    private String reviewerRoleLabel;

    @Column(name = "communication_skill_marks", nullable = false)
    private Integer communicationSkillMarks;

    @Column(name = "technical_skill_marks", nullable = false)
    private Integer technicalSkillMarks;

    @Column(name = "leadership_quality_marks", nullable = false)
    private Integer leadershipQualityMarks;

    @Column(name = "relevant_experience_marks", nullable = false)
    private Integer relevantExperienceMarks;

    @Column(name = "interviewer_grade", nullable = false, length = 10)
    private String interviewerGrade;

    @Column(name = "recommendation_status", nullable = false, length = 30)
    private String recommendationStatus;

    @Column(name = "assessment_remarks", length = 1000)
    private String assessmentRemarks;

    @Column(name = "final_remarks", length = 1000)
    private String finalRemarks;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @PrePersist
    @PreUpdate
    void normalize() {
        reviewerName = normalizeText(reviewerName);
        reviewerRoleLabel = normalizeText(reviewerRoleLabel);
        interviewerGrade = normalizeUpper(interviewerGrade);
        recommendationStatus = normalizeUpper(recommendationStatus);
        assessmentRemarks = normalizeText(assessmentRemarks);
        finalRemarks = normalizeText(finalRemarks);
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeUpper(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase() : null;
    }
}
