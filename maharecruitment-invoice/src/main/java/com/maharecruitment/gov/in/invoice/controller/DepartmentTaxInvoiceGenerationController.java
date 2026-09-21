package com.maharecruitment.gov.in.invoice.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.maharecruitment.gov.in.invoice.dto.TaxInvoiceBillingDetails;
import com.maharecruitment.gov.in.invoice.dto.TaxInvoiceGenerationFilter;
import com.maharecruitment.gov.in.invoice.dto.TaxInvoiceView;
import com.maharecruitment.gov.in.invoice.service.DepartmentTaxInvoiceGenerationService;
import com.maharecruitment.gov.in.invoice.service.TaxInvoiceQrCodeGenerator;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/invoice/tax-invoices/generate")
public class DepartmentTaxInvoiceGenerationController {

    private static final Logger log = LoggerFactory.getLogger(DepartmentTaxInvoiceGenerationController.class);
    private static final String VIEW_NAME = "invoice/tax-invoice-generation";
    private static final String INVOICE_VIEW_NAME = "invoice/tax-invoice-preview";

    private static final String DRAFT_SESSION_KEY = DepartmentTaxInvoiceGenerationController.class.getName() + ".draft";

    private final DepartmentTaxInvoiceGenerationService generationService;
    private final TaxInvoiceQrCodeGenerator qrCodeGenerator;

