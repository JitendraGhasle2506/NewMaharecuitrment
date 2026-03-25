package com.maharecruitment.gov.in.recruitment.entity;

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
import jakarta.persistence.OrderBy;
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
        name = "recruitment_internal_level_two_schedule",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_internal_level_two_schedule_candidate",
                        columnNames = "recruitment_interview_detail_id")
        },
        indexes = {
                @Index(
                        name = "idx_internal_level_two_schedule_candidate",
                        columnList = "recruitment_interview_detail_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecruitmentInternalLevelTwoScheduleEntity extends RecruitmentAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recruitment_internal_level_two_schedule_id")
    private Long recruitmentInternalLevelTwoScheduleId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recruitment_interview_detail_id", nullable = false, unique = true)
    private RecruitmentInterviewDetailEntity recruitmentInterviewDetail;

    @Column(name = "scheduled_by_user_id", nullable = false)
    private Long scheduledByUserId;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Column(name = "interview_date_time", nullable = false)
    private LocalDateTime interviewDateTime;

    @Column(name = "interview_time_slot", nullable = false, length = 100)
    private String interviewTimeSlot;

    @Column(name = "meeting_link", length = 700)
    private String meetingLink;

    @Column(name = "remarks", length = 1000)
    private String remarks;

    @Column(name = "panel_assigned_by_user_id")
    private Long panelAssignedByUserId;

    @Column(name = "panel_assigned_at")
    private LocalDateTime panelAssignedAt;

    @Column(name = "hr_time_change_requested", nullable = false)
    private Boolean hrTimeChangeRequested = false;

    @Column(name = "hr_time_change_reason", length = 1000)
    private String hrTimeChangeReason;

    @Column(name = "hr_time_change_requested_at")
    private LocalDateTime hrTimeChangeRequestedAt;

    @Column(name = "hr_time_change_requested_by_user_id")
    private Long hrTimeChangeRequestedByUserId;

    @OneToMany(
            mappedBy = "schedule",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    @OrderBy("recruitmentInternalLevelTwoPanelMemberId asc")
    private List<RecruitmentInternalLevelTwoPanelMemberEntity> panelMembers = new ArrayList<>();

    @PrePersist
    @PreUpdate
    void normalize() {
        interviewTimeSlot = normalizeText(interviewTimeSlot);
        meetingLink = normalizeText(meetingLink);
        remarks = normalizeText(remarks);
        hrTimeChangeReason = normalizeText(hrTimeChangeReason);
        hrTimeChangeRequested = !Boolean.FALSE.equals(hrTimeChangeRequested);
    }

    public void replacePanelMembers(List<RecruitmentInternalLevelTwoPanelMemberEntity> members) {
        panelMembers.clear();
        if (members == null || members.isEmpty()) {
            return;
        }

        for (RecruitmentInternalLevelTwoPanelMemberEntity member : members) {
            if (member == null) {
                continue;
            }
            member.setSchedule(this);
            panelMembers.add(member);
        }
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : value;
    }
}
