package com.maharecruitment.gov.in.master.service;

import com.maharecruitment.gov.in.master.entity.AgencyMasterAuditAction;

public interface AgencyMasterAuditService {

    void log(Long agencyId, AgencyMasterAuditAction action, String details);
}
