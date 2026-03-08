package com.maharecruitment.gov.in.web.controller.master;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

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

import com.maharecruitment.gov.in.master.dto.ManpowerDesignationMasterRequest;
import com.maharecruitment.gov.in.master.dto.ManpowerDesignationMasterResponse;
import com.maharecruitment.gov.in.master.dto.ResourceLevelExperienceResponse;
import com.maharecruitment.gov.in.master.service.ManpowerDesignationMasterService;
import com.maharecruitment.gov.in.master.service.ResourceLevelExperienceService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/master/designations")
public class DesignationMasterPageController {

    private final ManpowerDesignationMasterService designationService;
    private final ResourceLevelExperienceService resourceLevelService;

    public DesignationMasterPageController(
            ManpowerDesignationMasterService designationService,
            ResourceLevelExperienceService resourceLevelService) {
        this.designationService = designationService;
        this.resourceLevelService = resourceLevelService;
    }

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        Page<ManpowerDesignationMasterResponse> designations = designationService.getAll(includeInactive, pageable);

        model.addAttribute("designations", designations);
        model.addAttribute("includeInactive", includeInactive);
        return "master/designations/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        ManpowerDesignationMasterRequest form = new ManpowerDesignationMasterRequest();
        populateForm(model, form, null);
        return "master/designations/form";
    }

    @GetMapping("/{designationId}/edit")
    public String editForm(@PathVariable Long designationId, Model model, RedirectAttributes redirectAttributes) {
        try {
            ManpowerDesignationMasterResponse existing = designationService.getById(designationId, true);
            ManpowerDesignationMasterRequest form = new ManpowerDesignationMasterRequest();
            form.setCategory(existing.getCategory());
            form.setDesignationName(existing.getDesignationName());
            form.setRoleName(existing.getRoleName());
            form.setEducationalQualification(existing.getEducationalQualification());
            form.setCertification(existing.getCertification());
            form.setActiveFlag(existing.getActiveFlag());
            Set<Long> levelIds = existing.getLevels().stream()
                    .map(level -> level.getLevelId())
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            form.setLevelIds(levelIds);

            populateForm(model, form, designationId);
            return "master/designations/form";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/master/designations";
        }
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("designationForm") ManpowerDesignationMasterRequest form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateForm(model, form, null);
            return "master/designations/form";
        }

        try {
            designationService.create(form);
            redirectAttributes.addFlashAttribute("successMessage", "Designation created successfully");
            return "redirect:/master/designations";
        } catch (RuntimeException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            populateForm(model, form, null);
            return "master/designations/form";
        }
    }

    @PostMapping("/{designationId}")
    public String update(
            @PathVariable Long designationId,
            @Valid @ModelAttribute("designationForm") ManpowerDesignationMasterRequest form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateForm(model, form, designationId);
            return "master/designations/form";
        }

        try {
            designationService.update(designationId, form);
            redirectAttributes.addFlashAttribute("successMessage", "Designation updated successfully");
            return "redirect:/master/designations";
        } catch (RuntimeException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            populateForm(model, form, designationId);
            return "master/designations/form";
        }
    }

    @PostMapping("/{designationId}/delete")
    public String delete(@PathVariable Long designationId, RedirectAttributes redirectAttributes) {
        try {
            designationService.softDelete(designationId);
            redirectAttributes.addFlashAttribute("successMessage", "Designation deleted successfully");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/master/designations";
    }

    @PostMapping("/{designationId}/restore")
    public String restore(@PathVariable Long designationId, RedirectAttributes redirectAttributes) {
        try {
            designationService.restore(designationId);
            redirectAttributes.addFlashAttribute("successMessage", "Designation restored successfully");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/master/designations?includeInactive=true";
    }

    private void populateForm(Model model, ManpowerDesignationMasterRequest form, Long designationId) {
        model.addAttribute("designationForm", form);
        model.addAttribute("designationId", designationId);
        model.addAttribute("isEdit", designationId != null);
        model.addAttribute("availableLevels", getActiveLevels());
    }

    private java.util.List<ResourceLevelExperienceResponse> getActiveLevels() {
        return resourceLevelService.getAll(false, Pageable.unpaged()).getContent();
    }
}
