package com.maharecruitment.gov.in.department.controller;

import java.time.LocalDate;
import java.security.Principal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.maharecruitment.gov.in.department.dto.DepartmentProjectApplicationActivityView;
import com.maharecruitment.gov.in.department.dto.DepartmentProjectApplicationForm;
import com.maharecruitment.gov.in.department.dto.DepartmentTaxRateView;
import com.maharecruitment.gov.in.department.dto.LevelOptionView;
import com.maharecruitment.gov.in.department.entity.DepartmentApplicationType;
import com.maharecruitment.gov.in.department.exception.DepartmentApplicationException;
import com.maharecruitment.gov.in.department.service.DepartmentManpowerApplicationService;
import com.maharecruitment.gov.in.department.service.DepartmentTaxRateQueryService;
import com.maharecruitment.gov.in.department.service.DepartmentWorkOrderStorageService;
import com.maharecruitment.gov.in.department.service.model.WorkOrderDocumentView;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/department/manpower")
public class DepartmentManpowerApplicationController {

    private static final Logger log = LoggerFactory.getLogger(DepartmentManpowerApplicationController.class);

    private final DepartmentManpowerApplicationService manpowerApplicationService;
    private final DepartmentTaxRateQueryService taxRateQueryService;
    private final DepartmentWorkOrderStorageService workOrderStorageService;

    public DepartmentManpowerApplicationController(
            DepartmentManpowerApplicationService manpowerApplicationService,
            DepartmentTaxRateQueryService taxRateQueryService,
            DepartmentWorkOrderStorageService workOrderStorageService) {
        this.manpowerApplicationService = manpowerApplicationService;
        this.taxRateQueryService = taxRateQueryService;
        this.workOrderStorageService = workOrderStorageService;
    }

    @GetMapping("/apply")
    public String create(Model model, Principal principal) {
        String actorEmail = resolveActorEmail(principal);
        DepartmentProjectApplicationForm form = manpowerApplicationService.initializeApplicationForm(actorEmail);
        populateFormModel(model, form, List.of());
        return "department/manpower-application-form";
    }

    @GetMapping("/{applicationId}/edit")
    public String edit(@PathVariable Long applicationId, Model model, Principal principal, RedirectAttributes redirectAttributes) {
        String actorEmail = resolveActorEmail(principal);
        try {
            DepartmentProjectApplicationForm form = manpowerApplicationService.getApplicationForEdit(applicationId, actorEmail);
            List<DepartmentProjectApplicationActivityView> activities = manpowerApplicationService
                    .getApplicationActivities(applicationId, actorEmail);
            populateFormModel(model, form, activities);
            return "department/manpower-application-form";
        } catch (DepartmentApplicationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/department/manpower/list";
        }
    }

    @PostMapping("/save")
    public String save(
            @Valid @ModelAttribute("applicationForm") DepartmentProjectApplicationForm applicationForm,
            BindingResult bindingResult,
            @RequestParam(defaultValue = "submit") String actionStatus,
            Model model,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        String actorEmail = resolveActorEmail(principal);

        if (bindingResult.hasErrors()) {
            List<DepartmentProjectApplicationActivityView> activities = getActivitiesSafe(
                    applicationForm.getDepartmentProjectApplicationId(),
                    actorEmail);
            populateFormModel(model, applicationForm, activities);
            return "department/manpower-application-form";
        }

        try {
            Long savedApplicationId = manpowerApplicationService.saveApplication(applicationForm, actionStatus, actorEmail);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Application saved successfully. Application ID: " + savedApplicationId);
            return "redirect:/department/manpower/list";
        } catch (DepartmentApplicationException ex) {
            log.warn("Unable to save department manpower application. action={}, reason={}", actionStatus, ex.getMessage());
            model.addAttribute("errorMessage", ex.getMessage());
            List<DepartmentProjectApplicationActivityView> activities = getActivitiesSafe(
                    applicationForm.getDepartmentProjectApplicationId(),
                    actorEmail);
            populateFormModel(model, applicationForm, activities);
            return "department/manpower-application-form";
        }
    }

    @GetMapping("/list")
    public String list(Model model, Principal principal) {
        String actorEmail = resolveActorEmail(principal);
        model.addAttribute("applications", manpowerApplicationService.getApplicationSummaries(actorEmail));
        return "department/manpower-application-list";
    }

    @GetMapping("/by-designation/{designationId}")
    @ResponseBody
    public List<LevelOptionView> getLevelsByDesignation(@PathVariable Long designationId) {
        return manpowerApplicationService.getLevelsByDesignation(designationId);
    }

    @GetMapping("/rate")
    @ResponseBody
    public String getRate(
            @RequestParam Long designationId,
            @RequestParam String levelCode) {
        return manpowerApplicationService.getMonthlyRate(designationId, levelCode).toPlainString();
    }

    @GetMapping("/tax-rates")
    @ResponseBody
    public List<DepartmentTaxRateView> getApplicableTaxRates(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate applicableDate) {
        return taxRateQueryService.getApplicableTaxRates(applicableDate);
    }

    @GetMapping("/{applicationId}/activities")
    @ResponseBody
    public List<DepartmentProjectApplicationActivityView> getActivities(
            @PathVariable Long applicationId,
            Principal principal) {
        String actorEmail = resolveActorEmail(principal);
        return manpowerApplicationService.getApplicationActivities(applicationId, actorEmail);
    }

    @GetMapping("/download-work-order/{applicationId}")
    public ResponseEntity<Resource> downloadWorkOrder(
            @PathVariable Long applicationId,
            Principal principal) {
        String actorEmail = resolveActorEmail(principal);
        WorkOrderDocumentView documentView = manpowerApplicationService.getWorkOrderDocument(applicationId, actorEmail);
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

    private void populateFormModel(
            Model model,
            DepartmentProjectApplicationForm form,
            List<DepartmentProjectApplicationActivityView> activityTimeline) {
        model.addAttribute("applicationForm", form);
        model.addAttribute("applicationTypes", DepartmentApplicationType.values());
        model.addAttribute("designationOptions", manpowerApplicationService.getAvailableDesignations());
        model.addAttribute("activityTimeline", activityTimeline == null ? List.of() : activityTimeline);
    }

    private String resolveActorEmail(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new DepartmentApplicationException("Authenticated user is required.");
        }
        return principal.getName();
    }

    private List<DepartmentProjectApplicationActivityView> getActivitiesSafe(Long applicationId, String actorEmail) {
        if (applicationId == null) {
            return List.of();
        }

        try {
            return manpowerApplicationService.getApplicationActivities(applicationId, actorEmail);
        } catch (RuntimeException ex) {
            log.warn("Unable to load activity timeline for applicationId={}", applicationId, ex);
            return List.of();
        }
    }
}
