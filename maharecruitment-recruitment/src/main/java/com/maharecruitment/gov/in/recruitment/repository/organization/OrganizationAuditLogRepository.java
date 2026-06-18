package com.maharecruitment.gov.in.recruitment.repository.organization;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.recruitment.entity.organization.OrganizationAuditLogEntity;

@Repository
public interface OrganizationAuditLogRepository extends JpaRepository<OrganizationAuditLogEntity, Long> {

    List<OrganizationAuditLogEntity> findByEntityTypeAndEntityIdOrderByOccurredAtDescAuditIdDesc(
            String entityType,
            String entityId);
}
