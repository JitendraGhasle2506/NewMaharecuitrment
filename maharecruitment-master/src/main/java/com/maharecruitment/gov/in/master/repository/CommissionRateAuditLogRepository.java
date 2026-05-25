package com.maharecruitment.gov.in.master.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.master.entity.CommissionRateAuditLog;

@Repository
public interface CommissionRateAuditLogRepository extends JpaRepository<CommissionRateAuditLog, Long> {

    Page<CommissionRateAuditLog> findByCommissionRateIdOrderByActionTimestampDesc(
            Long commissionRateId,
            Pageable pageable);
}
