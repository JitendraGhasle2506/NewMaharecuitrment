package com.maharecruitment.gov.in.web.controller.agency;

import java.security.Principal;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.web.dto.agency.AgencyPreOnboardingForm;
import com.maharecruitment.gov.in.web.service.onboarding.CandidateIdentityValidationService;
import com.maharecruitment.gov.in.web.service.agency.AgencyOnboardingPageService;

@Controller
@RequestMapping("/agency/onboarding")
public class AgencyOnboardingPageController {

    private static final Logger log = LoggerFactory.getLogger(AgencyOnboardingPageController.class);

    private final AgencyOnboardingPageService onboardingPageService;
    private final CandidateIdentityValidationService candidateIdentityValidationService;

    public AgencyOnboardingPageController(
            AgencyOnboardingPageService onboardingPageService,
            CandidateIdentityValidationService candidateIdentityValidationService) {
        this.onboardingPageService = onboardingPageService;
        this.candidateIdentityValidationService = candidateIdentityValidationService;
    }

    @GetMapping
    public String onboardingReadyCandidates(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "search", required = false) String search,
            Principal principal,
            Model model) {
        String actorEmail = resolveActorEmail(principal);
        Pageable pageable = PageRequest.of(Math.max(0, page), size);
        model.addAttribute("onboardedEmployees", onboardingPageService.getOnboardedEmployees(actorEmail, search, pageable));
        model.addAttribute("currentStatus", "ACTIVE");
        model.addAttribute("search", search);
        model.addAttribute("pageTitle", "Onboarded Employees");
        model.addAttribute("pageSubtitle", "Agency-wise active onboarded employees.");
        return "agency/onboarding-list";
    }

    @GetMapping("/resigned")
    public String resignedEmployees(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "search", required = false) String search,
            Principal principal,
            Model model) {
        String actorEmail = resolveActorEmail(principal);
        Pageable pageable = PageRequest.of(Math.max(0, page), size);
        model.addAttribute("onboardedEmployees", onboardingPageService.getEmployeesByStatus(actorEmail, "RESIGNED", search, pageable));
        model.addAttribute("currentStatus", "RESIGNED");
        model.addAttribute("search", search);
        model.addAttribute("pageTitle", "Resigned Employees");
        model.addAttribute("pageSubtitle", "Agency employees who resigned and reopened their vacancy.");
        return "agency/onboarding-list";
    }

    @GetMapping("/pre/{recruitmentInterviewDetailId}")
    public String preOnboardingForm(
            @PathVariable Long recruitmentInterviewDetailId,
            Principal principal,
            Model model,
            RedirectAttributes redirectAttributes) {
        String actorEmail = resolveActorEmail(principal);

        try {
            if (!model.containsAttribute("preOnboardingForm")) {
                model.addAttribute(
                        "preOnboardingForm",
                        onboardingPageService.loadPreOnboardingForm(actorEmail, recruitmentInterviewDetailId));
            }
            return "agency/pre-onboarding-form";
        } catch (RecruitmentNotificationException ex) {
            log.warn(
                    "Unable to load pre-onboarding form. candidateId={}, actorEmail={}, reason={}",
                    recruitmentInterviewDetailId,
                    actorEmail,
                    ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/agency/selected-candidates";
        }
    }

    @PostMapping("/pre/{recruitmentInterviewDetailId}")
    public String savePreOnboarding(
            @PathVariable Long recruitmentInterviewDetailId,
            @Valid @ModelAttribute("preOnboardingForm") AgencyPreOnboardingForm form,
            BindingResult bindingResult,
            Principal principal,
            Model model,
            RedirectAttributes redirectAttributes) {
        String actorEmail = resolveActorEmail(principal);
        form.setRecruitmentInterviewDetailId(recruitmentInterviewDetailId);

        if (bindingResult.hasErrors()) {
            try {
                AgencyPreOnboardingForm referenceForm = onboardingPageService.loadPreOnboardingForm(
                        actorEmail,
                        recruitmentInterviewDetailId);
                mergeReadonlyFields(referenceForm, form);
                model.addAttribute("preOnboardingForm", form);
                return "agency/pre-onboarding-form";
            } catch (RecruitmentNotificationException ex) {
                redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
                return "redirect:/agency/selected-candidates";
            }
        }

        try {
            onboardingPageService.savePreOnboarding(actorEmail, recruitmentInterviewDetailId, form);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Pre-onboarding form submitted. Candidate is now available in onboarding.");
            return "redirect:/agency/onboarding";
        } catch (RecruitmentNotificationException ex) {
            log.warn(
                    "Unable to save pre-onboarding form. candidateId={}, actorEmail={}, reason={}",
                    recruitmentInterviewDetailId,
                    actorEmail,
                    ex.getMessage());
            try {
                AgencyPreOnboardingForm referenceForm = onboardingPageService.loadPreOnboardingForm(
                        actorEmail,
                        recruitmentInterviewDetailId);
                mergeReadonlyFields(referenceForm, form);
                model.addAttribute("errorMessage", ex.getMessage());
                model.addAttribute("preOnboardingForm", form);
                return "agency/pre-onboarding-form";
            } catch (RecruitmentNotificationException lockedEx) {
                log.warn(
                        "Unable to reload pre-onboarding form after save failure. candidateId={}, actorEmail={}, reason={}",
                        recruitmentInterviewDetailId,
                        actorEmail,
                        lockedEx.getMessage());
                redirectAttributes.addFlashAttribute("errorMessage", lockedEx.getMessage());
                return "redirect:/agency/selected-candidates";
            }
        }
    }

    @GetMapping("/pre/validate-aadhaar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> validateAadhaar(
            @RequestParam("aadhaar") String aadhaar,
            @RequestParam(name = "preOnboardingId", required = false) Long preOnboardingId,
            Principal principal) {
        String actorEmail = resolveActorEmail(principal);
        boolean duplicate = candidateIdentityValidationService.isAadhaarDuplicate(preOnboardingId, aadhaar);
        if (duplicate) {
            log.info(
                    "Aadhaar duplicate check failed. actorEmail={}, preOnboardingId={}",
                    actorEmail,
                    preOnboardingId);
        } else {
            log.debug(
                    "Aadhaar duplicate check passed. actorEmail={}, preOnboardingId={}",
                    actorEmail,
                    preOnboardingId);
        }

        return ResponseEntity.ok(Map.of(
                "duplicate", duplicate,
                "message", duplicate ? "Aadhaar number already exists in the system." : ""));
    }

    @GetMapping("/pre/validate-pan")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> validatePan(
            @RequestParam("pan") String pan,
            @RequestParam(name = "preOnboardingId", required = false) Long preOnboardingId,
            Principal principal) {
        String actorEmail = resolveActorEmail(principal);
        boolean duplicate = candidateIdentityValidationService.isPanDuplicate(preOnboardingId, pan);
        if (duplicate) {
            log.info("PAN duplicate check failed. actorEmail={}, preOnboardingId={}", actorEmail, preOnboardingId);
        } else {
            log.debug("PAN duplicate check passed. actorEmail={}, preOnboardingId={}", actorEmail, preOnboardingId);
        }

        return ResponseEntity.ok(Map.of(
                "duplicate", duplicate,
                "message", duplicate ? "PAN number already exists in the system." : ""));
    }

    @GetMapping("/pre/validate-email")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> validateEmail(
            @RequestParam("email") String email,
            @RequestParam(name = "preOnboardingId", required = false) Long preOnboardingId,
            Principal principal) {
        String actorEmail = resolveActorEmail(principal);
        boolean duplicate = candidateIdentityValidationService.isEmailDuplicate(preOnboardingId, email);
        if (duplicate) {
            log.info("Email duplicate check failed. actorEmail={}, preOnboardingId={}", actorEmail, preOnboardingId);
        } else {
            log.debug("Email duplicate check passed. actorEmail={}, preOnboardingId={}", actorEmail, preOnboardingId);
        }

        return ResponseEntity.ok(Map.of(
                "duplicate", duplicate,
                "message", duplicate ? "Email already exists in the system." : ""));
    }

    @GetMapping("/pre/validate-mobile")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> validateMobile(
            @RequestParam("mobile") String mobile,
            @RequestParam(name = "preOnboardingId", required = false) Long preOnboardingId,
            Principal principal) {
        String actorEmail = resolveActorEmail(principal);
        boolean duplicate = candidateIdentityValidationService.isMobileDuplicate(preOnboardingId, mobile);
        if (duplicate) {
            log.info("Mobile duplicate check failed. actorEmail={}, preOnboardingId={}", actorEmail, preOnboardingId);
        } else {
            log.debug("Mobile duplicate check passed. actorEmail={}, preOnboardingId={}", actorEmail, preOnboardingId);
        }

        return ResponseEntity.ok(Map.of(
                "duplicate", duplicate,
                "message", duplicate ? "Mobile number already exists in the system." : ""));
    }

    @PostMapping("/{employeeId}/resign")
    public String resignEmployee(
            @PathVariable Long employeeId,
            @RequestParam("resignationDate") java.time.LocalDate resignationDate,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        String actorEmail = resolveActorEmail(principal);
        try {
            onboardingPageService.markEmployeeResigned(actorEmail, employeeId, resignationDate);
            redirectAttributes.addFlashAttribute("successMessage", "Employee marked as resigned successfully.");
        } catch (RecruitmentNotificationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/agency/onboarding";
    }

    private void mergeReadonlyFields(AgencyPreOnboardingForm source, AgencyPreOnboardingForm target) {
        target.setPreOnboardingId(source.getPreOnboardingId());
        target.setRecruitmentInterviewDetailId(source.getRecruitmentInterviewDetailId());
        target.setRecruitmentNotificationId(source.getRecruitmentNotificationId());
        target.setRequestId(source.getRequestId());
        target.setProjectName(source.getProjectName());
        target.setDepartment(source.getDepartment());
        target.setSubDeptName(source.getSubDeptName());
        target.setDesignation(source.getDesignation());
        target.setLevelCode(source.getLevelCode());
        target.setAgencyName(source.getAgencyName());
        target.setMinExperienceYears(source.getMinExperienceYears());
        target.setExistingAadhaarFileName(source.getExistingAadhaarFileName());
        target.setExistingAadhaarFilePath(source.getExistingAadhaarFilePath());
        target.setExistingPanFileName(source.getExistingPanFileName());
        target.setExistingPanFilePath(source.getExistingPanFilePath());
        target.setExistingExperienceDocFileName(source.getExistingExperienceDocFileName());
        target.setExistingExperienceDocFilePath(source.getExistingExperienceDocFilePath());
        target.setExistingPhotoFileName(source.getExistingPhotoFileName());
        target.setExistingPhotoFilePath(source.getExistingPhotoFilePath());
    }

    private String resolveActorEmail(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new RecruitmentNotificationException("Authenticated user is required.");
        }
        return principal.getName().trim();
    }
}
