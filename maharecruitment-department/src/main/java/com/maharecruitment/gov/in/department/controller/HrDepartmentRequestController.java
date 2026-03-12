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
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.department.dto.HrDepartmentReviewForm;
import com.maharecruitment.gov.in.department.dto.HrAgencyRankMappingForm;
import com.maharecruitment.gov.in.department.dto.HrAgencyRankRowForm;
import com.maharecruitment.gov.in.department.entity.HrReviewDecision;
import com.maharecruitment.gov.in.department.exception.DepartmentApplicationException;
import com.maharecruitment.gov.in.department.service.DepartmentWorkOrderStorageService;
import com.maharecruitment.gov.in.department.service.HrAgencyRankMappingService;
import com.maharecruitment.gov.in.department.service.HrDepartmentRequestService;
import com.maharecruitment.gov.in.department.service.model.HrAgencyRankMappingListView;
import com.maharecruitment.gov.in.department.service.model.HrAgencyRankMappingView;
import com.maharecruitment.gov.in.department.service.model.HrDepartmentApplicationReviewDetailView;
import com.maharecruitment.gov.in.department.service.model.WorkOrderDocumentView;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/hr/department-requests")
public class HrDepartmentRequestController {

    private static final Logger log = LoggerFactory.getLogger(HrDepartmentRequestController.class);

    private final HrDepartmentRequestService hrDepartmentRequestService;
    private final DepartmentWorkOrderStorageService workOrderStorageService;
    private final HrAgencyRankMappingService hrAgencyRankMappingService;

    public HrDepartmentRequestController(
            HrDepartmentRequestService hrDepartmentRequestService,
            DepartmentWorkOrderStorageService workOrderStorageService,
            HrAgencyRankMappingService hrAgencyRankMappingService) {
        this.hrDepartmentRequestService = hrDepartmentRequestService;
        this.workOrderStorageService = workOrderStorageService;
        this.hrAgencyRankMappingService = hrAgencyRankMappingService;
    }

    @GetMapping
    public String departmentRequestSummary(Model model) {
        try {
            model.addAttribute("parentDepartmentRequests", hrDepartmentRequestService.getParentDepartmentRequests());
        } catch (DepartmentApplicationException ex) {
            log.warn("Unable to load HR parent department requests. reason={}", ex.getMessage());
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("parentDepartmentRequests", List.of());
        } catch (Exception ex) {
            log.error("Unexpected error while loading HR parent department requests.", ex);
            model.addAttribute(
                    "errorMessage",
                    "Unable to load parent departments right now. Please try again.");
            model.addAttribute("parentDepartmentRequests", List.of());
        }
        return "hr/department-request-list";
    }

    @GetMapping("/agency-rank-mapping")
    public String agencyRankMappingMenuEntry(Model model) {
        try {
            HrAgencyRankMappingListView listView = hrAgencyRankMappingService.getAgencyRankMappingListView();
            model.addAttribute("rankMappingListView", listView);
        } catch (DepartmentApplicationException ex) {
            log.warn("Unable to load HR agency rank mappings list. reason={}", ex.getMessage());
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("rankMappingListView", HrAgencyRankMappingListView.builder()
                    .rankMappings(List.of())
                    .build());
        } catch (Exception ex) {
            log.error("Unexpected error while loading HR agency rank mappings list.", ex);
            model.addAttribute("errorMessage", "Unable to load agency rank mappings right now. Please try again.");
            model.addAttribute("rankMappingListView", HrAgencyRankMappingListView.builder()
                    .rankMappings(List.of())
                    .build());
        }
        return "hr/department-request-agency-rank-list";
    }

    @GetMapping("/{departmentId}/subdepartments")
    public String subDepartmentProjectCounts(
            @PathVariable Long departmentId,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute(
                    "subDepartmentRequest",
                    hrDepartmentRequestService.getSubDepartmentProjectCounts(departmentId));
            return "hr/department-request-subdepartment-list";
        } catch (DepartmentApplicationException ex) {
            log.warn("Unable to load sub-department request counts for departmentId={}, reason={}",
                    departmentId,
                    ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/hr/department-requests";
        }
    }

