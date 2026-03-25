package com.maharecruitment.gov.in.recruitment.controller;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.maharecruitment.gov.in.recruitment.dto.hr.InternalVacancyLevelTwoChangeRequestForm;
import com.maharecruitment.gov.in.recruitment.dto.hr.InternalVacancyLevelTwoPanelAssignmentForm;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.recruitment.service.InternalVacancyLevelTwoWorkflowService;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyLevelTwoCandidateSummaryView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyLevelTwoPanelMemberView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyLevelTwoWorkflowDetailView;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/hr/internal-vacancies/level-two")
@PreAuthorize("hasAuthority('ROLE_HR')")
public class HRInternalVacancyLevelTwoController {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;

    private final InternalVacancyLevelTwoWorkflowService workflowService;

    public HRInternalVacancyLevelTwoController(InternalVacancyLevelTwoWorkflowService workflowService) {
        this.workflowService = workflowService;
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

        Page<InternalVacancyLevelTwoCandidateSummaryView> candidatePage = loadCandidatePage(
                normalizedSearch,
                resolvedPage,
                resolvedSize);
        if (candidatePage.getTotalPages() > 0 && resolvedPage >= candidatePage.getTotalPages()) {
            candidatePage = loadCandidatePage(
                    normalizedSearch,
                    candidatePage.getTotalPages() - 1,
                    resolvedSize);
        }

        model.addAttribute("candidatePage", candidatePage);
        model.addAttribute("levelTwoCandidates", candidatePage.getContent());
        model.addAttribute("searchTerm", normalizedSearch == null ? "" : normalizedSearch);
        model.addAttribute("pageSize", candidatePage.getSize());
        return "hr/internal-vacancy-level-two-list";
    }