    public DepartmentTaxInvoiceGenerationController(DepartmentTaxInvoiceGenerationService generationService,
            TaxInvoiceQrCodeGenerator qrCodeGenerator) {
        this.generationService = generationService;
        this.qrCodeGenerator = qrCodeGenerator;
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

    @PostMapping("/load")
    public String load(
            @ModelAttribute("invoiceFilter") TaxInvoiceGenerationFilter filter,
            BindingResult bindingResult, HttpSession session,
            Model model) {
        session.removeAttribute(DRAFT_SESSION_KEY);
        try {
            if (bindingResult.hasErrors()) {
                throw new IllegalArgumentException("Enter a valid department, project and billing dates.");
            }
            model.addAttribute("employees", generationService.loadProjectEmployees(filter));
            model.addAttribute("employeesLoaded", true);
            Draft draft = new Draft(UUID.randomUUID().toString(), Selection.from(filter), null);
            session.setAttribute(DRAFT_SESSION_KEY, draft);
            model.addAttribute("loadToken", draft.token());
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
        model.addAttribute(
                "errorMessage",
                "Direct generation is disabled. Preview the applications and open an invoice to generate it in the existing format.");
        try {
            model.addAttribute("preview", generationService.preview(filter));
            model.addAttribute("previewReady", true);
        } catch (RuntimeException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
        }
        populateForm(model, filter);
        return VIEW_NAME;
    }

    @PostMapping("/employee-preview")
    public String employeePreview(
            @ModelAttribute("invoiceFilter") TaxInvoiceGenerationFilter filter,
            BindingResult bindingResult,
            @RequestParam(required = false) String loadToken, HttpSession session,
            Model model) {
        try {
            Draft draft = (Draft) session.getAttribute(DRAFT_SESSION_KEY);
            if (bindingResult.hasErrors() || draft == null || !Objects.equals(loadToken, draft.token())
                    || !draft.selection().equals(Selection.from(filter))) {
                throw new IllegalArgumentException("The selection has changed or expired. Load employees again before Preview.");
            }
            TaxInvoiceView invoice = generationService.buildEmployeeInvoice(filter);
            draft = new Draft(draft.token(), draft.selection(), invoice);
            session.setAttribute(DRAFT_SESSION_KEY, draft);
            model.addAttribute("billingDetails", TaxInvoiceBillingDetails.from(invoice));
            return renderEmployeePreview(draft, model);
        } catch (RuntimeException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
        }
        populateForm(model, filter);
        return VIEW_NAME;
    }

    @PostMapping("/invoice")
    public String employeeInvoice(
            @Valid @ModelAttribute("billingDetails") TaxInvoiceBillingDetails details,
            BindingResult bindingResult,
            @RequestParam(required = false) String loadToken, HttpSession session, Model model) {
        Draft draft = (Draft) session.getAttribute(DRAFT_SESSION_KEY);
        if (draft == null || draft.invoice() == null || !Objects.equals(loadToken, draft.token())) {
            model.addAttribute("errorMessage", "Load employees and preview the invoice before generating it.");
            populateForm(model, new TaxInvoiceGenerationFilter());
            return VIEW_NAME;
        }
        if (bindingResult.hasErrors()) {
            return renderEmployeePreview(draft, model);
        }
        // Use the server-side preview snapshot: posted totals, rows and selection cannot override it.
        TaxInvoiceView invoice = draft.invoice();
        details.applyTo(invoice);
        invoice.setQrCodeDataUrl(qrCodeGenerator.generateDataUrl(invoice));
        model.addAttribute("invoice", invoice);
        session.removeAttribute(DRAFT_SESSION_KEY);
        return INVOICE_VIEW_NAME;
    }

    private String renderEmployeePreview(Draft draft, Model model) {
        model.addAttribute("invoice", draft.invoice());
        model.addAttribute("loadToken", draft.token());
        model.addAttribute("employeeBillingPreview", true);
        return INVOICE_VIEW_NAME;
    }

    private record Selection(Long departmentId, Long subDepartmentId, Long projectId,
            LocalDate startDate, LocalDate endDate) {
        static Selection from(TaxInvoiceGenerationFilter filter) {
            return new Selection(filter.getDepartmentId(), filter.getSubDepartmentId(), filter.getProjectId(),
                    filter.getStartDate(), filter.getEndDate());
        }
    }

    private record Draft(String token, Selection selection, TaxInvoiceView invoice) { }

    @GetMapping(value = "/options/sub-departments", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> subDepartmentOptions(
            @RequestParam(value = "departmentId", required = false) Long departmentId) {
        if (departmentId == null) {
            return ResponseEntity.ok(List.of());
        }
        try {
            return ResponseEntity.ok(generationService.getSubDepartmentOptions(departmentId));
        } catch (RuntimeException ex) {
            log.error("Failed to load sub-department options for departmentId={}", departmentId, ex);
            return optionLoadFailure("Unable to load subdepartments.");
        }
    }

    @GetMapping(value = "/options/projects", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> projectOptions(
            @RequestParam(value = "departmentId", required = false) Long departmentId,
            @RequestParam(value = "subDepartmentId", required = false) Long subDepartmentId) {
        if (departmentId == null) {
            return ResponseEntity.ok(List.of());
        }
        try {
            return ResponseEntity.ok(generationService.getProjectOptions(departmentId, subDepartmentId));
        } catch (RuntimeException ex) {
            log.error("Failed to load project options for departmentId={} subDepartmentId={}",
                    departmentId, subDepartmentId, ex);
            return optionLoadFailure("Unable to load projects.");
        }
    }

    private ResponseEntity<Map<String, String>> optionLoadFailure(String message) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("message", message));
    }

    private void populateForm(Model model, TaxInvoiceGenerationFilter filter) {
        TaxInvoiceGenerationFilter resolvedFilter = filter == null ? new TaxInvoiceGenerationFilter() : filter;
        Long departmentId = resolvedFilter.getDepartmentId();
        model.addAttribute("invoiceFilter", resolvedFilter);
        model.addAttribute("departments", generationService.getDepartmentOptions());
        // Dependent dropdowns start scoped to the current selection; the page reloads them via the options endpoints.
        model.addAttribute("subDepartments", departmentId == null
                ? List.of()
                : generationService.getSubDepartmentOptions(departmentId));
        model.addAttribute("projects", departmentId == null
                ? List.of()
                : generationService.getProjectOptions(departmentId, resolvedFilter.getSubDepartmentId()));
    }
}
