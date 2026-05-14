package com.maharecruitment.gov.in.recruitment.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.util.StringUtils;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "internal_vacancy_panel_assessment",
        indexes = {
                @Index(name = "idx_iv_panel_assessment_interview", columnList = "recruitment_interview_detail_id"),
                @Index(name = "idx_iv_panel_assessment_user", columnList = "assessor_user_id"),
                @Index(name = "idx_iv_panel_assessment_employee", columnList = "assessor_employee_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InternalVacancyPanelAssessmentEntity extends RecruitmentAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "internal_vacancy_panel_assessment_id")
    private Long internalVacancyPanelAssessmentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recruitment_interview_detail_id", nullable = false)
    private RecruitmentInterviewDetailEntity interviewDetail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessor_user_id")
    private User assessorUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessor_employee_id")
    private EmployeeEntity assessorEmployee;

    @Column(name = "technical_score", precision = 5, scale = 2)
    private BigDecimal technicalScore;

    @Column(name = "communication_score", precision = 5, scale = 2)
    private BigDecimal communicationScore;

    @Column(name = "leadership_score", precision = 5, scale = 2)
    private BigDecimal leadershipScore;

    @Column(name = "relevant_experience_score", precision = 5, scale = 2)
    private BigDecimal relevantExperienceScore;

    @Column(name = "total_score", precision = 5, scale = 2)
    private BigDecimal totalScore;

    @Column(name = "remarks", length = 1000)
    private String remarks;

    @Column(name = "interviewer_grade", length = 10)
    private String interviewerGrade;

    @Column(name = "recommendation_status", length = 30)
    private String recommendationStatus;

    @Column(name = "status", length = 20, nullable = false)
    private String status = "SUBMITTED";

    @PrePersist
    @PreUpdate
    void normalize() {
        remarks = StringUtils.hasText(remarks) ? remarks.trim() : null;
        interviewerGrade = StringUtils.hasText(interviewerGrade) ? interviewerGrade.trim().toUpperCase() : null;
        recommendationStatus = StringUtils.hasText(recommendationStatus) ? recommendationStatus.trim().toUpperCase() : null;
        if (status == null) {
            status = "SUBMITTED";
        }
        
        // Auto-calculate total if scores are provided
        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        if (technicalScore != null) { sum = sum.add(technicalScore); count++; }
        if (communicationScore != null) { sum = sum.add(communicationScore); count++; }
        if (leadershipScore != null) { sum = sum.add(leadershipScore); count++; }
        if (relevantExperienceScore != null) { sum = sum.add(relevantExperienceScore); count++; }
        
        if (count > 0) {
            this.totalScore = sum;
        }
    }
}
