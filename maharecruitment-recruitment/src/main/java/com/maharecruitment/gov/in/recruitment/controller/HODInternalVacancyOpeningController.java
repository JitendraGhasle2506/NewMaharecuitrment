package com.maharecruitment.gov.in.recruitment.controller;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
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

import com.maharecruitment.gov.in.recruitment.dto.hr.InternalVacancyOpeningForm;
import com.maharecruitment.gov.in.recruitment.entity.InternalVacancyOpeningStatus;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.recruitment.service.InternalVacancyOpeningService;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyOpeningCommand;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyOpeningLevelOptionView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyOpeningListMetricsView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyOpeningResult;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyOpeningSummaryView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyRequirementCommand;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/hod1/internal-vacancies")
@PreAuthorize("hasAnyAuthority('ROLE_HOD', 'ROLE_HOD1')")
public class HODInternalVacancyOpeningController {

    private static final Logger log = LoggerFactory.getLogger(HODInternalVacancyOpeningController.class);
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;

    private final InternalVacancyOpeningService internalVacancyOpeningService;

    public HODInternalVacancyOpeningController(InternalVacancyOpeningService internalVacancyOpeningService) {
        this.internalVacancyOpeningService = internalVacancyOpeningService;
    }

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(name = "search", required = false) String search,
            Principal principal,
            Model model) {
        int resolvedPage = Math.max(page, 0);
        int resolvedSize = resolvePageSize(size);
        String normalizedSearch = normalizeSearch(search);
        String actorEmail = resolveActorEmail(principal);

        Page<InternalVacancyOpeningSummaryView> openingPage = internalVacancyOpeningService.getOpeningPage(
                normalizedSearch,
                actorEmail, // HOD only sees their own
                List.of(), // HOD sees all their statuses including drafts
                PageRequest.of(resolvedPage, resolvedSize, Sort.by("internalVacancyOpeningId").descending()));

        InternalVacancyOpeningListMetricsView openingMetrics = internalVacancyOpeningService
                .getOpeningListMetrics(normalizedSearch, actorEmail, List.of());

        model.addAttribute("openings", openingPage.getContent());
        model.addAttribute("openingPage", openingPage);
        model.addAttribute("openingMetrics", openingMetrics);
        model.addAttribute("searchTerm", normalizedSearch == null ? "" : normalizedSearch);
        model.addAttribute("pageSize", openingPage.getSize());
        return "hod/internal-vacancy-opening-list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        populateFormModel(model, new InternalVacancyOpeningForm(), false);
        return "hod/internal-vacancy-opening-form";
    }

    @GetMapping("/{internalVacancyOpeningId}/edit")
    public String editForm(
            @PathVariable Long internalVacancyOpeningId,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            InternalVacancyOpeningForm form = internalVacancyOpeningService.getOpeningForEdit(internalVacancyOpeningId);
            if (form.getCurrentStatus() != InternalVacancyOpeningStatus.DRAFT && 
                form.getCurrentStatus() != InternalVacancyOpeningStatus.PENDING_HR_APPROVAL) {
                throw new RecruitmentNotificationException("Only draft or pending requests can be edited by HOD.");
            }
            populateFormModel(model, form, true);
            return "hod/internal-vacancy-opening-form";
        } catch (RecruitmentNotificationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/hod1/internal-vacancies";
        }
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("openingForm") InternalVacancyOpeningForm openingForm,
            BindingResult bindingResult,
            @RequestParam("action") String action,
            Model model,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        String actorEmail = resolveActorEmail(principal);
        boolean editMode = openingForm.getInternalVacancyOpeningId() != null;

        if (bindingResult.hasErrors()) {
            populateFormModel(model, openingForm, editMode);
            return "hod/internal-vacancy-opening-form";
        }

        try {
            InternalVacancyOpeningStatus targetStatus = resolveTargetStatus(action);
            InternalVacancyOpeningResult result = internalVacancyOpeningService.saveOpening(
                    toCommand(openingForm, actorEmail, targetStatus));
            
            String message = targetStatus == InternalVacancyOpeningStatus.PENDING_HR_APPROVAL 
                ? "Resource requirement request submitted to HR successfully. Request ID: " + result.getRequestId()
                : "Internal vacancy draft saved successfully. Request ID: " + result.getRequestId();
            
            redirectAttributes.addFlashAttribute("successMessage", message);
            return "redirect:/hod1/internal-vacancies";
        } catch (RecruitmentNotificationException ex) {
            log.warn("Unable to process HOD resource request. actor={}, reason={}", actorEmail, ex.getMessage());
            model.addAttribute("errorMessage", ex.getMessage());
            populateFormModel(model, openingForm, editMode);
            return "hod/internal-vacancy-opening-form";
        }
    }

    @GetMapping("/by-designation/{designationId}")
    @ResponseBody
    public List<InternalVacancyOpeningLevelOptionView> getLevelsByDesignation(@PathVariable Long designationId) {
        return internalVacancyOpeningService.getLevelsByDesignation(designationId);
    }

    private void populateFormModel(Model model, InternalVacancyOpeningForm openingForm, boolean isEdit) {
        model.addAttribute("openingForm", openingForm);
        model.addAttribute("isEdit", isEdit);
        model.addAttribute("projectOptions", internalVacancyOpeningService.getAvailableInternalProjects());
        model.addAttribute("designationOptions", internalVacancyOpeningService.getAvailableDesignations());
    }

    private InternalVacancyOpeningCommand toCommand(
            InternalVacancyOpeningForm openingForm,
            String actorEmail,
            InternalVacancyOpeningStatus targetStatus) {
        return InternalVacancyOpeningCommand.builder()
                .internalVacancyOpeningId(openingForm.getInternalVacancyOpeningId())
                .projectId(openingForm.getProjectId())
                .remarks(openingForm.getRemarks())
                .actorEmail(actorEmail)
                .targetStatus(targetStatus)
                .requirements(openingForm.getRequirements().stream()
                        .map(requirement -> InternalVacancyRequirementCommand.builder()
                                .designationId(requirement.getDesignationId())
                                .levelCode(requirement.getLevelCode())
                                .numberOfVacancy(requirement.getNumberOfVacancy())
                                .build())
                        .toList())
                .interviewAuthorityRoleIds(new ArrayList<>()) // HOD doesn't configure these
                .interviewAuthorityUserIds(new ArrayList<>()) // HOD doesn't configure these
                .interviewAuthorityEmployeeIds(new ArrayList<>()) // HOD doesn't configure these
                .build();
    }

    private InternalVacancyOpeningStatus resolveTargetStatus(String action) {
        if (action == null) {
            throw new RecruitmentNotificationException("Vacancy opening action is required.");
        }

        String normalizedAction = action.trim().toLowerCase(Locale.ROOT);
        if ("draft".equals(normalizedAction)) {
            return InternalVacancyOpeningStatus.DRAFT;
        }
        if ("submit".equals(normalizedAction)) {
            return InternalVacancyOpeningStatus.PENDING_HR_APPROVAL;
        }
        throw new RecruitmentNotificationException("Unsupported vacancy opening action.");
    }

    private String resolveActorEmail(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new RecruitmentNotificationException("Authenticated user is required.");
        }
        return principal.getName();
    }


    private int resolvePageSize(int size) {
        if (size \u003c= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private String normalizeSearch(String search) {
        return search == null || search.isBlank() ? null : search.trim();
    }
}
