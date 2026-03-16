package com.project.notification.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "notification_event",
        indexes = {
                @Index(name = "idx_notification_event_module", columnList = "module"),
                @Index(name = "idx_notification_event_created_at", columnList = "created_at")
        })
public class NotificationEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "message", nullable = false, length = 2000)
    private String message;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "module", nullable = false, length = 100)
    private String module;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NotificationRecipient> recipients = new ArrayList<>();

    public void addRecipient(NotificationRecipient recipient) {
        if (recipient == null) {
            return;
        }
        recipient.setEvent(this);
        recipients.add(recipient);
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (eventType != null) {
            eventType = eventType.trim();
        }
        if (title != null) {
            title = title.trim();
        }
        if (message != null) {
            message = message.trim();
        }
        if (module != null) {
            module = module.trim();
        }
    }
}
