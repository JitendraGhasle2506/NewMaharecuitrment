package com.maharecruitment.gov.in.department.controller;

import java.security.Principal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.maharecruitment.gov.in.department.dto.AuditorDepartmentReviewForm;
import com.maharecruitment.gov.in.department.entity.AuditorReviewDecision;
import com.maharecruitment.gov.in.department.exception.DepartmentApplicationException;
import com.maharecruitment.gov.in.department.service.AuditorDepartmentRequestService;
import com.maharecruitment.gov.in.department.service.DepartmentProfileDocumentStorageService;
import com.maharecruitment.gov.in.department.service.DepartmentWorkOrderStorageService;
import com.maharecruitment.gov.in.department.service.model.AuditorDepartmentApplicationReviewDetailView;
import com.maharecruitment.gov.in.department.service.model.DepartmentProfileDocumentType;
import com.maharecruitment.gov.in.department.service.model.WorkOrderDocumentView;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/auditor/department-requests")
public class AuditorDepartmentRequestController {

    private static final Logger log = LoggerFactory.getLogger(AuditorDepartmentRequestController.class);

    private final AuditorDepartmentRequestService auditorDepartmentRequestService;
    private final DepartmentWorkOrderStorageService workOrderStorageService;
    private final DepartmentProfileDocumentStorageService profileDocumentStorageService;

    public AuditorDepartmentRequestController(
            AuditorDepartmentRequestService auditorDepartmentRequestService,
            DepartmentWorkOrderStorageService workOrderStorageService,
            DepartmentProfileDocumentStorageService profileDocumentStorageService) {
        this.auditorDepartmentRequestService = auditorDepartmentRequestService;
        this.workOrderStorageService = workOrderStorageService;
        this.profileDocumentStorageService = profileDocumentStorageService;
    }

    @GetMapping
    public String departmentRequestSummary(Model model) {
        try {
            model.addAttribute("parentDepartmentRequests", auditorDepartmentRequestService.getParentDepartmentRequests());
        } catch (DepartmentApplicationException ex) {
            log.warn("Unable to load auditor parent department queue. reason={}", ex.getMessage());
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("parentDepartmentRequests", List.of());
        } catch (Exception ex) {
            log.error("Unexpected error while loading auditor parent department queue.", ex);
            model.addAttribute(
                    "errorMessage",
                    "Unable to load parent departments right now. Please try again.");
            model.addAttribute("parentDepartmentRequests", List.of());
        }
        return "auditor/department-request-list";
    }

    @GetMapping("/{departmentId}/subdepartments")
    public String subDepartmentProjectCounts(
            @PathVariable Long departmentId,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute(
                    "subDepartmentRequest",
                    auditorDepartmentRequestService.getSubDepartmentProjectCounts(departmentId));
            return "auditor/department-request-subdepartment-list";
        } catch (DepartmentApplicationException ex) {
            log.warn("Unable to load auditor sub-department queue for departmentId={}, reason={}",
                    departmentId,
                    ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/auditor/department-requests";
        }
    }

    @GetMapping("/{departmentId}/subdepartments/{subDepartmentId}/applications")
    public String queueApplications(
            @PathVariable Long departmentId,
            @PathVariable Long subDepartmentId,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute(
                    "applicationDetail",
                    auditorDepartmentRequestService.getSubDepartmentApplications(departmentId, subDepartmentId));
            return "auditor/department-request-applications";
        } catch (DepartmentApplicationException ex) {
            log.warn("Unable to load auditor queue applications for departmentId={}, subDepartmentId={}, reason={}",
                    departmentId,
                    subDepartmentId,
                    ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/auditor/department-requests/" + departmentId + "/subdepartments";
        }
    }

    @GetMapping("/{departmentId}/subdepartments/{subDepartmentId}/applications/{applicationId}")
    public String applicationDetail(
            @PathVariable Long departmentId,
            @PathVariable Long subDepartmentId,
            @PathVariable Long applicationId,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            AuditorDepartmentApplicationReviewDetailView reviewDetail = auditorDepartmentRequestService
                    .getApplicationReviewDetail(departmentId, subDepartmentId, applicationId);
            populateReviewModel(model, reviewDetail);
            if (!model.containsAttribute("reviewForm")) {
                model.addAttribute("reviewForm", new AuditorDepartmentReviewForm());
            }
            return "auditor/department-request-application-detail";
        } catch (DepartmentApplicationException ex) {
            log.warn("Unable to load auditor application detail. departmentId={}, subDepartmentId={}, applicationId={}, reason={}",
                    departmentId,
                    subDepartmentId,
                    applicationId,
                    ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/auditor/department-requests/" + departmentId + "/subdepartments/" + subDepartmentId
                    + "/applications";
        }
    }

