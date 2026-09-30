package com.maharecruitment.gov.in.web.repository.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import com.maharecruitment.gov.in.web.entity.notification.SmtpConfigurationEntity;

public interface SmtpConfigurationRepository extends JpaRepository<SmtpConfigurationEntity, Short> {
}
