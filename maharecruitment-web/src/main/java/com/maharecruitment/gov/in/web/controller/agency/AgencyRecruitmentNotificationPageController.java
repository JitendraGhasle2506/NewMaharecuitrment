package com.maharecruitment.gov.in.web.controller.agency;

import java.security.Principal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.recruitment.service.model.AgencyVisibleNotificationListMetricsView;
import com.maharecruitment.gov.in.recruitment.service.model.AgencyVisibleNotificationView;
import com.maharecruitment.gov.in.web.dto.agency.AgencyCandidateBatchForm;
import com.maharecruitment.gov.in.web.dto.agency.AgencyCandidateRowForm;
import com.maharecruitment.gov.in.web.dto.agency.AgencyInterviewScheduleForm;
import com.maharecruitment.gov.in.web.service.agency.AgencyRecruitmentNotificationPageService;

@Controller
@RequestMapping("/agency/recruitment-notifications")
public class AgencyRecruitmentNotificationPageController {

    private static final Logger log = LoggerFactory.getLogger(AgencyRecruitmentNotificationPageController.class);
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;

    private final AgencyRecruitmentNotificationPageService pageService;

    public AgencyRecruitmentNotificationPageController(AgencyRecruitmentNotificationPageService pageService) {
        this.pageService = pageService;
    }

