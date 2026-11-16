package com.maharecruitment.gov.in.attendance.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.maharecruitment.gov.in.attendance.entity.InternalApproverRoleAuditLogEntity;

public interface InternalApproverRoleAuditLogRepository extends JpaRepository<InternalApproverRoleAuditLogEntity, Long> {

    Page<InternalApproverRoleAuditLogEntity> findAllByOrderByChangedAtDescIdDesc(Pageable pageable);
}
