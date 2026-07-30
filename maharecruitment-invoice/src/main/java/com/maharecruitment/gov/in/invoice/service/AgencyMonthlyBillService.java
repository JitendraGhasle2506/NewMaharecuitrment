package com.maharecruitment.gov.in.invoice.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.maharecruitment.gov.in.invoice.dto.AgencyMonthlyBillGenerateRequest;
import com.maharecruitment.gov.in.invoice.dto.AgencyMonthlyBillListItemView;
import com.maharecruitment.gov.in.invoice.dto.AgencyMonthlyBillView;

public interface AgencyMonthlyBillService {

    Page<AgencyMonthlyBillListItemView> getGeneratedBills(Pageable pageable);

    Page<AgencyMonthlyBillListItemView> getGeneratedBillsForAgency(Long agencyId, Pageable pageable);

    AgencyMonthlyBillView getBill(Long billId);

    AgencyMonthlyBillView getBillForAgency(Long billId, Long agencyId);

    AgencyMonthlyBillView preview(AgencyMonthlyBillGenerateRequest request);

    AgencyMonthlyBillView generate(AgencyMonthlyBillGenerateRequest request, String actorEmail);

    void softDelete(Long billId, String actorEmail);
}