    @GetMapping
    public String listNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(name = "search", required = false) String search,
            Principal principal,
            Model model) {
        String actorEmail = resolveActorEmail(principal);
        int resolvedPage = Math.max(page, 0);
        int resolvedSize = resolvePageSize(size);
        String normalizedSearch = normalizeSearch(search);

        Page<AgencyVisibleNotificationView> notificationPage = loadNotificationPage(
                actorEmail,
                normalizedSearch,
                resolvedPage,
                resolvedSize);
        if (notificationPage.getTotalPages() > 0 && resolvedPage >= notificationPage.getTotalPages()) {
            notificationPage = loadNotificationPage(
                    actorEmail,
                    normalizedSearch,
                    notificationPage.getTotalPages() - 1,
                    resolvedSize);
        }

        AgencyVisibleNotificationListMetricsView notificationMetrics = pageService
                .getVisibleNotificationMetrics(actorEmail, normalizedSearch);

        model.addAttribute("notifications", notificationPage.getContent());
        model.addAttribute("notificationPage", notificationPage);
        model.addAttribute("notificationMetrics", notificationMetrics);
        model.addAttribute("searchTerm", normalizedSearch == null ? "" : normalizedSearch);
        model.addAttribute("pageSize", notificationPage.getSize());
        return "agency/recruitment-notification-list";
    }

    @GetMapping("/{recruitmentNotificationId}")
    public String notificationDetail(
            @PathVariable Long recruitmentNotificationId,
            Principal principal,
            Model model,
            RedirectAttributes redirectAttributes) {
        String actorEmail = resolveActorEmail(principal);
        try {
            model.addAttribute("detail", pageService.getNotificationDetail(actorEmail, recruitmentNotificationId));
            model.addAttribute("submittedCandidates", pageService.getSubmittedCandidates(actorEmail, recruitmentNotificationId));
            if (!model.containsAttribute("candidateForm")) {
                model.addAttribute("candidateForm", buildDefaultCandidateForm());
            }
            return "agency/recruitment-notification-detail";
        } catch (RecruitmentNotificationException ex) {
            log.warn("Unable to load notification detail. notificationId={}, actorEmail={}, reason={}",
                    recruitmentNotificationId, actorEmail, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/agency/recruitment-notifications";
        }
    }

    @PostMapping("/{recruitmentNotificationId}/read")
    public String markNotificationAsRead(
            @PathVariable Long recruitmentNotificationId,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        String actorEmail = resolveActorEmail(principal);
        try {
            pageService.markAsRead(actorEmail, recruitmentNotificationId);
            redirectAttributes.addFlashAttribute("successMessage", "Notification marked as read.");
        } catch (RecruitmentNotificationException ex) {
            log.warn(
                    "Unable to mark agency notification as read. notificationId={}, actorEmail={}, reason={}",
                    recruitmentNotificationId,
                    actorEmail,
                    ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/agency/recruitment-notifications/" + recruitmentNotificationId;
    }

    @PostMapping("/{recruitmentNotificationId}/respond")
    public String submitResponse(
            @PathVariable Long recruitmentNotificationId,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        String actorEmail = resolveActorEmail(principal);
        try {
            pageService.submitResponse(actorEmail, recruitmentNotificationId);
            redirectAttributes.addFlashAttribute("successMessage", "Response submitted successfully.");
        } catch (RecruitmentNotificationException ex) {
            log.warn(
                    "Unable to submit agency response. notificationId={}, actorEmail={}, reason={}",
                    recruitmentNotificationId,
                    actorEmail,
                    ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/agency/recruitment-notifications/" + recruitmentNotificationId;
    }

    @PostMapping("/{recruitmentNotificationId}/candidates")
    public String submitCandidates(
            @PathVariable Long recruitmentNotificationId,
            @ModelAttribute("candidateForm") AgencyCandidateBatchForm candidateForm,
            BindingResult bindingResult,
            Principal principal,
            Model model,
            RedirectAttributes redirectAttributes) {
        String actorEmail = resolveActorEmail(principal);

        if (bindingResult.hasErrors()) {
            try {
                model.addAttribute("detail", pageService.getNotificationDetail(actorEmail, recruitmentNotificationId));
                model.addAttribute("submittedCandidates",
                        pageService.getSubmittedCandidates(actorEmail, recruitmentNotificationId));
                model.addAttribute("errorMessage", "Please correct the candidate form values and submit again.");
                return "agency/recruitment-notification-detail";
            } catch (RecruitmentNotificationException ex) {
                redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
                return "redirect:/agency/recruitment-notifications";
            }
        }

        try {
            pageService.submitCandidates(actorEmail, recruitmentNotificationId, candidateForm);
            redirectAttributes.addFlashAttribute("successMessage", "Candidate details submitted successfully.");
        } catch (RecruitmentNotificationException ex) {
            log.warn("Unable to submit candidates. notificationId={}, actorEmail={}, reason={}",
                    recruitmentNotificationId,
                    actorEmail,
                    ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/agency/recruitment-notifications/" + recruitmentNotificationId;
    }

    @PostMapping("/{recruitmentNotificationId}/candidates/{recruitmentInterviewDetailId}/schedule-interview")
    public String scheduleInterview(
            @PathVariable Long recruitmentNotificationId,
            @PathVariable Long recruitmentInterviewDetailId,
            @ModelAttribute AgencyInterviewScheduleForm interviewScheduleForm,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        String actorEmail = resolveActorEmail(principal);

        try {
            pageService.scheduleInterview(
                    actorEmail,
                    recruitmentNotificationId,
                    recruitmentInterviewDetailId,
                    interviewScheduleForm);
            redirectAttributes.addFlashAttribute("successMessage", "Interview details submitted successfully.");
        } catch (RecruitmentNotificationException ex) {
            log.warn(
                    "Unable to submit interview schedule. notificationId={}, candidateId={}, actorEmail={}, reason={}",
                    recruitmentNotificationId,
                    recruitmentInterviewDetailId,
                    actorEmail,
                    ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/agency/recruitment-notifications/" + recruitmentNotificationId;
    }

    @PostMapping("/{recruitmentNotificationId}/candidates/{recruitmentInterviewDetailId}/withdraw")
    public String withdrawCandidate(
            @PathVariable Long recruitmentNotificationId,
            @PathVariable Long recruitmentInterviewDetailId,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        String actorEmail = resolveActorEmail(principal);

        try {
            pageService.withdrawCandidate(actorEmail, recruitmentNotificationId, recruitmentInterviewDetailId);
            redirectAttributes.addFlashAttribute("successMessage", "Candidate withdrawn successfully.");
        } catch (RecruitmentNotificationException ex) {
            log.warn(
                    "Unable to withdraw candidate. notificationId={}, candidateId={}, actorEmail={}, reason={}",
                    recruitmentNotificationId,
                    recruitmentInterviewDetailId,
                    actorEmail,
                    ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/agency/recruitment-notifications/" + recruitmentNotificationId;
    }

    private String resolveActorEmail(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new RecruitmentNotificationException("Authenticated user is required.");
        }
        return principal.getName().trim();
    }

    private Page<AgencyVisibleNotificationView> loadNotificationPage(
            String actorEmail,
            String normalizedSearch,
            int page,
            int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), resolvePageSize(size));
        return pageService.getVisibleNotifications(actorEmail, normalizedSearch, pageable);
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

    private AgencyCandidateBatchForm buildDefaultCandidateForm() {
        AgencyCandidateBatchForm candidateBatchForm = new AgencyCandidateBatchForm();
        candidateBatchForm.getCandidates().add(new AgencyCandidateRowForm());
        return candidateBatchForm;
    }
}
