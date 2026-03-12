package com.maharecruitment.gov.in.recruitment.entity;

import java.time.LocalDateTime;

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
        name = "agency_notification_tracking",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_agency_notification_tracking_notification_agency",
                        columnNames = { "recruitment_notification_id", "agency_id" })
        },
        indexes = {
                @Index(
                        name = "idx_agency_notification_tracking_notification",
                        columnList = "recruitment_notification_id"),
                @Index(name = "idx_agency_notification_tracking_agency", columnList = "agency_id"),
                @Index(name = "idx_agency_notification_tracking_status", columnList = "status"),
                @Index(
                        name = "idx_agency_notification_tracking_notification_rank",
                        columnList = "recruitment_notification_id,released_rank")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AgencyNotificationTrackingEntity extends RecruitmentAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "agency_notification_tracking_id")
    private Long agencyNotificationTrackingId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recruitment_notification_id", nullable = false)
    private RecruitmentNotificationEntity recruitmentNotification;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agency_id", nullable = false)
    private AgencyMaster agency;

    @Column(name = "released_rank", nullable = false)
    private Integer releasedRank;

    @Column(name = "notified_at", nullable = false)
    private LocalDateTime notifiedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private AgencyNotificationTrackingStatus status = AgencyNotificationTrackingStatus.RELEASED;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @PrePersist
    @PreUpdate
    void normalize() {
        if (releasedRank == null || releasedRank < 1) {
            throw new IllegalStateException("Released rank must be at least 1.");
        }
        if (notifiedAt == null) {
            notifiedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = AgencyNotificationTrackingStatus.RELEASED;
        }
    }
}
