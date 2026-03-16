package com.project.notification.service.impl;

import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.project.notification.dto.NotificationCreateRequest;
import com.project.notification.dto.NotificationItemDto;
import com.project.notification.entity.NotificationEvent;
import com.project.notification.entity.NotificationRecipient;
import com.project.notification.entity.NotificationStatus;
import com.project.notification.repository.NotificationEventRepository;
import com.project.notification.repository.NotificationRecipientRepository;
import com.project.notification.service.NotificationModules;
import com.project.notification.service.NotificationRedirectResolver;
import com.project.notification.service.NotificationService;

@Service("portalNotificationService")
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    private static final int DEFAULT_PAGE_SIZE = 10;

    private final NotificationEventRepository eventRepository;
    private final NotificationRecipientRepository recipientRepository;
    private final NotificationRedirectResolver redirectResolver;

    public NotificationServiceImpl(
            NotificationEventRepository eventRepository,
            NotificationRecipientRepository recipientRepository,
            NotificationRedirectResolver redirectResolver) {
        this.eventRepository = eventRepository;
        this.recipientRepository = recipientRepository;
        this.redirectResolver = redirectResolver;
    }

    @Override
    @Transactional
    public void createNotification(String eventType, String message, Long refId, List<Long> userIds) {
        NotificationCreateRequest request = NotificationCreateRequest.builder()
                .eventType(eventType)
                .title(eventType)
                .message(message)
                .referenceId(refId)
                .module(NotificationModules.GENERIC)
                .userIds(userIds)
                .actionRequired(false)
                .build();
        createNotification(request);
    }

    @Override
    @Transactional
    public void createNotification(NotificationCreateRequest request) {
        if (request == null || !StringUtils.hasText(request.getEventType()) || !StringUtils.hasText(request.getMessage())) {
            return;
        }

        List<Long> recipients = request.getUserIds() == null
                ? List.of()
                : request.getUserIds().stream()
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();

        if (recipients.isEmpty()) {
            return;
        }

        NotificationEvent event = new NotificationEvent();
        event.setEventType(request.getEventType());
        event.setTitle(StringUtils.hasText(request.getTitle()) ? request.getTitle() : request.getEventType());
        event.setMessage(request.getMessage());
        event.setReferenceId(request.getReferenceId());
        event.setModule(StringUtils.hasText(request.getModule()) ? request.getModule() : NotificationModules.GENERIC);

        for (Long userId : recipients) {
            NotificationRecipient recipient = new NotificationRecipient();
            recipient.setUserId(userId);
            recipient.setStatus(NotificationStatus.UNREAD);
            recipient.setSeen(false);
            recipient.setActionRequired(request.isActionRequired());
            event.addRecipient(recipient);
        }

        eventRepository.save(event);
    }

    @Override
    public List<NotificationItemDto> getNotificationsForUser(Long userId) {
        if (userId == null) {
            return List.of();
        }

        PageRequest pageRequest = PageRequest.of(
                0,
                DEFAULT_PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        return recipientRepository.findByUserIdOrderByCreatedAtDesc(userId, pageRequest)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public long getTotalCount(Long userId) {
        if (userId == null) {
            return 0;
        }
        return recipientRepository.countByUserId(userId);
    }

    @Override
    public long getUnseenCount(Long userId) {
        if (userId == null) {
            return 0;
        }
        return recipientRepository.countByUserIdAndSeenFalse(userId);
    }

    @Override
    public long getUnreadCount(Long userId) {
        if (userId == null) {
            return 0;
        }
        return recipientRepository.countByUserIdAndStatusNot(userId, NotificationStatus.READ);
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId) {
        if (notificationId == null) {
            return;
        }

        recipientRepository.findById(notificationId)
                .ifPresent(recipient -> {
                    if (recipient.getStatus() != NotificationStatus.READ) {
                        recipient.markAsRead();
                        recipientRepository.save(recipient);
                    }
                });
    }

    @Override
    @Transactional
    public void markAsReadForUser(Long notificationId, Long userId) {
        if (notificationId == null || userId == null) {
            return;
        }

        recipientRepository.findByIdAndUserId(notificationId, userId)
                .ifPresent(recipient -> {
                    if (recipient.getStatus() != NotificationStatus.READ) {
                        recipient.markAsRead();
                        recipientRepository.save(recipient);
                    }
                });
    }

    @Override
    @Transactional
    public void markAllAsSeen(Long userId) {
        if (userId == null) {
            return;
        }

        List<NotificationRecipient> unreadRecipients = recipientRepository.findByUserIdAndStatus(
                userId,
                NotificationStatus.UNREAD);
        if (unreadRecipients.isEmpty()) {
            return;
        }

        unreadRecipients.forEach(NotificationRecipient::markAsSeen);
        recipientRepository.saveAll(unreadRecipients);
    }

    private NotificationItemDto toDto(NotificationRecipient recipient) {
        NotificationEvent event = recipient.getEvent();
        boolean unread = recipient.getStatus() == NotificationStatus.UNREAD;

        return NotificationItemDto.builder()
                .id(recipient.getId())
                .eventId(event != null ? event.getId() : null)
                .eventType(event != null ? event.getEventType() : null)
                .title(event != null ? event.getTitle() : null)
                .message(event != null ? event.getMessage() : null)
                .referenceId(event != null ? event.getReferenceId() : null)
                .module(event != null ? event.getModule() : null)
                .status(recipient.getStatus())
                .unread(unread)
                .seen(recipient.isSeen())
                .actionRequired(recipient.isActionRequired())
                .createdAt(event != null ? event.getCreatedAt() : recipient.getCreatedAt())
                .redirectUrl(redirectResolver.resolve(event != null ? event.getModule() : null,
                        event != null ? event.getReferenceId() : null))
                .build();
    }
}
