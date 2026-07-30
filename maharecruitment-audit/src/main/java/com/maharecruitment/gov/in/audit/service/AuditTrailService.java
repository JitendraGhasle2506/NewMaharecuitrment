package com.maharecruitment.gov.in.audit.service;

import java.util.List;

import com.maharecruitment.gov.in.audit.dto.AuditEventView;
import com.maharecruitment.gov.in.audit.dto.AuditRecordRequest;

public interface AuditTrailService {

    void record(AuditRecordRequest request);

    List<AuditEventView> getTimeline(String moduleName, String entityType, String entityId);
}
