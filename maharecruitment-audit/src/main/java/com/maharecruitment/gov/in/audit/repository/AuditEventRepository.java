package com.maharecruitment.gov.in.audit.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.maharecruitment.gov.in.audit.entity.AuditEventEntity;

public interface AuditEventRepository extends JpaRepository<AuditEventEntity, Long> {

    List<AuditEventEntity> findByModuleNameAndEntityTypeAndEntityIdOrderByOccurredAtDescAuditEventIdDesc(
            String moduleName,
            String entityType,
            String entityId);
}
