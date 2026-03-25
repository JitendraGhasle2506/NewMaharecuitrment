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
import com.maharecruitment.gov.in.recruitment.service.InternalVacancyCandidateReviewService;
import com.maharecruitment.gov.in.recruitment.service.InternalVacancyOpeningService;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyInterviewAuthorityUserOptionView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyCandidateListView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyCandidateRequestListMetricsView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyOpeningCommand;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyOpeningListMetricsView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyOpeningLevelOptionView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyOpeningResult;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyOpeningSummaryView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyCandidateRequestSummaryView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyRequirementCommand;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/hr/internal-vacancies")
@PreAuthorize("hasAuthority('ROLE_HR')")
public class HRInternalVacancyOpeningController {

    private static final Logger log = LoggerFactory.getLogger(HRInternalVacancyOpeningController.class);
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;

    private final InternalVacancyOpeningService internalVacancyOpeningService;
    private final InternalVacancyCandidateReviewService internalVacancyCandidateReviewService;

    public HRInternalVacancyOpeningController(
            InternalVacancyOpeningService internalVacancyOpeningService,
            InternalVacancyCandidateReviewService internalVacancyCandidateReviewService) {
        this.internalVacancyOpeningService = internalVacancyOpeningService;
        this.internalVacancyCandidateReviewService = internalVacancyCandidateReviewService;
    }

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(name = "search", required = false) String search,
            Model model) {
        int resolvedPage = Math.max(page, 0);
        int resolvedSize = resolvePageSize(size);
        String normalizedSearch = normalizeSearch(search);

        Page<InternalVacancyOpeningSummaryView> openingPage = loadOpeningPage(
                normalizedSearch,
                resolvedPage,
                resolvedSize);
        if (openingPage.getTotalPages() > 0 && resolvedPage >= openingPage.getTotalPages()) {
            openingPage = loadOpeningPage(
                    normalizedSearch,
                    openingPage.getTotalPages() - 1,
                    resolvedSize);
        }

        InternalVacancyOpeningListMetricsView openingMetrics = internalVacancyOpeningService
                .getOpeningListMetrics(normalizedSearch);

        model.addAttribute("openings", openingPage.getContent());
        model.addAttribute("openingPage", openingPage);
        model.addAttribute("openingMetrics", openingMetrics);
        model.addAttribute("searchTerm", normalizedSearch == null ? "" : normalizedSearch);
        model.addAttribute("pageSize", openingPage.getSize());
        return "hr/internal-vacancy-opening-list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        populateFormModel(model, new InternalVacancyOpeningForm(), false);
        return "hr/internal-vacancy-opening-form";
    }

    @GetMapping("/candidates")
    public String viewAllSubmittedCandidates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(name = "search", required = false) String search,
            Model model) {
        int resolvedPage = Math.max(page, 0);
        int resolvedSize = resolvePageSize(size);
        String normalizedSearch = normalizeSearch(search);

        Page<InternalVacancyCandidateRequestSummaryView> requestSummaryPage = loadCandidateSummaryPage(
                normalizedSearch,
                resolvedPage,
                resolvedSize);
        if (requestSummaryPage.getTotalPages() > 0 && resolvedPage >= requestSummaryPage.getTotalPages()) {
            requestSummaryPage = loadCandidateSummaryPage(
                    normalizedSearch,
                    requestSummaryPage.getTotalPages() - 1,
                    resolvedSize);
        }

        InternalVacancyCandidateRequestListMetricsView candidateSummaryMetrics = internalVacancyCandidateReviewService
                .getCandidateRequestSummaryMetrics(normalizedSearch);

        model.addAttribute("requestSummaries", requestSummaryPage.getContent());
        model.addAttribute("requestSummaryPage", requestSummaryPage);
        model.addAttribute("candidateSummaryMetrics", candidateSummaryMetrics);
        model.addAttribute("searchTerm", normalizedSearch == null ? "" : normalizedSearch);
        model.addAttribute("pageSize", requestSummaryPage.getSize());
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

    @PostMapping("/{internalVacancyOpeningId}/status")
    public String updateStatus(
            @PathVariable Long internalVacancyOpeningId,
            @RequestParam("action") String action,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "search", required = false) String search,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        String actorEmail = resolveActorEmail(principal);

        try {
            InternalVacancyOpeningStatus targetStatus = resolveStatusAction(action);
            InternalVacancyOpeningResult result = internalVacancyOpeningService.changeOpeningStatus(
                    internalVacancyOpeningId,
                    actorEmail,
                    targetStatus);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    buildStatusChangeMessage(targetStatus, result.getRequestId()));
        } catch (RecruitmentNotificationException ex) {
            log.warn(
                    "Unable to update internal vacancy opening status. openingId={}, actor={}, reason={}",
                    internalVacancyOpeningId,
                    actorEmail,
                    ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        redirectAttributes.addAttribute("page", Math.max(page, 0));
        redirectAttributes.addAttribute("size", resolvePageSize(size));
        String normalizedSearch = normalizeSearch(search);
        if (normalizedSearch != null) {
            redirectAttributes.addAttribute("search", normalizedSearch);
        }
        return "redirect:/hr/internal-vacancies";
    }

    @GetMapping("/by-designation/{designationId}")
    @ResponseBody
    public List<InternalVacancyOpeningLevelOptionView> getLevelsByDesignation(@PathVariable Long designationId) {
        return internalVacancyOpeningService.getLevelsByDesignation(designationId);
    }

    @GetMapping("/interview-authorities")
    @ResponseBody
    public List<InternalVacancyInterviewAuthorityUserOptionView> getInterviewAuthorities(
            @RequestParam(name = "roleIds", required = false) List<Long> roleIds) {
        return internalVacancyOpeningService.getAvailableInterviewAuthorities(roleIds);
    }

    private void populateFormModel(Model model, InternalVacancyOpeningForm openingForm, boolean isEdit) {
        model.addAttribute("openingForm", openingForm);
        model.addAttribute("isEdit", isEdit);
        model.addAttribute("isSubmittedEdit", isEdit && openingForm.getCurrentStatus() == InternalVacancyOpeningStatus.OPEN);
        model.addAttribute("projectOptions", internalVacancyOpeningService.getAvailableInternalProjects());
        model.addAttribute("designationOptions", internalVacancyOpeningService.getAvailableDesignations());
        model.addAttribute("interviewAuthorityRoleOptions", internalVacancyOpeningService.getAvailableInterviewAuthorityRoles());
        model.addAttribute(
                "interviewAuthorityUserOptions",
                internalVacancyOpeningService.getAvailableInterviewAuthorities(
                        openingForm.getInterviewAuthorityRoleIds() == null
                                ? List.of()
                                : openingForm.getInterviewAuthorityRoleIds()));
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
                .interviewAuthorityRoleIds(
                        openingForm.getInterviewAuthorityRoleIds() == null
                                ? List.of()
                                : new ArrayList<>(openingForm.getInterviewAuthorityRoleIds()))
                .interviewAuthorityUserIds(
                        openingForm.getInterviewAuthorityUserIds() == null
                                ? List.of()
                                : new ArrayList<>(openingForm.getInterviewAuthorityUserIds()))
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
            return InternalVacancyOpeningStatus.OPEN;
        }
        throw new RecruitmentNotificationException("Unsupported vacancy opening action.");
    }

    private InternalVacancyOpeningStatus resolveStatusAction(String action) {
        if (action == null) {
            throw new RecruitmentNotificationException("Vacancy opening status action is required.");
        }

        String normalizedAction = action.trim().toLowerCase(Locale.ROOT);
        if ("activate".equals(normalizedAction)) {
            return InternalVacancyOpeningStatus.OPEN;
        }
        if ("deactivate".equals(normalizedAction)) {
            return InternalVacancyOpeningStatus.CLOSED;
        }
        throw new RecruitmentNotificationException("Unsupported vacancy opening status action.");
    }

    private String buildSuccessMessage(
            InternalVacancyOpeningStatus targetStatus,
            String requestId,
            boolean editMode) {
        String operationLabel = editMode ? "updated" : "created";
        if (targetStatus == InternalVacancyOpeningStatus.DRAFT) {
            return "Internal vacancy draft " + operationLabel + " successfully. Request ID: " + requestId;
        }
        if (editMode) {
            return "Internal vacancy opening updated successfully. Request ID: " + requestId;
        }
        return "Internal vacancy opening submitted successfully. Request ID: " + requestId;
    }

    private String buildStatusChangeMessage(
            InternalVacancyOpeningStatus targetStatus,
            String requestId) {
        if (targetStatus == InternalVacancyOpeningStatus.OPEN) {
            return "Internal vacancy opening activated successfully. Request ID: " + requestId;
        }
        if (targetStatus == InternalVacancyOpeningStatus.CLOSED) {
            return "Internal vacancy opening deactivated successfully. Request ID: " + requestId;
        }
        return "Internal vacancy opening status updated successfully. Request ID: " + requestId;
    }

    private String resolveActorEmail(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new RecruitmentNotificationException("Authenticated user is required.");
        }
        return principal.getName();
    }

    private Page<InternalVacancyOpeningSummaryView> loadOpeningPage(
            String normalizedSearch,
            int page,
            int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                resolvePageSize(size),
                Sort.by(Sort.Direction.DESC, "internalVacancyOpeningId"));
        return internalVacancyOpeningService.getOpeningPage(normalizedSearch, pageable);
    }

    private Page<InternalVacancyCandidateRequestSummaryView> loadCandidateSummaryPage(
            String normalizedSearch,
            int page,
            int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), resolvePageSize(size));
        return internalVacancyCandidateReviewService.getCandidateRequestSummaryPage(normalizedSearch, pageable);
    }

    private int resolvePageSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private String normalizeSearch(String search) {
        return search == null || search.isBlank() ? null : search.trim();
    }
}
