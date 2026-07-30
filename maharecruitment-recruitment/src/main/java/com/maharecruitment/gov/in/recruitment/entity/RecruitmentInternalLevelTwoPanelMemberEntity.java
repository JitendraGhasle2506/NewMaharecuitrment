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
        name = "recruitment_internal_level_two_panel_member",
        indexes = {
                @Index(
                        name = "idx_internal_level_two_panel_member_schedule",
                        columnList = "recruitment_internal_level_two_schedule_id"),
                @Index(
                        name = "idx_internal_level_two_panel_member_user",
                        columnList = "panel_user_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecruitmentInternalLevelTwoPanelMemberEntity extends RecruitmentAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recruitment_internal_level_two_panel_member_id")
    private Long recruitmentInternalLevelTwoPanelMemberId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recruitment_internal_level_two_schedule_id", nullable = false)
    private RecruitmentInternalLevelTwoScheduleEntity schedule;

    @Column(name = "panel_user_id")
    private Long panelUserId;

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
