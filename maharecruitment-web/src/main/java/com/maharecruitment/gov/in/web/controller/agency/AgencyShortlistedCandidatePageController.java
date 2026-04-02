package com.maharecruitment.gov.in.web.controller.agency;

import java.security.Principal;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.recruitment.service.model.AgencyShortlistedCandidateProjectView;
import com.maharecruitment.gov.in.recruitment.service.model.AgencyShortlistedCandidateView;
import com.maharecruitment.gov.in.web.dto.agency.AgencyInterviewScheduleForm;
import com.maharecruitment.gov.in.web.service.agency.AgencyRecruitmentNotificationPageService;

@Controller
@RequestMapping("/agency/shortlisted-candidates")
public class AgencyShortlistedCandidatePageController {

    private final AgencyRecruitmentNotificationPageService pageService;

    public AgencyShortlistedCandidatePageController(AgencyRecruitmentNotificationPageService pageService) {
        this.pageService = pageService;
    }

    @GetMapping
    public String shortlistedCandidateProjects(
            @RequestParam(name = "open", required = false) Long openRecruitmentNotificationId,
            Principal principal,
            Model model) {
        String actorEmail = resolveActorEmail(principal);
        List<AgencyShortlistedCandidateProjectView> shortlistedProjects = pageService
                .getShortlistedCandidateProjects(actorEmail);
        List<AgencyShortlistedCandidateView> expandedShortlistedCandidates = openRecruitmentNotificationId == null
                ? List.of()
                : pageService.getShortlistedCandidates(actorEmail, openRecruitmentNotificationId, Pageable.unpaged())
                        .getContent();

        model.addAttribute("shortlistedProjects", shortlistedProjects);
        model.addAttribute("expandedShortlistedCandidates", expandedShortlistedCandidates);
        model.addAttribute("expandedRecruitmentNotificationId", openRecruitmentNotificationId);
        return "agency/shortlisted-candidate-project-list";
    }

    @GetMapping("/{recruitmentNotificationId}")
    public String shortlistedCandidatesByProject(
            @PathVariable Long recruitmentNotificationId) {
        return "redirect:/agency/shortlisted-candidates?open=" + recruitmentNotificationId;
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
            redirectAttributes.addFlashAttribute("successMessage", "Interview details saved successfully.");
        } catch (RecruitmentNotificationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/agency/shortlisted-candidates?open=" + recruitmentNotificationId;
    }

    private String resolveActorEmail(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new RecruitmentNotificationException("Authenticated user is required.");
        }
        return principal.getName().trim();
    }
}
