package com.maharecruitment.gov.in.invoice.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maharecruitment.gov.in.invoice.dto.EmployeeTaxInvoiceListItem;
import com.maharecruitment.gov.in.invoice.dto.TaxInvoiceBillingDetails;
import com.maharecruitment.gov.in.invoice.dto.TaxInvoiceGenerationFilter;
import com.maharecruitment.gov.in.invoice.dto.TaxInvoiceLineItemView;
import com.maharecruitment.gov.in.invoice.dto.TaxInvoiceView;
import com.maharecruitment.gov.in.invoice.exception.TaxInvoiceException;
import com.maharecruitment.gov.in.invoice.exception.TaxInvoiceNotFoundException;
import com.maharecruitment.gov.in.invoice.repository.EmployeeTaxInvoiceRepository;
import com.maharecruitment.gov.in.invoice.repository.EmployeeTaxInvoiceRepository.InvoicedEmployee;

@Service
@Transactional(readOnly = true)
public class EmployeeTaxInvoiceService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

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
        List<TaxInvoiceLineItemView> lines = preview.getLineItems();
        if (lines.stream().anyMatch(line -> line.getEmployeeId() == null || line.getBilledFrom() == null
                || line.getBilledTo() == null)) {
            throw new TaxInvoiceException("The preview is out of date. Load employees and preview the invoice again.");
        }
        // Lock the employees, then re-check, so two users cannot bill the same employee days at the same time.
        repository.lockEmployeesForInvoicing(lines.stream().map(TaxInvoiceLineItemView::getEmployeeId).toList());
        existingId = repository.findIdByGenerationToken(token).orElse(null);
        if (existingId != null) {
            return existingId;
        }
        rejectAlreadyInvoicedEmployees(lines);
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

    private void rejectAlreadyInvoicedEmployees(List<TaxInvoiceLineItemView> lines) {
        for (TaxInvoiceLineItemView line : lines) {
            List<InvoicedEmployee> invoiced = repository.findInvoicedEmployees(
                    List.of(line.getEmployeeId()), line.getBilledFrom(), line.getBilledTo());
            if (!invoiced.isEmpty()) {
                InvoicedEmployee existing = invoiced.getFirst();
                throw new TaxInvoiceException(line.getEmployeeName() + " is already invoiced in "
                        + existing.tiNumber() + " for " + DATE_FORMAT.format(existing.billedFrom()) + " to "
                        + DATE_FORMAT.format(existing.billedTo())
                        + ". Load employees and preview the invoice again.");
            }
        }
    }

    /** Employees of the given list already billed for any day between from and to, keyed by employee ID. */
    public Map<Long, InvoicedEmployee> findInvoicedEmployees(Collection<Long> employeeIds, LocalDate from,
            LocalDate to) {
        Map<Long, InvoicedEmployee> invoiced = new LinkedHashMap<>();
        repository.findInvoicedEmployees(employeeIds, from, to)
                .forEach(existing -> invoiced.putIfAbsent(existing.employeeId(), existing));
        return invoiced;
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
