package com.maharecruitment.gov.in.department.controller;

import java.security.Principal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ResponseBody;

import com.maharecruitment.gov.in.department.dto.AdvancePaymentForm;
import com.maharecruitment.gov.in.department.dto.DepartmentProjectApplicationSummaryView;
import com.maharecruitment.gov.in.department.entity.DepartmentAdvancePaymentEntity;
import com.maharecruitment.gov.in.department.service.DepartmentAdvancePaymentService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/department/payment")
public class DepartmentPaymentController {

    private static final Logger log = LoggerFactory.getLogger(DepartmentPaymentController.class);

    private final DepartmentAdvancePaymentService paymentService;

    public DepartmentPaymentController(DepartmentAdvancePaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/apply/{applicationId}")
    public String create(@PathVariable Long applicationId, Model model, Principal principal) {
        String actorEmail = principal.getName();
        AdvancePaymentForm form = paymentService.initializePaymentForm(applicationId, actorEmail);
        populateModel(model, form, applicationId);
        return "department/advance-payment";
    }

    @GetMapping("/{paymentId}/edit")
    public String edit(@PathVariable Long paymentId, Model model, Principal principal) {
        String actorEmail = principal.getName();
        AdvancePaymentForm form = paymentService.getPaymentForEdit(paymentId, actorEmail);
        populateModel(model, form, form.getDepartmentProjectApplicationId());
        return "department/advance-payment";
    }

    @PostMapping("/save")
    public String save(
            @Valid @ModelAttribute("paymentForm") AdvancePaymentForm form,
            BindingResult bindingResult,
            @RequestParam(defaultValue = "save") String actionStatus,
            Model model,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        String actorEmail = principal.getName();

        if (bindingResult.hasErrors()) {
            populateModel(model, form, form.getDepartmentProjectApplicationId());
            return "department/advance-payment";
        }

        if (paymentService.isReceiptNumberDuplicate(form.getReceiptNumber(), form.getId())) {
            bindingResult.rejectValue("receiptNumber", "duplicate", "Receipt number already exists.");
            populateModel(model, form, form.getDepartmentProjectApplicationId());
            return "department/advance-payment";
        }

        try {
            paymentService.savePayment(form, actionStatus, actorEmail);
            redirectAttributes.addFlashAttribute("successMessage", "Payment record processed successfully.");
            return "redirect:/department/payment/list";
        } catch (Exception ex) {
            log.error("Error saving advance payment", ex);
            model.addAttribute("errorMessage", ex.getMessage());
            populateModel(model, form, form.getDepartmentProjectApplicationId());
            return "department/advance-payment";
        }
    }

    @GetMapping("/{paymentId}/view")
    public String view(@PathVariable Long paymentId, Model model, Principal principal) {
        String actorEmail = principal.getName();
        // Use getPaymentForReview to allow viewing even if not in DRAFT
        AdvancePaymentForm payment = paymentService.getPaymentForReview(paymentId, actorEmail);
        populateModel(model, payment, payment.getDepartmentProjectApplicationId());
        return "department/advance-payment-review";
    }

    @GetMapping("/list")
    public String list(Model model, Principal principal) {
        String actorEmail = principal.getName();
        List<DepartmentAdvancePaymentEntity> payments = paymentService.getPaymentSummaries(actorEmail);
        List<DepartmentProjectApplicationSummaryView> eligibleProjects = paymentService
                .getEligibleProjectsForAdvancePayment(actorEmail);

        model.addAttribute("payments", payments);
        model.addAttribute("eligibleProjects", eligibleProjects);
        return "department/advance-payment-list";
    }

    @GetMapping("/{paymentId}/receipt")
    @ResponseBody
    public ResponseEntity<Resource> viewReceipt(@PathVariable Long paymentId, Principal principal) {
        String actorEmail = principal.getName();
        try {
            AdvancePaymentForm form = paymentService.getPaymentForReview(paymentId, actorEmail);
            Resource resource = paymentService.getReceiptResource(paymentId, actorEmail);

            String contentType = form.getReceiptFileType();
            if (contentType == null || contentType.isBlank()) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + form.getReceiptOriginalName() + "\"")
                    .body(resource);
        } catch (Exception ex) {
            log.error("Error serving receipt file", ex);
            return ResponseEntity.notFound().build();
        }
    }

    private void populateModel(Model model, AdvancePaymentForm form, Long applicationId) {
        DepartmentProjectApplicationSummaryView projectApp = paymentService.getProjectApplicationSummary(applicationId);
        model.addAttribute("paymentForm", form);
        model.addAttribute("payment", form); // For consistency with review page
        model.addAttribute("projectApp", projectApp);
        model.addAttribute("invoices", paymentService.getAvailableInvoices(applicationId));
    }
}
