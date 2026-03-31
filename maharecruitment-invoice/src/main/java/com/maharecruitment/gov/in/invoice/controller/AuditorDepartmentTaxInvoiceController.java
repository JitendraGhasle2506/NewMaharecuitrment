package com.maharecruitment.gov.in.invoice.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.maharecruitment.gov.in.invoice.dto.TaxInvoiceListItemView;
import com.maharecruitment.gov.in.invoice.service.DepartmentTaxInvoiceService;

@Controller
@RequestMapping("/auditor/department-tax-invoices")
public class AuditorDepartmentTaxInvoiceController {

    private final DepartmentTaxInvoiceService taxInvoiceService;

    public AuditorDepartmentTaxInvoiceController(DepartmentTaxInvoiceService taxInvoiceService) {
        this.taxInvoiceService = taxInvoiceService;
    }

    @GetMapping
    public String listGeneratedInvoices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(
                        Sort.Order.desc("tiDate"),
                        Sort.Order.desc("departmentTaxInvoiceId")));

        Page<TaxInvoiceListItemView> taxInvoices = taxInvoiceService.getGeneratedInvoices(pageable);
        model.addAttribute("taxInvoices", taxInvoices);
        return "invoice/auditor-department-tax-invoice-list";
    }
}
