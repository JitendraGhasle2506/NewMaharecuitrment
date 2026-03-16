package com.project.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.notification.entity.NotificationEvent;

@Repository
public interface NotificationEventRepository extends JpaRepository<NotificationEvent, Long> {
}
