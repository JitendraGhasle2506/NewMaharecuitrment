package com.maharecruitment.gov.in.common.sms.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.maharecruitment.gov.in.common.sms.entity.SmsTransactionLogEntity;

public interface SmsTransactionLogRepository extends JpaRepository<SmsTransactionLogEntity, Long> {
}
