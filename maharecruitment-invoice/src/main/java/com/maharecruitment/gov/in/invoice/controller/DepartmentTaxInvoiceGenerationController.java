package com.maharecruitment.gov.in.invoice.controller;

import java.time.LocalDate;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.invoice.dto.TaxInvoiceGenerationFilter;
import com.maharecruitment.gov.in.invoice.service.DepartmentTaxInvoiceGenerationService;

@Controller
@RequestMapping("/invoice/tax-invoices/generate")
public class DepartmentTaxInvoiceGenerationController {

    private static final String VIEW_NAME = "invoice/tax-invoice-generation";
    private static final String DEFAULT_ACTOR = "SYSTEM";

    private final DepartmentTaxInvoiceGenerationService generationService;

    public DepartmentTaxInvoiceGenerationController(DepartmentTaxInvoiceGenerationService generationService) {
        this.generationService = generationService;
    }

    @GetMapping
    public String form(Model model) {
        TaxInvoiceGenerationFilter filter = new TaxInvoiceGenerationFilter();
        LocalDate today = LocalDate.now();
        filter.setStartDate(today.withDayOfMonth(1));
        filter.setEndDate(today);
        populateForm(model, filter);
        return VIEW_NAME;
    }

    @PostMapping("/preview")
    public String preview(
            @ModelAttribute("invoiceFilter") TaxInvoiceGenerationFilter filter,
            Model model) {
        try {
            model.addAttribute("preview", generationService.preview(filter));
            model.addAttribute("previewReady", true);
        } catch (RuntimeException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
        }
        populateForm(model, filter);
        return VIEW_NAME;
    }

    @PostMapping("/generate")
    public String generate(
            @ModelAttribute("invoiceFilter") TaxInvoiceGenerationFilter filter,
            Model model) {
        try {
            model.addAttribute("generationResult", generationService.generate(filter, resolveActorEmail()));
            model.addAttribute("successMessage", "Tax invoice generation completed.");
        } catch (RuntimeException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
        }
        populateForm(model, filter);
        return VIEW_NAME;
    }

    private void populateForm(Model model, TaxInvoiceGenerationFilter filter) {
        TaxInvoiceGenerationFilter resolvedFilter = filter == null ? new TaxInvoiceGenerationFilter() : filter;
        model.addAttribute("invoiceFilter", resolvedFilter);
        model.addAttribute("departments", generationService.getDepartmentOptions());
        model.addAttribute("subDepartments", generationService.getSubDepartmentOptions(null));
        model.addAttribute("projects", generationService.getProjectOptions(null, null));
    }

    private String resolveActorEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !StringUtils.hasText(authentication.getName())) {
            return DEFAULT_ACTOR;
        }
        return authentication.getName().trim();
    }
}
