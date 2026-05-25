package com.maharecruitment.gov.in.master.service;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.maharecruitment.gov.in.master.dto.CommissionRateAuditLogResponse;
import com.maharecruitment.gov.in.master.dto.CommissionRateRequest;
import com.maharecruitment.gov.in.master.dto.CommissionRateResponse;
import com.maharecruitment.gov.in.master.entity.CommissionCode;

public interface CommissionRateMasterService {

    CommissionRateResponse create(CommissionRateRequest request);

    CommissionRateResponse update(Long commissionRateId, CommissionRateRequest request);

    CommissionRateResponse getById(Long commissionRateId, boolean includeInactive);

    Page<CommissionRateResponse> getAll(CommissionCode commissionCode, boolean includeInactive, Pageable pageable);

    CommissionRateResponse getApplicableRate(CommissionCode commissionCode, LocalDate effectiveDate);

    Page<CommissionRateAuditLogResponse> getAuditLogs(Long commissionRateId, Pageable pageable);

    void softDelete(Long commissionRateId);

    void restore(Long commissionRateId);
}
