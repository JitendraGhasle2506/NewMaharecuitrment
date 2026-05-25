package com.maharecruitment.gov.in.invoice.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.invoice.entity.AgencyMonthlyBillSequenceEntity;
import com.maharecruitment.gov.in.invoice.exception.TaxInvoiceException;
import com.maharecruitment.gov.in.invoice.repository.AgencyMonthlyBillSequenceRepository;

@Component
public class AgencyMonthlyBillNumberGenerator {

    private static final int MAX_SEQUENCE_PER_DAY = 9_999;
    private static final long DATE_SEQUENCE_SCOPE_AGENCY_ID = 0L;
    private static final DateTimeFormatter BILL_DATE_FORMAT = DateTimeFormatter.ofPattern("ddMMMyyyy", Locale.ENGLISH);
    private static final Pattern BILL_NUMBER_PATTERN = Pattern.compile("\\d{2}[A-Z]{3}\\d{4}-\\d{4}");

    private final AgencyMonthlyBillSequenceRepository sequenceRepository;

    public AgencyMonthlyBillNumberGenerator(AgencyMonthlyBillSequenceRepository sequenceRepository) {
        this.sequenceRepository = sequenceRepository;
    }

    @Transactional
    public String generate(Long agencyId, int billYear, int billMonth, LocalDate generatedDate) {
        if (agencyId == null) {
            throw new TaxInvoiceException("Agency id is required for bill number generation.");
        }
        LocalDate issueDate = generatedDate != null ? generatedDate : LocalDate.now();

        AgencyMonthlyBillSequenceEntity sequence = sequenceRepository
                .findForUpdate(issueDate)
                .orElseGet(() -> createSequenceRow(issueDate));

        int nextSequence = sequence.getLastSequence() + 1;
        if (nextSequence > MAX_SEQUENCE_PER_DAY) {
            throw new TaxInvoiceException("Agency bill sequence limit reached for issue date.");
        }
        sequence.setLastSequence(nextSequence);
        sequenceRepository.save(sequence);

        return issueDate.format(BILL_DATE_FORMAT).toUpperCase(Locale.ROOT)
                + "-"
                + String.format(Locale.ROOT, "%04d", nextSequence);
    }

    public boolean isCurrentFormat(String billNumber) {
        return billNumber != null && BILL_NUMBER_PATTERN.matcher(billNumber.trim()).matches();
    }

    private AgencyMonthlyBillSequenceEntity createSequenceRow(LocalDate issueDate) {
        AgencyMonthlyBillSequenceEntity sequence = new AgencyMonthlyBillSequenceEntity();
        sequence.setAgencyId(DATE_SEQUENCE_SCOPE_AGENCY_ID);
        sequence.setBillYear(issueDate.getYear());
        sequence.setBillMonth(issueDate.getDayOfYear());
        sequence.setSequenceDate(issueDate);
        sequence.setLastSequence(0);
        try {
            return sequenceRepository.saveAndFlush(sequence);
        } catch (DataIntegrityViolationException ex) {
            return sequenceRepository.findForUpdate(issueDate)
                    .orElseThrow(() -> new TaxInvoiceException(
                            "Unable to initialize agency bill date sequence.",
                            ex));
        }
    }
}
