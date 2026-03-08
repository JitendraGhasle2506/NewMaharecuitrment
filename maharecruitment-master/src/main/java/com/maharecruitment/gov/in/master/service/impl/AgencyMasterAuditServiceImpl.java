package com.maharecruitment.gov.in.master.service.impl;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.maharecruitment.gov.in.master.entity.AgencyMasterAuditAction;
import com.maharecruitment.gov.in.master.entity.AgencyMasterAuditLog;
import com.maharecruitment.gov.in.master.repository.AgencyMasterAuditLogRepository;
import com.maharecruitment.gov.in.master.service.AgencyMasterAuditService;
import com.maharecruitment.gov.in.master.service.CurrentActorProvider;

@Service
public class AgencyMasterAuditServiceImpl implements AgencyMasterAuditService {

    private final AgencyMasterAuditLogRepository auditLogRepository;
    private final CurrentActorProvider currentActorProvider;

    public AgencyMasterAuditServiceImpl(
            AgencyMasterAuditLogRepository auditLogRepository,
            CurrentActorProvider currentActorProvider) {
        this.auditLogRepository = auditLogRepository;
        this.currentActorProvider = currentActorProvider;
    }

    @Override
    public void log(Long agencyId, AgencyMasterAuditAction action, String details) {
        AgencyMasterAuditLog auditLog = new AgencyMasterAuditLog();
        auditLog.setAgencyId(agencyId);
        auditLog.setActionType(action);
        auditLog.setActorUserId(currentActorProvider.getCurrentUserId());
        auditLog.setActorUsername(resolveActorUsername());
        auditLog.setDetails(details);
        auditLogRepository.save(auditLog);
    }

    private String resolveActorUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return "SYSTEM";
        }
        return authentication.getName();
    }
}
