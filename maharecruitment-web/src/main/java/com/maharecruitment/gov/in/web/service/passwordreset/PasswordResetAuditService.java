package com.maharecruitment.gov.in.web.service.passwordreset;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import com.maharecruitment.gov.in.audit.dto.AuditRecordRequest;
import com.maharecruitment.gov.in.audit.service.AuditTrailService;
import com.maharecruitment.gov.in.web.entity.passwordreset.PasswordResetRequestEntity;

@Service
public class PasswordResetAuditService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetAuditService.class);
    private static final String MODULE_NAME = "SECURITY";
    private static final String ENTITY_TYPE = "PASSWORD_RESET";

    private final ObjectProvider<AuditTrailService> auditTrailServiceProvider;

    public PasswordResetAuditService(ObjectProvider<AuditTrailService> auditTrailServiceProvider) {
        this.auditTrailServiceProvider = auditTrailServiceProvider;
    }

    public void record(
            String action,
            PasswordResetRequestEntity request,
            ResetPasswordChannel channel,
            String clientIp,
            String reasonCode,
            Map<String, Object> metadata) {
        Map<String, Object> auditMetadata = new LinkedHashMap<>();
        auditMetadata.put("channel", channel == null ? "UNKNOWN" : channel.name());
        auditMetadata.put("clientIp", clientIp == null ? "unknown" : clientIp);
        if (request != null && request.getId() != null) {
            auditMetadata.put("resetRequestId", request.getId());
        }
        if (request != null && request.getUser() != null && request.getUser().getId() != null) {
            auditMetadata.put("userId", request.getUser().getId());
        }
        if (reasonCode != null) {
            auditMetadata.put("reasonCode", reasonCode);
        }
        if (metadata != null && !metadata.isEmpty()) {
            auditMetadata.putAll(metadata);
        }

        log.info(
                "Password reset audit action={} requestId={} userId={} channel={} clientIp={} reason={}",
                action,
                request == null ? null : request.getId(),
                request == null || request.getUser() == null ? null : request.getUser().getId(),
                channel,
                clientIp,
                reasonCode);

        auditTrailServiceProvider.ifAvailable(auditTrailService -> safeRecord(
                auditTrailService,
                action,
                request,
                reasonCode,
                auditMetadata));
    }

    public void recordSuppressed(
            String action,
            ResetPasswordChannel channel,
            String clientIp,
            String reasonCode,
            Map<String, Object> metadata) {
        record(action, null, channel, clientIp, reasonCode, metadata);
    }

    private void safeRecord(
            AuditTrailService auditTrailService,
            String action,
            PasswordResetRequestEntity request,
            String reasonCode,
            Map<String, Object> auditMetadata) {
        try {
            auditTrailService.record(AuditRecordRequest.builder()
                    .moduleName(MODULE_NAME)
                    .entityType(ENTITY_TYPE)
                    .entityId(request == null || request.getId() == null
                            ? "anonymous"
                            : request.getId().toString())
                    .actionType(action)
                    .actorLoginId("anonymous")
                    .actorName("Anonymous")
                    .activitySummary(action.replace('_', ' '))
                    .activityDetails(reasonCode)
                    .metadata(auditMetadata)
                    .build());
        } catch (RuntimeException ex) {
            log.warn("Unable to persist password reset audit event. action={}", action, ex);
        }
    }
}
