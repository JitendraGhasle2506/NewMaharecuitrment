package com.maharecruitment.gov.in.department.controller;

import java.security.Principal;
import java.util.Locale;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import com.maharecruitment.gov.in.department.dto.AdvancePaymentForm;
import com.maharecruitment.gov.in.department.dto.DepartmentProjectApplicationSummaryView;
import com.maharecruitment.gov.in.department.dto.DepartmentReviewActionRequest;
import com.maharecruitment.gov.in.department.entity.AuditorReviewDecision;
import com.maharecruitment.gov.in.department.entity.HrReviewDecision;
import com.maharecruitment.gov.in.department.exception.DepartmentApplicationException;
import com.maharecruitment.gov.in.department.service.DepartmentAdvancePaymentService;
import com.maharecruitment.gov.in.master.dto.ApiResponse;

import jakarta.validation.Valid;

@Controller
public class DepartmentPaymentReviewController {

    private final DepartmentAdvancePaymentService paymentService;

    public DepartmentPaymentReviewController(DepartmentAdvancePaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping({ "/hr/department/payment/list", "/auditor/department/payment/list" })
    public String listPayments(Model model, Principal principal) {
        String actorEmail = resolveActorEmail(principal);
        var payments = paymentService.getReviewList(actorEmail);
        System.out.println("DEBUG: Review list fetched for " + actorEmail + ", count: " + payments.size());
        model.addAttribute("payments", payments);
        return "hr/department-payment-list";
    }

    @GetMapping({ "/hr/department/payment/{paymentId}", "/auditor/department/payment/{paymentId}" })
    public String showReviewPage(@PathVariable Long paymentId, Model model, Principal principal) {
        String actorEmail = resolveActorEmail(principal);
        // We use getPaymentForReview which doesn't have the strict department check
        AdvancePaymentForm payment = paymentService.getPaymentForReview(paymentId, actorEmail);
        DepartmentProjectApplicationSummaryView projectApp = paymentService
                .getProjectApplicationSummary(payment.getDepartmentProjectApplicationId());

        model.addAttribute("payment", payment);
        model.addAttribute("projectApp", projectApp);
        model.addAttribute("hrDecisions", HrReviewDecision.values());
        model.addAttribute("auditorDecisions", AuditorReviewDecision.values());

        return "department/advance-payment-review";
    }

    @PostMapping("/hr/department/payment/{paymentId}/review")
    @ResponseBody
    public ResponseEntity<ApiResponse<String>> reviewByHr(
            @PathVariable Long paymentId,
            @Valid @RequestBody DepartmentReviewActionRequest request,
            Principal principal) {
        String actorEmail = resolveActorEmail(principal);
        HrReviewDecision decision = parseHrDecision(request.getDecision());
        paymentService.reviewByHr(paymentId, decision, request.getRemarks(), actorEmail);
        return ResponseEntity.ok(ApiResponse.of("HR review decision for payment applied successfully.", "SUCCESS"));
    }

    @PostMapping("/auditor/department/payment/{paymentId}/review")
    @ResponseBody
    public ResponseEntity<ApiResponse<String>> reviewByAuditor(
            @PathVariable Long paymentId,
            @Valid @RequestBody DepartmentReviewActionRequest request,
            Principal principal) {
        String actorEmail = resolveActorEmail(principal);
        AuditorReviewDecision decision = parseAuditorDecision(request.getDecision());
        paymentService.reviewByAuditor(paymentId, decision, request.getRemarks(), actorEmail);
        return ResponseEntity
                .ok(ApiResponse.of("Auditor review decision for payment applied successfully.", "SUCCESS"));
    }

    private String resolveActorEmail(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new DepartmentApplicationException("Authenticated user is required.");
        }
        return principal.getName();
    }

    private HrReviewDecision parseHrDecision(String value) {
        try {
            return HrReviewDecision.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException ex) {
            throw new DepartmentApplicationException("Invalid HR decision. Allowed: APPROVE, REJECT, SEND_BACK.");
        }
    }

    private AuditorReviewDecision parseAuditorDecision(String value) {
        try {
            return AuditorReviewDecision.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException ex) {
            throw new DepartmentApplicationException("Invalid Auditor decision. Allowed: APPROVE, SEND_BACK.");
        }
    }
}
