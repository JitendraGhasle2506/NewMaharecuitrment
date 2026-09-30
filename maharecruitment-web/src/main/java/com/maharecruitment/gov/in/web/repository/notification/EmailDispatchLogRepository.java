package com.maharecruitment.gov.in.web.repository.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import com.maharecruitment.gov.in.web.entity.notification.EmailDispatchLogEntity;

public interface EmailDispatchLogRepository extends JpaRepository<EmailDispatchLogEntity, Long> {
}
