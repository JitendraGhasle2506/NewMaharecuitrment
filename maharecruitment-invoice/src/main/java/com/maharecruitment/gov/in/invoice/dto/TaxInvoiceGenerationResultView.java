package com.maharecruitment.gov.in.invoice.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TaxInvoiceGenerationResultView {

    private final int generatedCount;
    private final int alreadyGeneratedCount;
    private final int failedCount;
    private final List<TaxInvoiceGenerationApplicationView> applications;

    public int getTotalProcessedCount() {
        return generatedCount + alreadyGeneratedCount + failedCount;
    }
}
