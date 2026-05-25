package com.maharecruitment.gov.in.master.service;

import com.maharecruitment.gov.in.master.entity.CommissionRateAuditAction;

public interface CommissionRateAuditService {

    void log(Long commissionRateId, CommissionRateAuditAction action, String details);
}
