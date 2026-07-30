package com.maharecruitment.gov.in.invoice.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.maharecruitment.gov.in.common.event.invoice.DepartmentTaxInvoiceGenerationRequestedEvent;
import com.maharecruitment.gov.in.common.event.invoice.DepartmentTaxInvoiceInvalidationRequestedEvent;
import com.maharecruitment.gov.in.invoice.service.DepartmentTaxInvoiceService;

@Component
public class DepartmentTaxInvoiceGenerationListener {

    private static final Logger log = LoggerFactory.getLogger(DepartmentTaxInvoiceGenerationListener.class);

    private final DepartmentTaxInvoiceService taxInvoiceService;

    public DepartmentTaxInvoiceGenerationListener(DepartmentTaxInvoiceService taxInvoiceService) {
        this.taxInvoiceService = taxInvoiceService;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onTaxInvoiceGenerationRequested(DepartmentTaxInvoiceGenerationRequestedEvent event) {
        if (event == null || event.departmentProjectApplicationId() == null) {
            return;
        }

        try {
            log.info("Tax invoice generation requested. applicationId={}, requestId={}, actor={}",
                    event.departmentProjectApplicationId(),
                    event.requestId(),
                    event.actorEmail());

            taxInvoiceService.generateForApplication(
                    event.departmentProjectApplicationId(),
                    event.actorEmail());
        } catch (RuntimeException ex) {
            log.error("Tax invoice generation failed. applicationId={}, requestId={}, actor={}",
                    event.departmentProjectApplicationId(),
                    event.requestId(),
                    event.actorEmail(),
                    ex);
            throw ex;
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaxInvoiceInvalidationRequested(DepartmentTaxInvoiceInvalidationRequestedEvent event) {
        if (event == null || event.departmentProjectApplicationId() == null) {
            return;
        }

        try {
            log.info("Tax invoice invalidation requested. applicationId={}, requestId={}, actor={}",
                    event.departmentProjectApplicationId(),
                    event.requestId(),
                    event.actorEmail());

            taxInvoiceService.invalidateForApplication(
                    event.departmentProjectApplicationId(),
                    event.actorEmail());
        } catch (RuntimeException ex) {
            log.error("Tax invoice invalidation failed. applicationId={}, requestId={}, actor={}",
                    event.departmentProjectApplicationId(),
                    event.requestId(),
                    event.actorEmail(),
                    ex);
        }
    }
}
