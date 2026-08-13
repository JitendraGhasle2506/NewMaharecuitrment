package com.maharecruitment.gov.in.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.maharecruitment.gov.in.auth.entity.LoginLogoutAuditHistory;

public interface LoginLogoutAuditHistoryRepository extends JpaRepository<LoginLogoutAuditHistory, Long> {
}
