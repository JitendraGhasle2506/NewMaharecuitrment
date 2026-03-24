package com.maharecruitment.gov.in.web.controller.hr;

import java.security.Principal;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.maharecruitment.gov.in.web.dto.agency.AgencyPreOnboardingForm;
import com.maharecruitment.gov.in.web.service.agency.model.AgencyOnboardingCandidateView;
import com.maharecruitment.gov.in.web.service.hr.HROnboardingPageService;

@Controller
@RequestMapping("/hr/onboarding")
@PreAuthorize("hasAuthority('                                     ')")
public class HROnboardingPageController {

    private final HROnboardingPageService hrOnboardingPageService;

    public HROnboardingPageController(HROnboardingPageService hrOnboardingPageService) {
        this.hrOnboardingPageService = hrOnboardingPageService;
    }

    @GetMapping
    public String onboardingList(Model model) {
        List<AgencyOnboardingCandidateView> candidates = hrOnboardingPageService.getPendingHROnboardingCandidates();
        model.addAttribute("candidates", candidates);
        return "hr/onboarding-list";
    }

    @GetMapping("/process/{id}")
    public String onboardingForm(@PathVariable("id") Long preOnboardingId, Model model) {
        AgencyPreOnboardingForm form = hrOnboardingPageService.loadOnboardingForm(preOnboardingId);
        model.addAttribute("preOnboardingForm", form);
        return "agency/pre-onboarding-form"; // REUSING THE SAME FORM
    }

    @PostMapping("/process/{id}")
    public String saveOnboarding(
            @PathVariable("id") Long preOnboardingId,
            @ModelAttribute("preOnboardingForm") AgencyPreOnboardingForm form,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            hrOnboardingPageService.saveOnboarding(preOnboardingId, form, principal.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Candidate onboarded successfully.");
            return "redirect:/hr/onboarding";
        } catch (Exception e) {
            form.setHrFlow(true);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/hr/onboarding/process/" + preOnboardingId;
        }
    }
}
