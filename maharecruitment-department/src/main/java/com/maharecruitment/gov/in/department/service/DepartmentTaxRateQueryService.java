package com.maharecruitment.gov.in.department.service;

import java.time.LocalDate;
import java.util.List;

import com.maharecruitment.gov.in.department.dto.DepartmentTaxRateView;

public interface DepartmentTaxRateQueryService {

    List<DepartmentTaxRateView> getApplicableTaxRates(LocalDate applicableDate);
}
