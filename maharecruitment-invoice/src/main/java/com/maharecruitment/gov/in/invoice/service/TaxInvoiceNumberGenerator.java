package com.maharecruitment.gov.in.invoice.service;

import java.time.LocalDate;
import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.invoice.entity.DepartmentTaxInvoiceSequenceEntity;
import com.maharecruitment.gov.in.invoice.exception.TaxInvoiceException;
import com.maharecruitment.gov.in.invoice.repository.DepartmentTaxInvoiceSequenceRepository;

@Component
public class TaxInvoiceNumberGenerator {

    private static final int MAX_SEQUENCE_PER_FINANCIAL_YEAR = 99_999;

    private final DepartmentTaxInvoiceSequenceRepository sequenceRepository;

    public TaxInvoiceNumberGenerator(DepartmentTaxInvoiceSequenceRepository sequenceRepository) {
        this.sequenceRepository = sequenceRepository;
    }

    @Transactional
    public String generate(LocalDate issueDate) {
        LocalDate effectiveDate = issueDate != null ? issueDate : LocalDate.now();
        String financialYearCode = resolveFinancialYearCode(effectiveDate);

        DepartmentTaxInvoiceSequenceEntity sequenceEntity = sequenceRepository
                .findForUpdate(financialYearCode)
                .orElseGet(() -> createSequenceRow(financialYearCode));

        int nextSequence = sequenceEntity.getLastSequence() + 1;
        if (nextSequence > MAX_SEQUENCE_PER_FINANCIAL_YEAR) {
            throw new TaxInvoiceException(
                    "Tax invoice sequence limit reached for financial year " + financialYearCode + ".");
        }

        sequenceEntity.setLastSequence(nextSequence);
        sequenceRepository.save(sequenceEntity);

        return String.format(Locale.ROOT, "TI-%s-%05d", financialYearCode, nextSequence);
    }

    private DepartmentTaxInvoiceSequenceEntity createSequenceRow(String financialYearCode) {
        DepartmentTaxInvoiceSequenceEntity sequenceEntity = new DepartmentTaxInvoiceSequenceEntity();
        sequenceEntity.setFinancialYearCode(financialYearCode);
        sequenceEntity.setLastSequence(0);

        try {
            return sequenceRepository.saveAndFlush(sequenceEntity);
        } catch (DataIntegrityViolationException ex) {
            return sequenceRepository.findForUpdate(financialYearCode)
                    .orElseThrow(() -> new TaxInvoiceException(
                            "Unable to initialize tax invoice sequence for financial year "
                                    + financialYearCode + ".",
                            ex));
        }
    }

    private String resolveFinancialYearCode(LocalDate issueDate) {
        int year = issueDate.getYear();
        int startYear = issueDate.getMonthValue() >= 4 ? year : year - 1;
        int endYear = startYear + 1;
        return String.format(Locale.ROOT, "%d-%02d", startYear, endYear % 100);
    }
}
