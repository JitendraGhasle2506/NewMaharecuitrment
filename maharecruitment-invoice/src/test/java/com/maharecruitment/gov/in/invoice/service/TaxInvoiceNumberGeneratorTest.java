package com.maharecruitment.gov.in.invoice.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import com.maharecruitment.gov.in.invoice.entity.DepartmentTaxInvoiceSequenceEntity;
import com.maharecruitment.gov.in.invoice.exception.TaxInvoiceException;
import com.maharecruitment.gov.in.invoice.repository.DepartmentTaxInvoiceSequenceRepository;

class TaxInvoiceNumberGeneratorTest {
    private final DepartmentTaxInvoiceSequenceRepository repository = mock(DepartmentTaxInvoiceSequenceRepository.class);
    private final TaxInvoiceNumberGenerator generator = new TaxInvoiceNumberGenerator(repository);

    @Test
    void initializesMissingFinancialYearThenAllocatesFromLockedRow() {
        DepartmentTaxInvoiceSequenceEntity row = new DepartmentTaxInvoiceSequenceEntity(1L, "2026-27", 0);
        when(repository.findForUpdate("2026-27")).thenReturn(Optional.empty(), Optional.of(row));
        assertThat(generator.generate(LocalDate.of(2026, 9, 22))).isEqualTo("TI-2026-27-00001");
        verify(repository).initializeIfAbsent("2026-27");
        verify(repository).save(row);
    }

    @Test
    void usesExistingSequenceAndPreviousFinancialYearBeforeApril() {
        DepartmentTaxInvoiceSequenceEntity row = new DepartmentTaxInvoiceSequenceEntity(1L, "2025-26", 9);
        when(repository.findForUpdate("2025-26")).thenReturn(Optional.of(row));
        assertThat(generator.generate(LocalDate.of(2026, 3, 31))).isEqualTo("TI-2025-26-00010");
        verify(repository, never()).initializeIfAbsent(any());
    }

    @Test
    void exhaustedSequenceDoesNotWriteAnotherNumber() {
        when(repository.findForUpdate("2026-27")).thenReturn(Optional.of(
                new DepartmentTaxInvoiceSequenceEntity(1L, "2026-27", 99_999)));
        assertThatThrownBy(() -> generator.generate(LocalDate.of(2026, 4, 1))).isInstanceOf(TaxInvoiceException.class);
        verify(repository, never()).save(any());
    }
}
