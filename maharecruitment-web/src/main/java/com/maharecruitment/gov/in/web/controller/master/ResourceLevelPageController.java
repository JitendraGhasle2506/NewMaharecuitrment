package com.maharecruitment.gov.in.web.controller.master;

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

import com.maharecruitment.gov.in.master.dto.ResourceLevelExperienceRequest;
import com.maharecruitment.gov.in.master.dto.ResourceLevelExperienceResponse;
import com.maharecruitment.gov.in.master.service.ResourceLevelExperienceService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/master/resource-levels")
public class ResourceLevelPageController {

    private final ResourceLevelExperienceService service;

    public ResourceLevelPageController(ResourceLevelExperienceService service) {
        this.service = service;
    }

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        Page<ResourceLevelExperienceResponse> levels = service.getAll(includeInactive, pageable);

        model.addAttribute("levels", levels);
        model.addAttribute("includeInactive", includeInactive);
        return "master/resource-levels/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        populateForm(model, new ResourceLevelExperienceRequest(), null);
        return "master/resource-levels/form";
    }

    @GetMapping("/{levelId}/edit")
    public String editForm(@PathVariable Long levelId, Model model, RedirectAttributes redirectAttributes) {
        try {
            ResourceLevelExperienceResponse existing = service.getById(levelId, true);
            ResourceLevelExperienceRequest form = new ResourceLevelExperienceRequest();
            form.setLevelCode(existing.getLevelCode());
            form.setLevelName(existing.getLevelName());
            form.setMinExperience(existing.getMinExperience());
            form.setMaxExperience(existing.getMaxExperience());
            form.setActiveFlag(existing.getActiveFlag());

            populateForm(model, form, levelId);
            return "master/resource-levels/form";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/master/resource-levels";
        }
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("resourceLevelForm") ResourceLevelExperienceRequest form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateForm(model, form, null);
            return "master/resource-levels/form";
        }

        try {
            service.create(form);
            redirectAttributes.addFlashAttribute("successMessage", "Resource level created successfully");
            return "redirect:/master/resource-levels";
        } catch (RuntimeException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            populateForm(model, form, null);
            return "master/resource-levels/form";
        }
    }

    @PostMapping("/{levelId}")
    public String update(
            @PathVariable Long levelId,
            @Valid @ModelAttribute("resourceLevelForm") ResourceLevelExperienceRequest form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateForm(model, form, levelId);
            return "master/resource-levels/form";
        }

        try {
            service.update(levelId, form);
            redirectAttributes.addFlashAttribute("successMessage", "Resource level updated successfully");
            return "redirect:/master/resource-levels";
        } catch (RuntimeException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            populateForm(model, form, levelId);
            return "master/resource-levels/form";
        }
    }

    @PostMapping("/{levelId}/delete")
    public String delete(@PathVariable Long levelId, RedirectAttributes redirectAttributes) {
        try {
            service.softDelete(levelId);
            redirectAttributes.addFlashAttribute("successMessage", "Resource level deleted successfully");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/master/resource-levels";
    }

    @PostMapping("/{levelId}/restore")
    public String restore(@PathVariable Long levelId, RedirectAttributes redirectAttributes) {
        try {
            service.restore(levelId);
            redirectAttributes.addFlashAttribute("successMessage", "Resource level restored successfully");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/master/resource-levels?includeInactive=true";
    }

    private void populateForm(Model model, ResourceLevelExperienceRequest form, Long levelId) {
        model.addAttribute("resourceLevelForm", form);
        model.addAttribute("levelId", levelId);
        model.addAttribute("isEdit", levelId != null);
    }
}
