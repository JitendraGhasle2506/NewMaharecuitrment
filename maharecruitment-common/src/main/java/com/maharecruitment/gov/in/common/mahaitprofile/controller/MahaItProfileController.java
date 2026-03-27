package com.maharecruitment.gov.in.common.mahaitprofile.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

import com.maharecruitment.gov.in.common.mahaitprofile.dto.MahaItProfileAuditResponse;
import com.maharecruitment.gov.in.common.mahaitprofile.dto.MahaItProfileRequest;
import com.maharecruitment.gov.in.common.mahaitprofile.dto.MahaItProfileResponse;
import com.maharecruitment.gov.in.common.mahaitprofile.service.MahaItProfileService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/common/mahait-profile")
public class MahaItProfileController {

    private final MahaItProfileService profileService;

    public MahaItProfileController(MahaItProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1),
                Sort.by(Sort.Direction.DESC, "updatedDate"));
        Page<MahaItProfileResponse> profiles = profileService.getAll(pageable);
        model.addAttribute("profiles", profiles);
        return "common/mahait-profile/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        MahaItProfileRequest form = new MahaItProfileRequest();
        populateForm(model, form, null, null, List.of());
        return "common/mahait-profile/form";
    }

    @GetMapping("/{mahaitProfileId}/edit")
    public String editForm(
            @PathVariable Long mahaitProfileId,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            MahaItProfileResponse response = profileService.getById(mahaitProfileId);
            populateForm(
                    model,
                    toForm(response),
                    response.getMahaItProfileId(),
                    response,
                    profileService.getAuditTrail(mahaitProfileId));
            return "common/mahait-profile/form";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/common/mahait-profile";
        }
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("profileForm") MahaItProfileRequest form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateForm(model, form, null, null, List.of());
            return "common/mahait-profile/form";
        }

        try {
            profileService.create(form);
            redirectAttributes.addFlashAttribute("successMessage", "MahaIT profile saved successfully");
            return "redirect:/common/mahait-profile";
        } catch (RuntimeException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            populateForm(model, form, null, null, List.of());
            return "common/mahait-profile/form";
        }
    }

    @PostMapping("/{mahaitProfileId}")
    public String update(
            @PathVariable Long mahaitProfileId,
            @Valid @ModelAttribute("profileForm") MahaItProfileRequest form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateForm(
                    model,
                    form,
                    mahaitProfileId,
                    loadProfileSafely(mahaitProfileId),
                    profileService.getAuditTrail(mahaitProfileId));
            return "common/mahait-profile/form";
        }

        try {
            profileService.update(mahaitProfileId, form);
            redirectAttributes.addFlashAttribute("successMessage", "MahaIT profile updated successfully");
            return "redirect:/common/mahait-profile";
        } catch (RuntimeException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            populateForm(
                    model,
                    form,
                    mahaitProfileId,
                    loadProfileSafely(mahaitProfileId),
                    profileService.getAuditTrail(mahaitProfileId));
            return "common/mahait-profile/form";
        }
    }

    private void populateForm(
            Model model,
            MahaItProfileRequest form,
            Long mahaitProfileId,
            MahaItProfileResponse response,
            List<MahaItProfileAuditResponse> auditLogs) {
        model.addAttribute("profileForm", form);
        model.addAttribute("profileId", mahaitProfileId);
        model.addAttribute("isEdit", mahaitProfileId != null);
        model.addAttribute("profile", response);
        model.addAttribute("auditLogs", auditLogs);
    }

    private MahaItProfileRequest toForm(MahaItProfileResponse response) {
        MahaItProfileRequest form = new MahaItProfileRequest();
        form.setMahaItProfileId(response.getMahaItProfileId());
        form.setProfileName(response.getProfileName());
        form.setCompanyName(response.getCompanyName());
        form.setCompanyAddress(response.getCompanyAddress());
        form.setCinNumber(response.getCinNumber());
        form.setPanNumber(response.getPanNumber());
        form.setGstNumber(response.getGstNumber());
        form.setBankName(response.getBankName());
        form.setBranchName(response.getBranchName());
        form.setAccountHolderName(response.getAccountHolderName());
        form.setAccountNumber(response.getAccountNumber());
        form.setIfscCode(response.getIfscCode());
        form.setActive(response.getActive());
        return form;
    }

    private MahaItProfileResponse loadProfileSafely(Long mahaitProfileId) {
        try {
            return profileService.getById(mahaitProfileId);
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
