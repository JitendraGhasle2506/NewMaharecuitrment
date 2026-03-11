package com.maharecruitment.gov.in.web.controller.agency;

import java.security.Principal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.web.service.agency.AgencyRecruitmentNotificationPageService;

@Controller
@RequestMapping("/agency/recruitment-notifications")
public class AgencyRecruitmentNotificationPageController {

    private static final Logger log = LoggerFactory.getLogger(AgencyRecruitmentNotificationPageController.class);

    private final AgencyRecruitmentNotificationPageService pageService;

    public AgencyRecruitmentNotificationPageController(AgencyRecruitmentNotificationPageService pageService) {
        this.pageService = pageService;
    }

    @GetMapping
    public String listNotifications(
            Principal principal,
            Model model) {
        String actorEmail = resolveActorEmail(principal);
        model.addAttribute("notifications", pageService.getVisibleNotifications(actorEmail));
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

    private String resolveActorEmail(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new RecruitmentNotificationException("Authenticated user is required.");
        }
        return principal.getName().trim();
    }
}
