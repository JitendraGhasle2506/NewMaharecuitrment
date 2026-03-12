package com.maharecruitment.gov.in.recruitment.entity;

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
        name = "recruitment_assessment_panel_member",
        indexes = {
                @Index(name = "idx_assessment_panel_member_assessment", columnList = "recruitment_assessment_feedback_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecruitmentAssessmentPanelMemberEntity extends RecruitmentAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recruitment_assessment_panel_member_id")
    private Long recruitmentAssessmentPanelMemberId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recruitment_assessment_feedback_id", nullable = false)
    private RecruitmentAssessmentFeedbackEntity assessmentFeedback;

    @Column(name = "panel_member_name", nullable = false, length = 150)
    private String panelMemberName;

    @Column(name = "panel_member_designation", nullable = false, length = 150)
    private String panelMemberDesignation;

    @PrePersist
    @PreUpdate
    void normalize() {
        panelMemberName = normalizeText(panelMemberName);
        panelMemberDesignation = normalizeText(panelMemberDesignation);
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : value;
    }
}
