package com.maharecruitment.gov.in.recruitment.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.master.entity.AgencyMaster;

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
        name = "recruitment_interview_detail",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_recruitment_interview_notification_agency_email",
                        columnNames = { "recruitment_notification_id", "agency_id", "candidate_email" }),
                @UniqueConstraint(
                        name = "uk_recruitment_interview_notification_agency_mobile",
                        columnNames = { "recruitment_notification_id", "agency_id", "candidate_mobile" })
        },
        indexes = {
                @Index(name = "idx_recruitment_interview_notification", columnList = "recruitment_notification_id"),
                @Index(name = "idx_recruitment_interview_agency", columnList = "agency_id"),
                @Index(name = "idx_recruitment_interview_vacancy", columnList = "recruitment_designation_vacancy_id"),
                @Index(name = "idx_recruitment_interview_status", columnList = "candidate_status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecruitmentInterviewDetailEntity extends RecruitmentAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recruitment_interview_detail_id")
    private Long recruitmentInterviewDetailId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recruitment_notification_id", nullable = false)
    private RecruitmentNotificationEntity recruitmentNotification;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agency_id", nullable = false)
    private AgencyMaster agency;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recruitment_designation_vacancy_id", nullable = false)
    private RecruitmentDesignationVacancyEntity designationVacancy;

    @Column(name = "agency_user_id", nullable = false)
    private Long agencyUserId;

    @Column(name = "candidate_name", nullable = false, length = 150)
    private String candidateName;

    @Column(name = "candidate_email", nullable = false, length = 255)
    private String candidateEmail;

    @Column(name = "candidate_mobile", nullable = false, length = 15)
    private String candidateMobile;

    @Column(name = "candidate_education", nullable = false, length = 255)
    private String candidateEducation;

    @Column(name = "total_experience", nullable = false, precision = 5, scale = 1)
    private BigDecimal totalExperience;

    @Column(name = "relevant_experience", nullable = false, precision = 5, scale = 1)
    private BigDecimal relevantExperience;

    @Column(name = "joining_time", nullable = false, length = 50)
    private String joiningTime;

    @Column(name = "resume_original_name", nullable = false, length = 255)
    private String resumeOriginalName;

    @Column(name = "resume_file_path", nullable = false, length = 700)
    private String resumeFilePath;

    @Column(name = "resume_file_type", length = 120)
    private String resumeFileType;

    @Column(name = "resume_file_size")
    private Long resumeFileSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "candidate_status", nullable = false, length = 50)
    private RecruitmentCandidateStatus candidateStatus = RecruitmentCandidateStatus.SUBMITTED_BY_AGENCY;

    @Column(name = "department_shortlisted_at")
    private LocalDateTime departmentShortlistedAt;

    @Column(name = "department_shortlisted_by_user_id")
    private Long departmentShortlistedByUserId;

    @Column(name = "department_shortlist_remarks", length = 1000)
    private String departmentShortlistRemarks;

    @Column(name = "interview_scheduled_at")
    private LocalDateTime interviewScheduledAt;

    @Column(name = "interview_scheduled_by_user_id")
    private Long interviewScheduledByUserId;

    @Column(name = "interview_date_time")
    private LocalDateTime interviewDateTime;

    @Column(name = "interview_time_slot", length = 100)
    private String interviewTimeSlot;

    @Column(name = "interview_link", length = 700)
    private String interviewLink;

    @Column(name = "interview_remarks", length = 1000)
    private String interviewRemarks;

    @Column(name = "department_interview_change_requested", nullable = false)
    private Boolean departmentInterviewChangeRequested = false;

    @Column(name = "department_interview_change_reason", length = 1000)
    private String departmentInterviewChangeReason;

    @Column(name = "department_interview_change_requested_at")
    private LocalDateTime departmentInterviewChangeRequestedAt;

    @Column(name = "department_interview_change_requested_by_user_id")
    private Long departmentInterviewChangeRequestedByUserId;

    @Column(name = "assessment_submitted", nullable = false)
    private Boolean assessmentSubmitted = false;

    @Column(name = "assessment_submitted_at")
    private LocalDateTime assessmentSubmittedAt;

    @Column(name = "assessment_submitted_by_user_id")
    private Long assessmentSubmittedByUserId;

    @Column(name = "final_decision_status", length = 20)
    private String finalDecisionStatus;

    @Column(name = "final_decision_at")
    private LocalDateTime finalDecisionAt;

    @Column(name = "final_decision_by_user_id")
    private Long finalDecisionByUserId;

    @Column(name = "final_decision_remarks", length = 1000)
    private String finalDecisionRemarks;

    @OneToOne(mappedBy = "recruitmentInterviewDetail", fetch = FetchType.LAZY, orphanRemoval = true)
    private RecruitmentAssessmentFeedbackEntity assessmentFeedback;

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @PrePersist
    @PreUpdate
    void normalize() {
        candidateName = normalizeText(candidateName);
        candidateEmail = normalizeEmail(candidateEmail);
        candidateMobile = normalizeText(candidateMobile);
        candidateEducation = normalizeText(candidateEducation);
        joiningTime = normalizeText(joiningTime);
        resumeOriginalName = normalizeText(resumeOriginalName);
        resumeFilePath = normalizeText(resumeFilePath);
        resumeFileType = normalizeText(resumeFileType);
        interviewTimeSlot = normalizeText(interviewTimeSlot);
        interviewLink = normalizeText(interviewLink);
        interviewRemarks = normalizeText(interviewRemarks);
        departmentShortlistRemarks = normalizeText(departmentShortlistRemarks);
        departmentInterviewChangeReason = normalizeText(departmentInterviewChangeReason);
        finalDecisionStatus = normalizeText(finalDecisionStatus);
        finalDecisionRemarks = normalizeText(finalDecisionRemarks);

        if (candidateStatus == null) {
            candidateStatus = RecruitmentCandidateStatus.SUBMITTED_BY_AGENCY;
        }
        departmentInterviewChangeRequested = !Boolean.FALSE.equals(departmentInterviewChangeRequested);
        assessmentSubmitted = !Boolean.FALSE.equals(assessmentSubmitted);
        active = !Boolean.FALSE.equals(active);
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : value;
    }

    private String normalizeEmail(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase() : value;
    }
}
