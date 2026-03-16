package com.project.notification.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationCreateRequest {
    private final String eventType;
    private final String title;
    private final String message;
    private final Long referenceId;
    private final String module;
    private final List<Long> userIds;
    private final boolean actionRequired;
}
