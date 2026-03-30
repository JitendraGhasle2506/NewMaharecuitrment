package com.maharecruitment.gov.in.invoice.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.maharecruitment.gov.in.invoice.dto.TaxInvoiceView;
import com.maharecruitment.gov.in.invoice.service.DepartmentTaxInvoiceService;
import com.maharecruitment.gov.in.invoice.service.TaxInvoiceQrCodeGenerator;

@Controller
@RequestMapping("/invoice/tax-invoices")
public class TaxInvoiceController {

    private static final Logger log = LoggerFactory.getLogger(TaxInvoiceController.class);

    private final DepartmentTaxInvoiceService taxInvoiceService;
    private final TaxInvoiceQrCodeGenerator qrCodeGenerator;

    public TaxInvoiceController(DepartmentTaxInvoiceService taxInvoiceService,
            TaxInvoiceQrCodeGenerator qrCodeGenerator) {
        this.taxInvoiceService = taxInvoiceService;
        this.qrCodeGenerator = qrCodeGenerator;
    }

    @GetMapping("/{requestId}")
    public String viewByRequestId(@PathVariable String requestId, Model model) {
        model.addAttribute("invoice", taxInvoiceService.getInvoiceByRequestId(requestId));
        return "invoice/tax-invoice-clean";
    }

    @GetMapping("/application/{applicationId}")
    public String viewByApplicationId(@PathVariable Long applicationId, Model model) {
        model.addAttribute("invoice", taxInvoiceService.getInvoiceByApplicationId(applicationId));
        return "invoice/tax-invoice-clean";
    }

    @GetMapping("/application/{applicationId}/preview")
    public String previewByApplicationId(@PathVariable Long applicationId, Model model) {
        model.addAttribute("applicationId", applicationId);
        return "invoice/tax-invoice-preview-combined";
    }

    @GetMapping("/application/{applicationId}/preview/old")
    public String previewOldByApplicationId(
            @PathVariable Long applicationId,
            @RequestParam(name = "embedded", defaultValue = "false") boolean embedded,
            Model model) {
        model.addAttribute("invoice", taxInvoiceService.previewInvoiceByApplicationId(applicationId));
        model.addAttribute("invoicePreviewMode", true);
        model.addAttribute("embeddedPreviewMode", embedded);
        return "invoice/tax-invoice-clean";
    }

    @GetMapping("/application/{applicationId}/preview/new")
    public String previewNewByApplicationId(
            @PathVariable Long applicationId,
            @RequestParam(name = "embedded", defaultValue = "false") boolean embedded,
            Model model) {
        model.addAttribute("invoice", taxInvoiceService.previewInvoiceByApplicationId(applicationId));
        model.addAttribute("invoicePreviewMode", true);
        model.addAttribute("embeddedPreviewMode", embedded);
        return "invoice/tax-invoice-preview";
    }

    @GetMapping(value = "/{requestId}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> qrByRequestId(@PathVariable String requestId) {
        TaxInvoiceView invoice = taxInvoiceService.getInvoiceByRequestId(requestId);
        byte[] pngBytes = qrCodeGenerator.generatePngBytes(invoice);
        if (pngBytes == null || pngBytes.length == 0) {
            log.warn("Tax invoice QR generation returned empty payload for requestId={}", requestId);
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(pngBytes);
    }

    @GetMapping(value = "/application/{applicationId}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> qrByApplicationId(@PathVariable Long applicationId) {
        TaxInvoiceView invoice = taxInvoiceService.getInvoiceByApplicationId(applicationId);
        byte[] pngBytes = qrCodeGenerator.generatePngBytes(invoice);
        if (pngBytes == null || pngBytes.length == 0) {
            log.warn("Tax invoice QR generation returned empty payload for applicationId={}", applicationId);
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(pngBytes);
    }
}
