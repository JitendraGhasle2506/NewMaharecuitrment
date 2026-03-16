package com.project.notification.service;

import java.util.List;

import com.project.notification.dto.NotificationCreateRequest;
import com.project.notification.dto.NotificationItemDto;

public interface NotificationService {

    void createNotification(String eventType, String message, Long refId, List<Long> userIds);

    void createNotification(NotificationCreateRequest request);

    List<NotificationItemDto> getNotificationsForUser(Long userId);

    long getTotalCount(Long userId);

    long getUnseenCount(Long userId);

    long getUnreadCount(Long userId);

    void markAsRead(Long notificationId);

    void markAsReadForUser(Long notificationId, Long userId);

    void markAllAsSeen(Long userId);
}