    @PostMapping("/{departmentId}/subdepartments/{subDepartmentId}/applications/{applicationId}/review")
    public String reviewApplicationByAuditor(
            @PathVariable Long departmentId,
            @PathVariable Long subDepartmentId,
            @PathVariable Long applicationId,
            @Valid @ModelAttribute("reviewForm") AuditorDepartmentReviewForm reviewForm,
            BindingResult bindingResult,
            Model model,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        if (isRemarksMandatory(reviewForm.getDecision()) && !StringUtils.hasText(reviewForm.getRemarks())) {
            bindingResult.rejectValue("remarks", "reviewForm.remarks.required",
                    "Remarks are required for send back decision.");
        }

        if (bindingResult.hasErrors()) {
            try {
                AuditorDepartmentApplicationReviewDetailView reviewDetail = auditorDepartmentRequestService
                        .getApplicationReviewDetail(departmentId, subDepartmentId, applicationId);
                populateReviewModel(model, reviewDetail);
                return "auditor/department-request-application-detail";
            } catch (DepartmentApplicationException ex) {
                redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
                return "redirect:/auditor/department-requests/" + departmentId + "/subdepartments/" + subDepartmentId
                        + "/applications";
            }
        }

        try {
            auditorDepartmentRequestService.reviewApplicationByAuditor(
                    departmentId,
                    subDepartmentId,
                    applicationId,
                    reviewForm.getDecision(),
                    reviewForm.getRemarks(),
                    resolveActorEmail(principal));

            redirectAttributes.addFlashAttribute("successMessage", "Auditor decision applied successfully.");
            return "redirect:/auditor/department-requests/" + departmentId + "/subdepartments/" + subDepartmentId
                    + "/applications/" + applicationId;
        } catch (DepartmentApplicationException ex) {
            log.warn("Unable to apply auditor decision. departmentId={}, subDepartmentId={}, applicationId={}, reason={}",
                    departmentId,
                    subDepartmentId,
                    applicationId,
                    ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/auditor/department-requests/" + departmentId + "/subdepartments/" + subDepartmentId
                    + "/applications/" + applicationId;
        }
    }

    @PostMapping("/{departmentId}/subdepartments/{subDepartmentId}/applications/{applicationId}/complete")
    public String completeApplication(
            @PathVariable Long departmentId,
            @PathVariable Long subDepartmentId,
            @PathVariable Long applicationId,
            @RequestParam(name = "completionRemarks", required = false) String completionRemarks,
            @RequestParam(name = "verificationConfirmed", defaultValue = "false") boolean verificationConfirmed,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        if (!verificationConfirmed) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Please review the tax invoice preview and confirm completion.");
            return "redirect:/auditor/department-requests/" + departmentId + "/subdepartments/" + subDepartmentId
                    + "/applications/" + applicationId;
        }

        try {
            auditorDepartmentRequestService.completeApplication(
                    departmentId,
                    subDepartmentId,
                    applicationId,
                    completionRemarks,
                    resolveActorEmail(principal));
            redirectAttributes.addFlashAttribute("successMessage", "Application marked completed successfully.");
        } catch (DepartmentApplicationException ex) {
            log.warn("Unable to complete application. departmentId={}, subDepartmentId={}, applicationId={}, reason={}",
                    departmentId,
                    subDepartmentId,
                    applicationId,
                    ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/auditor/department-requests/" + departmentId + "/subdepartments/" + subDepartmentId
                + "/applications/" + applicationId;
    }

    @GetMapping("/{departmentId}/subdepartments/{subDepartmentId}/applications/{applicationId}/download-work-order")
    public ResponseEntity<Resource> downloadWorkOrder(
            @PathVariable Long departmentId,
            @PathVariable Long subDepartmentId,
            @PathVariable Long applicationId) {
        WorkOrderDocumentView documentView = auditorDepartmentRequestService.getApplicationWorkOrderDocument(
                departmentId,
                subDepartmentId,
                applicationId);
        Resource resource = workOrderStorageService.loadAsResource(documentView.getFullPath());

        String contentType = documentView.getContentType() != null
                ? documentView.getContentType()
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(documentView.getOriginalFileName())
                        .build()
                        .toString())
                .body(resource);
    }

    @GetMapping("/{departmentId}/subdepartments/{subDepartmentId}/applications/{applicationId}/registration-documents/{documentType}")
    public ResponseEntity<Resource> downloadRegistrationDocument(
            @PathVariable Long departmentId,
            @PathVariable Long subDepartmentId,
            @PathVariable Long applicationId,
            @PathVariable String documentType) {
        DepartmentProfileDocumentType profileDocumentType = DepartmentProfileDocumentType.from(documentType);
        WorkOrderDocumentView documentView = auditorDepartmentRequestService.getDepartmentRegistrationDocument(
                departmentId,
                subDepartmentId,
                applicationId,
                profileDocumentType);
        Resource resource = profileDocumentStorageService.loadAsResource(documentView.getFullPath());

        String contentType = documentView.getContentType() != null
                ? documentView.getContentType()
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(documentView.getOriginalFileName())
                        .build()
                        .toString())
                .body(resource);
    }

    private void populateReviewModel(Model model, AuditorDepartmentApplicationReviewDetailView reviewDetail) {
        model.addAttribute("applicationReviewDetail", reviewDetail);
    }

    private String resolveActorEmail(Principal principal) {
        if (principal == null || !StringUtils.hasText(principal.getName())) {
            throw new DepartmentApplicationException("Authenticated user is required.");
        }
        return principal.getName();
    }

    private boolean isRemarksMandatory(AuditorReviewDecision decision) {
        return decision == AuditorReviewDecision.SEND_BACK;
    }
}
