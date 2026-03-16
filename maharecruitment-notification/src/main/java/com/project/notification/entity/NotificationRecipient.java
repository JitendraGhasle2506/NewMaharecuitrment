package com.project.notification.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "notification_recipient",
        indexes = {
                @Index(name = "idx_notification_recipient_user_id", columnList = "user_id"),
                @Index(name = "idx_notification_recipient_user_status", columnList = "user_id,status"),
                @Index(name = "idx_notification_recipient_event_id", columnList = "event_id"),
                @Index(name = "idx_notification_recipient_created_at", columnList = "created_at")
        })
public class NotificationRecipient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false, foreignKey = @ForeignKey(name = "fk_notification_recipient_event"))
    private NotificationEvent event;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private NotificationStatus status = NotificationStatus.UNREAD;

    @Column(name = "is_seen", nullable = false)
    private boolean seen = false;

    @Column(name = "action_required", nullable = false)
    private boolean actionRequired = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (status == null) {
            status = NotificationStatus.UNREAD;
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == NotificationStatus.UNREAD) {
            seen = false;
            readAt = null;
        }
    }

    public void markAsSeen() {
        if (status == NotificationStatus.UNREAD) {
            status = NotificationStatus.SEEN;
        }
        seen = true;
    }

    public void markAsRead() {
        status = NotificationStatus.READ;
        seen = true;
        readAt = LocalDateTime.now();
    }
}