    @GetMapping("/{recruitmentInterviewDetailId}")
    public String detail(
            @PathVariable Long recruitmentInterviewDetailId,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            populateDetailModel(model, workflowService.getWorkflowDetail(recruitmentInterviewDetailId));
            return "hr/internal-vacancy-level-two-detail";
        } catch (RecruitmentNotificationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/hr/internal-vacancies/level-two";
        }
    }

    @PostMapping("/{recruitmentInterviewDetailId}/panel")
    public String assignPanel(
            @PathVariable Long recruitmentInterviewDetailId,
            @Valid @ModelAttribute("panelAssignmentForm") InternalVacancyLevelTwoPanelAssignmentForm panelAssignmentForm,
            BindingResult bindingResult,
            @ModelAttribute("changeRequestForm") InternalVacancyLevelTwoChangeRequestForm changeRequestForm,
            Principal principal,
            RedirectAttributes redirectAttributes,
            Model model) {
        InternalVacancyLevelTwoWorkflowDetailView detail;
        try {
            detail = workflowService.getWorkflowDetail(recruitmentInterviewDetailId);
        } catch (RecruitmentNotificationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/hr/internal-vacancies/level-two";
        }
        if (bindingResult.hasErrors()) {
            populateDetailModel(model, detail, panelAssignmentForm, ensureChangeRequestForm(changeRequestForm, detail));
            return "hr/internal-vacancy-level-two-detail";
        }

        try {
            workflowService.assignInterviewPanel(
                    recruitmentInterviewDetailId,
                    resolveActorEmail(principal),
                    panelAssignmentForm.getSelectedUserIds());
            redirectAttributes.addFlashAttribute("successMessage", "Interview panel saved successfully.");
            return "redirect:/hr/internal-vacancies/level-two/" + recruitmentInterviewDetailId;
        } catch (RecruitmentNotificationException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            populateDetailModel(model, detail, panelAssignmentForm, ensureChangeRequestForm(changeRequestForm, detail));
            return "hr/internal-vacancy-level-two-detail";
        }
    }

    @PostMapping("/{recruitmentInterviewDetailId}/request-time-change")
    public String requestTimeChange(
            @PathVariable Long recruitmentInterviewDetailId,
            @Valid @ModelAttribute("changeRequestForm") InternalVacancyLevelTwoChangeRequestForm changeRequestForm,
            BindingResult bindingResult,
            @ModelAttribute("panelAssignmentForm") InternalVacancyLevelTwoPanelAssignmentForm panelAssignmentForm,
            Principal principal,
            RedirectAttributes redirectAttributes,
            Model model) {
        InternalVacancyLevelTwoWorkflowDetailView detail;
        try {
            detail = workflowService.getWorkflowDetail(recruitmentInterviewDetailId);
        } catch (RecruitmentNotificationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/hr/internal-vacancies/level-two";
        }
        if (bindingResult.hasErrors()) {
            populateDetailModel(model, detail, ensurePanelAssignmentForm(panelAssignmentForm, detail), changeRequestForm);
            return "hr/internal-vacancy-level-two-detail";
        }

        try {
            workflowService.requestInterviewTimeChange(
                    recruitmentInterviewDetailId,
                    resolveActorEmail(principal),
                    changeRequestForm.getChangeReason());
            redirectAttributes.addFlashAttribute("successMessage", "Interview reschedule request sent to agency.");
            return "redirect:/hr/internal-vacancies/level-two/" + recruitmentInterviewDetailId;
        } catch (RecruitmentNotificationException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            populateDetailModel(model, detail, ensurePanelAssignmentForm(panelAssignmentForm, detail), changeRequestForm);
            return "hr/internal-vacancy-level-two-detail";
        }
    }

    private void populateDetailModel(Model model, InternalVacancyLevelTwoWorkflowDetailView detail) {
        populateDetailModel(
                model,
                detail,
                buildPanelAssignmentForm(detail),
                buildChangeRequestForm(detail));
    }

    private void populateDetailModel(
            Model model,
            InternalVacancyLevelTwoWorkflowDetailView detail,
            InternalVacancyLevelTwoPanelAssignmentForm panelAssignmentForm,
            InternalVacancyLevelTwoChangeRequestForm changeRequestForm) {
        model.addAttribute("detail", detail);
        model.addAttribute("eligiblePanelUsers", workflowService.getEligiblePanelUsers());
        model.addAttribute("panelAssignmentForm", ensurePanelAssignmentForm(panelAssignmentForm, detail));
        model.addAttribute("changeRequestForm", ensureChangeRequestForm(changeRequestForm, detail));
    }

    private InternalVacancyLevelTwoPanelAssignmentForm buildPanelAssignmentForm(
            InternalVacancyLevelTwoWorkflowDetailView detail) {
        InternalVacancyLevelTwoPanelAssignmentForm form = new InternalVacancyLevelTwoPanelAssignmentForm();
        List<Long> selectedUserIds = detail.getPanelMembers() == null
                ? new ArrayList<>()
                : detail.getPanelMembers().stream()
                        .map(InternalVacancyLevelTwoPanelMemberView::getPanelUserId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .collect(Collectors.toCollection(ArrayList::new));
        List<String> selectedRoleLabels = detail.getPanelMembers() == null
                ? new ArrayList<>()
                : detail.getPanelMembers().stream()
                        .map(InternalVacancyLevelTwoPanelMemberView::getPanelMemberDesignation)
                        .filter(Objects::nonNull)
                        .flatMap(value -> List.of(value.split(",")).stream())
                        .map(String::trim)
                        .filter(value -> !value.isBlank())
                        .distinct()
                        .collect(Collectors.toCollection(ArrayList::new));
        form.setSelectedRoleLabels(selectedRoleLabels);
        form.setSelectedUserIds(selectedUserIds);
        return form;
    }

    private InternalVacancyLevelTwoChangeRequestForm buildChangeRequestForm(
            InternalVacancyLevelTwoWorkflowDetailView detail) {
        InternalVacancyLevelTwoChangeRequestForm form = new InternalVacancyLevelTwoChangeRequestForm();
        form.setChangeReason(detail.getTimeChangeReason());
        return form;
    }

    private InternalVacancyLevelTwoPanelAssignmentForm ensurePanelAssignmentForm(
            InternalVacancyLevelTwoPanelAssignmentForm form,
            InternalVacancyLevelTwoWorkflowDetailView detail) {
        if (form == null) {
            return buildPanelAssignmentForm(detail);
        }
        if (form.getSelectedRoleLabels() == null) {
            form.setSelectedRoleLabels(new ArrayList<>());
        } else {
            form.setSelectedRoleLabels(form.getSelectedRoleLabels().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .distinct()
                    .collect(Collectors.toCollection(ArrayList::new)));
        }
        if (form.getSelectedUserIds() == null) {
            form.setSelectedUserIds(new ArrayList<>());
            return form;
        }
        form.setSelectedUserIds(form.getSelectedUserIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new)));
        return form;
    }

    private InternalVacancyLevelTwoChangeRequestForm ensureChangeRequestForm(
            InternalVacancyLevelTwoChangeRequestForm form,
            InternalVacancyLevelTwoWorkflowDetailView detail) {
        if (form == null) {
            return buildChangeRequestForm(detail);
        }
        if (form.getChangeReason() == null) {
            form.setChangeReason(detail.getTimeChangeReason());
        }
        return form;
    }

    private String resolveActorEmail(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new RecruitmentNotificationException("Authenticated user is required.");
        }
        return principal.getName().trim();
    }

    private Page<InternalVacancyLevelTwoCandidateSummaryView> loadCandidatePage(
            String search,
            int page,
            int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                resolvePageSize(size),
                Sort.by(Sort.Direction.DESC, "interviewDateTime")
                        .and(Sort.by(Sort.Direction.DESC, "scheduledAt")));
        return workflowService.getScheduledCandidatePage(search, pageable);
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