    @GetMapping("/{departmentId}/subdepartments/{subDepartmentId}/applications")
    public String submittedApplications(
            @PathVariable Long departmentId,
            @PathVariable Long subDepartmentId,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute(
                    "applicationDetail",
                    hrDepartmentRequestService.getSubDepartmentApplications(departmentId, subDepartmentId));
            return "hr/department-request-applications";
        } catch (DepartmentApplicationException ex) {
            log.warn("Unable to load submitted applications for departmentId={}, subDepartmentId={}, reason={}",
                    departmentId,
                    subDepartmentId,
                    ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/hr/department-requests/" + departmentId + "/subdepartments";
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
            HrDepartmentApplicationReviewDetailView reviewDetail = hrDepartmentRequestService.getApplicationReviewDetail(
                    departmentId,
                    subDepartmentId,
                    applicationId);
            populateReviewModel(model, reviewDetail);
            if (!model.containsAttribute("reviewForm")) {
                model.addAttribute("reviewForm", new HrDepartmentReviewForm());
            }
            return "hr/department-request-application-detail";
        } catch (DepartmentApplicationException ex) {
            log.warn("Unable to load HR application detail. departmentId={}, subDepartmentId={}, applicationId={}, reason={}",
                    departmentId,
                    subDepartmentId,
                    applicationId,
                    ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/hr/department-requests/" + departmentId + "/subdepartments/" + subDepartmentId + "/applications";
        }
    }

    @PostMapping("/{departmentId}/subdepartments/{subDepartmentId}/applications/{applicationId}/review")
    public String reviewApplicationByHr(
            @PathVariable Long departmentId,
            @PathVariable Long subDepartmentId,
            @PathVariable Long applicationId,
            @Valid @ModelAttribute("reviewForm") HrDepartmentReviewForm reviewForm,
            BindingResult bindingResult,
            Model model,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        if (isRemarksMandatory(reviewForm.getDecision()) && !StringUtils.hasText(reviewForm.getRemarks())) {
            bindingResult.rejectValue("remarks", "reviewForm.remarks.required",
                    "Remarks are required for send back and reject decisions.");
        }

        if (bindingResult.hasErrors()) {
            try {
                HrDepartmentApplicationReviewDetailView reviewDetail = hrDepartmentRequestService.getApplicationReviewDetail(
                        departmentId,
                        subDepartmentId,
                        applicationId);
                populateReviewModel(model, reviewDetail);
                return "hr/department-request-application-detail";
            } catch (DepartmentApplicationException ex) {
                redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
                return "redirect:/hr/department-requests/" + departmentId + "/subdepartments/" + subDepartmentId + "/applications";
            }
        }

        try {
            hrDepartmentRequestService.reviewApplicationByHr(
                    departmentId,
                    subDepartmentId,
                    applicationId,
                    reviewForm.getDecision(),
                    reviewForm.getRemarks(),
                    resolveActorEmail(principal));

            redirectAttributes.addFlashAttribute("successMessage", "HR review decision applied successfully.");
            return "redirect:/hr/department-requests/" + departmentId + "/subdepartments/" + subDepartmentId
                    + "/applications/" + applicationId;
        } catch (DepartmentApplicationException ex) {
            log.warn("Unable to apply HR decision. departmentId={}, subDepartmentId={}, applicationId={}, reason={}",
                    departmentId,
                    subDepartmentId,
                    applicationId,
                    ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/hr/department-requests/" + departmentId + "/subdepartments/" + subDepartmentId
                    + "/applications/" + applicationId;
        }
    }

    @GetMapping("/{departmentId}/subdepartments/{subDepartmentId}/applications/{applicationId}/download-work-order")
    public ResponseEntity<Resource> downloadWorkOrder(
            @PathVariable Long departmentId,
            @PathVariable Long subDepartmentId,
            @PathVariable Long applicationId) {
        WorkOrderDocumentView documentView = hrDepartmentRequestService.getApplicationWorkOrderDocument(
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

    @GetMapping("/{departmentId}/subdepartments/{subDepartmentId}/applications/{applicationId}/agency-ranks")
    public String agencyRankMapping(
            @PathVariable Long departmentId,
            @PathVariable Long subDepartmentId,
            @PathVariable Long applicationId,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            HrAgencyRankMappingView rankMappingView = hrAgencyRankMappingService.getRankMappingView(
                    departmentId,
                    subDepartmentId,
                    applicationId);
            model.addAttribute("agencyRankMappingView", rankMappingView);

            if (!model.containsAttribute("rankMappingForm")) {
                model.addAttribute("rankMappingForm", buildRankMappingForm(rankMappingView));
            }

            return "hr/department-request-agency-rank-mapping";
        } catch (DepartmentApplicationException ex) {
            log.warn(
                    "Unable to load HR agency rank mapping page. departmentId={}, subDepartmentId={}, applicationId={}, reason={}",
                    departmentId,
                    subDepartmentId,
                    applicationId,
                    ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/hr/department-requests/" + departmentId + "/subdepartments/" + subDepartmentId
                    + "/applications/" + applicationId;
        }
    }

    @PostMapping("/{departmentId}/subdepartments/{subDepartmentId}/applications/{applicationId}/agency-ranks")
    public String saveAgencyRankMapping(
            @PathVariable Long departmentId,
            @PathVariable Long subDepartmentId,
            @PathVariable Long applicationId,
            @ModelAttribute("rankMappingForm") HrAgencyRankMappingForm rankMappingForm,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            hrAgencyRankMappingService.assignAgencyRanks(
                    departmentId,
                    subDepartmentId,
                    applicationId,
                    rankMappingForm != null ? rankMappingForm.getRankRows() : List.of());

            redirectAttributes.addFlashAttribute("successMessage", "Agency rank mapping saved successfully.");
            return "redirect:/hr/department-requests/" + departmentId + "/subdepartments/" + subDepartmentId
                    + "/applications/" + applicationId + "/agency-ranks";
        } catch (DepartmentApplicationException ex) {
            HrAgencyRankMappingView rankMappingView = hrAgencyRankMappingService.getRankMappingView(
                    departmentId,
                    subDepartmentId,
                    applicationId);
            model.addAttribute("agencyRankMappingView", rankMappingView);
            model.addAttribute("errorMessage", ex.getMessage());
            if (rankMappingForm == null) {
                model.addAttribute("rankMappingForm", buildRankMappingForm(rankMappingView));
            }
            return "hr/department-request-agency-rank-mapping";
        }
    }

    private void populateReviewModel(Model model, HrDepartmentApplicationReviewDetailView reviewDetail) {
        model.addAttribute("applicationReviewDetail", reviewDetail);
    }

    private String resolveActorEmail(Principal principal) {
        if (principal == null || !StringUtils.hasText(principal.getName())) {
            throw new DepartmentApplicationException("Authenticated user is required.");
        }
        return principal.getName();
    }

    private boolean isRemarksMandatory(HrReviewDecision decision) {
        return decision == HrReviewDecision.SEND_BACK || decision == HrReviewDecision.REJECT;
    }

    private HrAgencyRankMappingForm buildRankMappingForm(HrAgencyRankMappingView rankMappingView) {
        HrAgencyRankMappingForm form = new HrAgencyRankMappingForm();
        if (rankMappingView == null || rankMappingView.getAssignedAgencyRanks() == null
                || rankMappingView.getAssignedAgencyRanks().isEmpty()) {
            form.getRankRows().add(new HrAgencyRankRowForm());
            return form;
        }

        rankMappingView.getAssignedAgencyRanks().forEach(assignedRank -> {
            HrAgencyRankRowForm row = new HrAgencyRankRowForm();
            row.setAgencyId(assignedRank.getAgencyId());
            row.setRankNumber(assignedRank.getRankNumber());
            form.getRankRows().add(row);
        });
        return form;
    }

}
