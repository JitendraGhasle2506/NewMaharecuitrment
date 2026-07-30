package com.maharecruitment.gov.in.master.service.impl;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.maharecruitment.gov.in.master.entity.CommissionRateAuditAction;
import com.maharecruitment.gov.in.master.entity.CommissionRateAuditLog;
import com.maharecruitment.gov.in.master.repository.CommissionRateAuditLogRepository;
import com.maharecruitment.gov.in.master.service.CommissionRateAuditService;
import com.maharecruitment.gov.in.master.service.CurrentActorProvider;

@Service
public class CommissionRateAuditServiceImpl implements CommissionRateAuditService {

    private final CommissionRateAuditLogRepository auditLogRepository;
    private final CurrentActorProvider currentActorProvider;

    public CommissionRateAuditServiceImpl(
            CommissionRateAuditLogRepository auditLogRepository,
            CurrentActorProvider currentActorProvider) {
        this.auditLogRepository = auditLogRepository;
        this.currentActorProvider = currentActorProvider;
    }

    @Override
    public void log(Long commissionRateId, CommissionRateAuditAction action, String details) {
        CommissionRateAuditLog auditLog = new CommissionRateAuditLog();
        auditLog.setCommissionRateId(commissionRateId);
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
