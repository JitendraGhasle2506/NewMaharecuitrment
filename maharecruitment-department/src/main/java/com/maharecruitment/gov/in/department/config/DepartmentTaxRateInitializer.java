package com.maharecruitment.gov.in.department.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.department.entity.DepartmentTaxRateMasterEntity;
import com.maharecruitment.gov.in.department.repository.DepartmentTaxRateMasterRepository;

@Component
@Order(42)
public class DepartmentTaxRateInitializer implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(DepartmentTaxRateInitializer.class);

    private static final ZoneId INDIA_ZONE_ID = ZoneId.of("Asia/Kolkata");
    private static final BigDecimal DEFAULT_TAX_RATE = new BigDecimal("9.0000");

    private static final String TAX_CODE_SGST = "SGST";
    private static final String TAX_CODE_CGST = "CGST";

    private final DepartmentTaxRateMasterRepository taxRateMasterRepository;

    public DepartmentTaxRateInitializer(DepartmentTaxRateMasterRepository taxRateMasterRepository) {
        this.taxRateMasterRepository = taxRateMasterRepository;
    }

    @Override
    @Transactional
    public void afterPropertiesSet() {
        FinancialYear currentFinancialYear = resolveCurrentFinancialYear();

        List<TaxRateSeed> taxRatesToSeed = List.of(
                new TaxRateSeed(TAX_CODE_SGST, "State Goods and Services Tax", DEFAULT_TAX_RATE),
                new TaxRateSeed(TAX_CODE_CGST, "Central Goods and Services Tax", DEFAULT_TAX_RATE));

        for (TaxRateSeed taxRateSeed : taxRatesToSeed) {
            upsertTaxRate(taxRateSeed, currentFinancialYear);
        }

        log.info(
                "Tax rates ensured for financial year {} to {}. Seeded tax codes: {}",
                currentFinancialYear.startDate(),
                currentFinancialYear.endDate(),
                taxRatesToSeed.stream().map(TaxRateSeed::taxCode).toList());
    }

    private void upsertTaxRate(TaxRateSeed taxRateSeed, FinancialYear financialYear) {
        Optional<DepartmentTaxRateMasterEntity> existingTaxRate = taxRateMasterRepository
                .findFirstByTaxCodeIgnoreCaseAndEffectiveFromOrderByDepartmentTaxRateMasterIdAsc(
                        taxRateSeed.taxCode(),
                        financialYear.startDate());

        DepartmentTaxRateMasterEntity taxRateEntity = existingTaxRate.orElseGet(DepartmentTaxRateMasterEntity::new);
        taxRateEntity.setTaxCode(taxRateSeed.taxCode());
        taxRateEntity.setTaxName(taxRateSeed.taxName());
        taxRateEntity.setRatePercentage(taxRateSeed.ratePercentage());
        taxRateEntity.setEffectiveFrom(financialYear.startDate());
        taxRateEntity.setEffectiveTo(financialYear.endDate());
        taxRateEntity.setActive(Boolean.TRUE);

        DepartmentTaxRateMasterEntity savedTaxRate = taxRateMasterRepository.save(taxRateEntity);
        log.info(
                "Tax master upserted. id={}, taxCode={}, ratePercentage={}, effectiveFrom={}, effectiveTo={}",
                savedTaxRate.getDepartmentTaxRateMasterId(),
                savedTaxRate.getTaxCode(),
                savedTaxRate.getRatePercentage(),
                savedTaxRate.getEffectiveFrom(),
                savedTaxRate.getEffectiveTo());
    }

    private FinancialYear resolveCurrentFinancialYear() {
        LocalDate currentDateInIndia = LocalDate.now(INDIA_ZONE_ID);
        int currentYear = currentDateInIndia.getYear();

        LocalDate financialYearStartDate;
        LocalDate financialYearEndDate;

        if (currentDateInIndia.getMonthValue() >= Month.APRIL.getValue()) {
            financialYearStartDate = LocalDate.of(currentYear, Month.APRIL, 1);
            financialYearEndDate = LocalDate.of(currentYear + 1, Month.MARCH, 31);
        } else {
            financialYearStartDate = LocalDate.of(currentYear - 1, Month.APRIL, 1);
            financialYearEndDate = LocalDate.of(currentYear, Month.MARCH, 31);
        }

        return new FinancialYear(financialYearStartDate, financialYearEndDate);
    }

    private record FinancialYear(LocalDate startDate, LocalDate endDate) {
    }

    private record TaxRateSeed(String taxCode, String taxName, BigDecimal ratePercentage) {
    }
}
