package com.maharecruitment.gov.in.common.mahaitprofile.service.impl;

import org.springframework.stereotype.Service;

import com.maharecruitment.gov.in.common.mahaitprofile.entity.MahaItProfileAuditAction;
import com.maharecruitment.gov.in.common.mahaitprofile.entity.MahaItProfileAuditLog;
import com.maharecruitment.gov.in.common.mahaitprofile.repository.MahaItProfileAuditLogRepository;
import com.maharecruitment.gov.in.common.mahaitprofile.service.MahaItProfileAuditService;
import com.maharecruitment.gov.in.common.service.CurrentActorProvider;

@Service
public class MahaItProfileAuditServiceImpl implements MahaItProfileAuditService {

    private final MahaItProfileAuditLogRepository auditLogRepository;
    private final CurrentActorProvider currentActorProvider;

    public MahaItProfileAuditServiceImpl(
            MahaItProfileAuditLogRepository auditLogRepository,
            CurrentActorProvider currentActorProvider) {
        this.auditLogRepository = auditLogRepository;
        this.currentActorProvider = currentActorProvider;
    }

    @Override
    public void log(Long mahaitProfileId, MahaItProfileAuditAction action, String details) {
        MahaItProfileAuditLog auditLog = new MahaItProfileAuditLog();
        auditLog.setMahaItProfileId(mahaitProfileId);
        auditLog.setActionType(action);
        auditLog.setActorUserId(currentActorProvider.getCurrentUserId());
        auditLog.setActorUsername(currentActorProvider.getCurrentActorEmail());
        auditLog.setDetails(details);
        auditLogRepository.save(auditLog);
    }
}
