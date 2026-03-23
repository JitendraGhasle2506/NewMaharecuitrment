package com.maharecruitment.gov.in.recruitment.controller;

import java.security.Principal;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.maharecruitment.gov.in.recruitment.service.InternalVacancyCandidateReviewService;
import com.maharecruitment.gov.in.recruitment.service.InternalVacancyOpeningService;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyCandidateListView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyOpeningCommand;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyOpeningLevelOptionView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyOpeningResult;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyRequirementCommand;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/hr/internal-vacancies")
@PreAuthorize("hasAnyAuthority('ROLE_HR', 'HR')")
public class HRInternalVacancyOpeningController {

    private static final Logger log = LoggerFactory.getLogger(HRInternalVacancyOpeningController.class);

    private final InternalVacancyOpeningService internalVacancyOpeningService;
    private final InternalVacancyCandidateReviewService internalVacancyCandidateReviewService;

    public HRInternalVacancyOpeningController(
            InternalVacancyOpeningService internalVacancyOpeningService,
            InternalVacancyCandidateReviewService internalVacancyCandidateReviewService) {
        this.internalVacancyOpeningService = internalVacancyOpeningService;
        this.internalVacancyCandidateReviewService = internalVacancyCandidateReviewService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("openings", internalVacancyOpeningService.getAllOpenings());
        return "hr/internal-vacancy-opening-list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        populateFormModel(model, new InternalVacancyOpeningForm(), false);
        return "hr/internal-vacancy-opening-form";
    }

    @GetMapping("/candidates")
    public String viewAllSubmittedCandidates(Model model) {
        model.addAttribute("requestSummaries", internalVacancyCandidateReviewService.getCandidateRequestSummaries());
        return "hr/internal-vacancy-candidate-all-list";
    }

    @GetMapping("/request/{requestId}/candidates")
    public String viewSubmittedCandidates(
            @PathVariable String requestId,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            InternalVacancyCandidateListView candidateListView = internalVacancyCandidateReviewService
                    .getSubmittedCandidatesByRequestId(requestId);
            model.addAttribute("candidateListView", candidateListView);
            return "hr/internal-vacancy-candidate-list";
        } catch (RecruitmentNotificationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/hr/internal-vacancies";
        }
    }

    @GetMapping("/{internalVacancyOpeningId}/edit")
    public String editForm(
            @PathVariable Long internalVacancyOpeningId,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            populateFormModel(
                    model,
                    internalVacancyOpeningService.getOpeningForEdit(internalVacancyOpeningId),
                    true);
            return "hr/internal-vacancy-opening-form";
        } catch (RecruitmentNotificationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/hr/internal-vacancies";
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
            return "hr/internal-vacancy-opening-form";
        }

        try {
            InternalVacancyOpeningStatus targetStatus = resolveTargetStatus(action);
            InternalVacancyOpeningResult result = internalVacancyOpeningService.saveOpening(
                    toCommand(openingForm, actorEmail, targetStatus));
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    buildSuccessMessage(targetStatus, result.getRequestId(), editMode));
            return "redirect:/hr/internal-vacancies";
        } catch (RecruitmentNotificationException ex) {
            log.warn("Unable to create internal vacancy opening. actor={}, reason={}", actorEmail, ex.getMessage());
            model.addAttribute("errorMessage", ex.getMessage());
            populateFormModel(model, openingForm, editMode);
            return "hr/internal-vacancy-opening-form";
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
                .build();
    }

    private InternalVacancyOpeningStatus resolveTargetStatus(String action) {
        if (action == null) {
            throw new RecruitmentNotificationException("Vacancy opening action is required.");
        }

        return switch (action.trim().toLowerCase(Locale.ROOT)) {
            case "draft" -> InternalVacancyOpeningStatus.DRAFT;
            case "submit" -> InternalVacancyOpeningStatus.OPEN;
            default -> throw new RecruitmentNotificationException("Unsupported vacancy opening action.");
        };
    }

    private String buildSuccessMessage(
            InternalVacancyOpeningStatus targetStatus,
            String requestId,
            boolean editMode) {
        String operationLabel = editMode ? "updated" : "created";
        if (targetStatus == InternalVacancyOpeningStatus.DRAFT) {
            return "Internal vacancy draft " + operationLabel + " successfully. Request ID: " + requestId;
        }
        return "Internal vacancy opening submitted successfully. Request ID: " + requestId;
    }

    private String resolveActorEmail(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new RecruitmentNotificationException("Authenticated user is required.");
        }
        return principal.getName();
    }
}
