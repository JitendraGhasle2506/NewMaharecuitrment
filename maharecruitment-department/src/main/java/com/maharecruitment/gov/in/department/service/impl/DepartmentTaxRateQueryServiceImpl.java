package com.maharecruitment.gov.in.department.service.impl;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.department.dto.DepartmentTaxRateView;
import com.maharecruitment.gov.in.department.repository.DepartmentTaxRateMasterRepository;
import com.maharecruitment.gov.in.department.service.DepartmentTaxRateQueryService;

@Service
@Transactional(readOnly = true)
public class DepartmentTaxRateQueryServiceImpl implements DepartmentTaxRateQueryService {

    private final DepartmentTaxRateMasterRepository taxRateMasterRepository;

    public DepartmentTaxRateQueryServiceImpl(DepartmentTaxRateMasterRepository taxRateMasterRepository) {
        this.taxRateMasterRepository = taxRateMasterRepository;
    }

    @Override
    public List<DepartmentTaxRateView> getApplicableTaxRates(LocalDate applicableDate) {
        LocalDate effectiveDate = applicableDate != null ? applicableDate : LocalDate.now();

        return taxRateMasterRepository.findApplicableTaxRates(effectiveDate)
                .stream()
                .map(taxRate -> DepartmentTaxRateView.builder()
                        .taxCode(taxRate.getTaxCode())
                        .taxName(taxRate.getTaxName())
                        .ratePercentage(taxRate.getRatePercentage())
                        .build())
                .toList();
    }
}
