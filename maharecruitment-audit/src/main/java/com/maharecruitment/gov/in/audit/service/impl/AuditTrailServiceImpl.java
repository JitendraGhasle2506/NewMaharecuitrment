package com.maharecruitment.gov.in.audit.service.impl;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maharecruitment.gov.in.audit.dto.AuditEventView;
import com.maharecruitment.gov.in.audit.dto.AuditRecordRequest;
import com.maharecruitment.gov.in.audit.entity.AuditEventEntity;
import com.maharecruitment.gov.in.audit.repository.AuditEventRepository;
import com.maharecruitment.gov.in.audit.service.AuditTrailService;
import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.service.UserService;

@Service
@Transactional(readOnly = true)
public class AuditTrailServiceImpl implements AuditTrailService {

    private static final Logger log = LoggerFactory.getLogger(AuditTrailServiceImpl.class);

    private static final String DEFAULT_ACTOR_NAME = "System";

    private final AuditEventRepository auditEventRepository;
    private final UserService userService;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public AuditTrailServiceImpl(
            AuditEventRepository auditEventRepository,
            UserService userService) {
        this.auditEventRepository = auditEventRepository;
        this.userService = userService;
    }

    @Override
    @Transactional
    public void record(AuditRecordRequest request) {
        validateRequest(request);

        String actorLoginId = trimToNull(request.getActorLoginId());
        User user = actorLoginId == null ? null : userService.findUserByEmail(actorLoginId);

        String actorName = trimToNull(request.getActorName());
        if (actorName == null && user != null) {
            actorName = trimToNull(user.getName());
        }
        if (actorName == null) {
            actorName = actorLoginId != null ? actorLoginId : DEFAULT_ACTOR_NAME;
        }

        Long actorUserId = request.getActorUserId();
        if (actorUserId == null && user != null) {
            actorUserId = user.getId();
        }

        AuditEventEntity entity = AuditEventEntity.builder()
                .moduleName(request.getModuleName().trim())
                .entityType(request.getEntityType().trim())
                .entityId(request.getEntityId().trim())
                .actionType(request.getActionType().trim())
                .actorUserId(actorUserId)
                .actorLoginId(actorLoginId)
                .actorName(actorName)
                .activitySummary(request.getActivitySummary().trim())
                .activityDetails(trimToNull(request.getActivityDetails()))
                .metadataJson(toJson(request.getMetadata()))
                .build();

        auditEventRepository.save(entity);
    }

    @Override
    public List<AuditEventView> getTimeline(String moduleName, String entityType, String entityId) {
        String normalizedModuleName = requireValue(moduleName, "moduleName");
        String normalizedEntityType = requireValue(entityType, "entityType");
        String normalizedEntityId = requireValue(entityId, "entityId");

        return auditEventRepository
                .findByModuleNameAndEntityTypeAndEntityIdOrderByOccurredAtDescAuditEventIdDesc(
                        normalizedModuleName,
                        normalizedEntityType,
                        normalizedEntityId)
                .stream()
                .map(this::toView)
                .toList();
    }

    private AuditEventView toView(AuditEventEntity entity) {
        return AuditEventView.builder()
                .auditEventId(entity.getAuditEventId())
                .moduleName(entity.getModuleName())
                .entityType(entity.getEntityType())
                .entityId(entity.getEntityId())
                .actionType(entity.getActionType())
                .actorUserId(entity.getActorUserId())
                .actorLoginId(entity.getActorLoginId())
                .actorName(entity.getActorName())
                .activitySummary(entity.getActivitySummary())
                .activityDetails(entity.getActivityDetails())
                .metadataJson(entity.getMetadataJson())
                .occurredAt(entity.getOccurredAt())
                .build();
    }

    private void validateRequest(AuditRecordRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Audit record request is required.");
        }

        requireValue(request.getModuleName(), "moduleName");
        requireValue(request.getEntityType(), "entityType");
        requireValue(request.getEntityId(), "entityId");
        requireValue(request.getActionType(), "actionType");
        requireValue(request.getActivitySummary(), "activitySummary");
    }

    private String requireValue(String value, String fieldName) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String toJson(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException ex) {
            log.warn("Unable to serialize audit metadata. metadataKeys={}", metadata.keySet(), ex);
            return null;
        }
    }
}
