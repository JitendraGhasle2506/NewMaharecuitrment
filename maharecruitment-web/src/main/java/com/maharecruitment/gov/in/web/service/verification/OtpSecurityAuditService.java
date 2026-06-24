package com.maharecruitment.gov.in.web.service.verification;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import com.maharecruitment.gov.in.audit.dto.AuditRecordRequest;
import com.maharecruitment.gov.in.audit.service.AuditTrailService;
import com.maharecruitment.gov.in.web.dto.verification.VerificationChannel;

@Service
public class OtpSecurityAuditService {

    private static final Logger log = LoggerFactory.getLogger(OtpSecurityAuditService.class);
    private static final String MODULE_NAME = "SECURITY";
    private static final String ENTITY_TYPE = "OTP";

    private final ObjectProvider<AuditTrailService> auditTrailServiceProvider;

    public OtpSecurityAuditService(ObjectProvider<AuditTrailService> auditTrailServiceProvider) {
        this.auditTrailServiceProvider = auditTrailServiceProvider;
    }

    public void record(
            String action,
            String purpose,
            VerificationChannel channel,
            String maskedReference,
            OtpRequestContext context,
            String detailReason,
            Map<String, Object> metadata) {
        String clientIp = context == null ? "unknown" : context.normalizedClientIp();
        Map<String, Object> auditMetadata = new LinkedHashMap<>();
        auditMetadata.put("purpose", purpose);
        auditMetadata.put("channel", channel == null ? "UNKNOWN" : channel.name());
        auditMetadata.put("reference", maskedReference);
        auditMetadata.put("clientIp", clientIp);
        if (detailReason != null) {
            auditMetadata.put("reason", detailReason);
        }
        if (metadata != null && !metadata.isEmpty()) {
            auditMetadata.putAll(metadata);
        }

        log.info(
                "OTP audit action={} purpose={} channel={} reference={} clientIp={} reason={}",
                action,
                purpose,
                channel,
                maskedReference,
                clientIp,
                detailReason);

        auditTrailServiceProvider.ifAvailable(auditTrailService -> safeRecord(
                auditTrailService,
                action,
                purpose,
                channel,
                maskedReference,
                detailReason,
                auditMetadata));
    }

    private void safeRecord(
            AuditTrailService auditTrailService,
            String action,
            String purpose,
            VerificationChannel channel,
            String maskedReference,
            String detailReason,
            Map<String, Object> auditMetadata) {
        try {
            auditTrailService.record(AuditRecordRequest.builder()
                    .moduleName(MODULE_NAME)
                    .entityType(ENTITY_TYPE)
                    .entityId(buildEntityId(purpose, channel, maskedReference))
                    .actionType(action)
                    .actorLoginId("anonymous")
                    .actorName("Anonymous")
                    .activitySummary(action.replace('_', ' '))
                    .activityDetails(detailReason)
                    .metadata(auditMetadata)
                    .build());
        } catch (RuntimeException ex) {
            log.warn("Unable to persist OTP audit event. action={} purpose={}", action, purpose, ex);
        }
    }

    private String buildEntityId(String purpose, VerificationChannel channel, String maskedReference) {
        String rawEntityId = String.join(
                ":",
                purpose == null ? "unknown" : purpose,
                channel == null ? "UNKNOWN" : channel.name(),
                maskedReference == null ? "unknown" : maskedReference);
        return rawEntityId.length() <= 80 ? rawEntityId : rawEntityId.substring(0, 80);
    }
}
