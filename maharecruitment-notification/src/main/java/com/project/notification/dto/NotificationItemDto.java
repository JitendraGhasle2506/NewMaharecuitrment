package com.project.notification.dto;

import java.time.LocalDateTime;

import com.project.notification.entity.NotificationStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationItemDto {
    private final Long id;
    private final Long eventId;
    private final String eventType;
    private final String title;
    private final String message;
    private final Long referenceId;
    private final String module;
    private final NotificationStatus status;
    private final boolean unread;
    private final boolean seen;
    private final boolean actionRequired;
    private final LocalDateTime createdAt;
    private final String redirectUrl;
}
