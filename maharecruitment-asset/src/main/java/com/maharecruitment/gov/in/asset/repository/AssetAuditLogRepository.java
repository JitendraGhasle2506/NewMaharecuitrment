package com.maharecruitment.gov.in.asset.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.maharecruitment.gov.in.asset.entity.AssetAuditLogEntity;

public interface AssetAuditLogRepository extends JpaRepository<AssetAuditLogEntity, Long> {
}
