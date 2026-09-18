package com.maharecruitment.gov.in.invoice.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TaxInvoiceGenerationPreviewView {

    private final List<TaxInvoiceEmployeePreviewView> employees;
    private final List<TaxInvoiceGenerationApplicationView> applications;

    public int getEmployeeCount() {
        return employees == null ? 0 : employees.size();
    }

    public int getApplicationCount() {
        return applications == null ? 0 : applications.size();
    }

    public long getAlreadyGeneratedCount() {
        if (applications == null) {
            return 0;
        }
        return applications.stream()
                .filter(TaxInvoiceGenerationApplicationView::isGenerated)
                .count();
    }

    public long getPendingGenerationCount() {
        if (applications == null) {
            return 0;
        }
        return applications.stream()
                .filter(application -> !application.isGenerated())
                .count();
    }
}
