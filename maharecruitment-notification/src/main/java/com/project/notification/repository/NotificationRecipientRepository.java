package com.project.notification.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.notification.entity.NotificationRecipient;
import com.project.notification.entity.NotificationStatus;

@Repository
public interface NotificationRecipientRepository extends JpaRepository<NotificationRecipient, Long> {

    @EntityGraph(attributePaths = "event")
    List<NotificationRecipient> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<NotificationRecipient> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);

    long countByUserIdAndSeenFalse(Long userId);

    long countByUserIdAndStatusNot(Long userId, NotificationStatus status);

    long countByUserIdAndStatus(Long userId, NotificationStatus status);

    List<NotificationRecipient> findByUserIdAndStatus(Long userId, NotificationStatus status);
}
