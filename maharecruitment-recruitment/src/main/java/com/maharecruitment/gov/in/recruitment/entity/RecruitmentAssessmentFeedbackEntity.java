package com.maharecruitment.gov.in.recruitment.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.util.StringUtils;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
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
        name = "recruitment_assessment_feedback",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_assessment_feedback_interview_detail",
                        columnNames = "recruitment_interview_detail_id")
        },
        indexes = {
                @Index(name = "idx_assessment_feedback_request_id", columnList = "request_id"),
                @Index(name = "idx_assessment_feedback_department_registration", columnList = "department_registration_id"),
                @Index(name = "idx_assessment_feedback_application", columnList = "department_project_application_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecruitmentAssessmentFeedbackEntity extends RecruitmentAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recruitment_assessment_feedback_id")
    private Long recruitmentAssessmentFeedbackId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recruitment_interview_detail_id", nullable = false, unique = true)
    private RecruitmentInterviewDetailEntity recruitmentInterviewDetail;

    @Column(name = "department_registration_id")
    private Long departmentRegistrationId;

    @Column(name = "request_id", nullable = false, length = 32)
    private String requestId;

    @Column(name = "department_project_application_id")
    private Long departmentProjectApplicationId;

    @Column(name = "internal_vacancy_opening_id")
    private Long internalVacancyOpeningId;

    @Column(name = "interview_authority", length = 255)
    private String interviewAuthority;

    @Column(name = "candidate_name", nullable = false, length = 150)
    private String candidateName;

    @Column(name = "interview_date_time")
    private LocalDateTime interviewDateTime;

    @Column(name = "mobile", length = 15)
    private String mobile;

    @Column(name = "designation_id", nullable = false)
    private Long designationId;

    @Column(name = "designation_name", length = 150)
    private String designationName;

    @Column(name = "level_code", length = 50)
    private String levelCode;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "alt_email", length = 255)
    private String altEmail;

    @Column(name = "qualification", length = 255)
    private String qualification;

    @Column(name = "total_experience", precision = 5, scale = 1)
    private BigDecimal totalExperience;

    @Column(name = "communication_skill_marks")
    private Integer communicationSkillMarks;

    @Column(name = "technical_skill_marks")
    private Integer technicalSkillMarks;

    @Column(name = "leadership_quality_marks")
    private Integer leadershipQualityMarks;

    @Column(name = "relevant_experience_marks")
    private Integer relevantExperienceMarks;

    @Column(name = "interviewer_grade", length = 10, nullable = false)
    private String interviewerGrade;

    @Column(name = "recommendation_status", length = 30, nullable = false)
    private String recommendationStatus;

    @Column(name = "assessment_remarks", length = 1000)
    private String assessmentRemarks;

    @Column(name = "final_remarks", length = 1000)
    private String finalRemarks;

    @Column(name = "interviewer_user_id", nullable = false)
    private Long interviewerUserId;

    @OneToMany(mappedBy = "assessmentFeedback", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecruitmentAssessmentPanelMemberEntity> panelMembers = new ArrayList<>();

    public void replacePanelMembers(List<RecruitmentAssessmentPanelMemberEntity> members) {
        panelMembers.clear();
        if (members == null || members.isEmpty()) {
            return;
        }
        for (RecruitmentAssessmentPanelMemberEntity member : members) {
            addPanelMember(member);
        }
    }

    public void addPanelMember(RecruitmentAssessmentPanelMemberEntity member) {
        if (member == null) {
            return;
        }
        member.setAssessmentFeedback(this);
        panelMembers.add(member);
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        requestId = normalizeUpper(requestId);
        interviewAuthority = normalizeText(interviewAuthority);
        candidateName = normalizeText(candidateName);
        mobile = normalizeText(mobile);
        designationName = normalizeText(designationName);
        levelCode = normalizeUpper(levelCode);
        email = normalizeEmail(email);
        altEmail = normalizeEmail(altEmail);
        qualification = normalizeText(qualification);
        interviewerGrade = normalizeUpper(interviewerGrade);
        recommendationStatus = normalizeUpper(recommendationStatus);
        assessmentRemarks = normalizeText(assessmentRemarks);
        finalRemarks = normalizeText(finalRemarks);
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : value;
    }

    private String normalizeUpper(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase() : value;
    }

    private String normalizeEmail(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase() : value;
    }
}
