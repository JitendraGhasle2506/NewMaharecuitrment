package com.maharecruitment.gov.in.master.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.maharecruitment.gov.in.master.entity.AgencyMasterAuditLog;

public interface AgencyMasterAuditLogRepository extends JpaRepository<AgencyMasterAuditLog, Long> {
}
