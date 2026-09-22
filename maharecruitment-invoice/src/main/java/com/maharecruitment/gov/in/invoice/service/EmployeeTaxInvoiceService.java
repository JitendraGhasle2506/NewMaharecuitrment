package com.maharecruitment.gov.in.invoice.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maharecruitment.gov.in.invoice.dto.EmployeeTaxInvoiceListItem;
import com.maharecruitment.gov.in.invoice.dto.TaxInvoiceBillingDetails;
import com.maharecruitment.gov.in.invoice.dto.TaxInvoiceGenerationFilter;
import com.maharecruitment.gov.in.invoice.dto.TaxInvoiceView;
import com.maharecruitment.gov.in.invoice.exception.TaxInvoiceException;
import com.maharecruitment.gov.in.invoice.exception.TaxInvoiceNotFoundException;
import com.maharecruitment.gov.in.invoice.repository.EmployeeTaxInvoiceRepository;

@Service
@Transactional(readOnly = true)
public class EmployeeTaxInvoiceService {
    private final EmployeeTaxInvoiceRepository repository;
    private final TaxInvoiceNumberGenerator numberGenerator;
    private final ObjectMapper objectMapper;

    public EmployeeTaxInvoiceService(EmployeeTaxInvoiceRepository repository,
            TaxInvoiceNumberGenerator numberGenerator, ObjectMapper objectMapper) {
        this.repository = repository;
        this.numberGenerator = numberGenerator;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public long generate(String token, TaxInvoiceGenerationFilter selection, TaxInvoiceView preview,
            TaxInvoiceBillingDetails details, String actor) {
        Long existingId = repository.findIdByGenerationToken(token).orElse(null);
        if (existingId != null) {
            return existingId;
        }
        if (preview == null || preview.getLineItems() == null || preview.getLineItems().isEmpty()
                || preview.getTotalAmount() == null || preview.getTotalAmount().signum() <= 0) {
            throw new TaxInvoiceException("Preview an invoice with billable employees before generating it.");
        }
        // Copy the trusted preview so a failed save or concurrent request cannot change the session draft.
        TaxInvoiceView invoice = objectMapper.convertValue(preview, TaxInvoiceView.class);
        details.applyTo(invoice);
        invoice.setDocumentTitle("TAX INVOICE");
        invoice.setTiDate(LocalDate.now());
        invoice.setTiNumber(numberGenerator.generate(invoice.getTiDate()));
        invoice.setGeneratedOn(LocalDateTime.now());
        invoice.setGeneratedByLoginId(actor == null || actor.isBlank() ? "SYSTEM" : actor);
        invoice.setQrCodeDataUrl(null); // Regenerated from the saved document when it is opened.
        try {
            return repository.save(token, selection, invoice, objectMapper.writeValueAsString(invoice));
        } catch (JsonProcessingException ex) {
            throw new TaxInvoiceException("Unable to save the invoice document. Please try again.", ex);
        }
    }

    public TaxInvoiceView getInvoice(long id) {
        String snapshot = repository.findSnapshotById(id)
                .orElseThrow(() -> new TaxInvoiceNotFoundException("Generated tax invoice was not found."));
        try {
            return objectMapper.readValue(snapshot, TaxInvoiceView.class);
        } catch (JsonProcessingException ex) {
            throw new TaxInvoiceException("Unable to read the saved invoice document.", ex);
        }
    }

    public Page<EmployeeTaxInvoiceListItem> list(String search, Long departmentId, Integer month, Integer year,
            int page) {
        return repository.findAll(search == null ? "" : search.trim(), departmentId, month, year,
                PageRequest.of(Math.max(0, page), 20));
    }
}
