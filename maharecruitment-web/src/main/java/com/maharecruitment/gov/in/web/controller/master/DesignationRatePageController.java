package com.maharecruitment.gov.in.web.controller.master;

import java.util.List;
import java.util.Map;
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

import com.maharecruitment.gov.in.master.dto.ManpowerDesignationMasterResponse;
import com.maharecruitment.gov.in.master.dto.ManpowerDesignationRateRequest;
import com.maharecruitment.gov.in.master.dto.ManpowerDesignationRateResponse;
import com.maharecruitment.gov.in.master.dto.ResourceLevelExperienceResponse;
import com.maharecruitment.gov.in.master.service.ManpowerDesignationMasterService;
import com.maharecruitment.gov.in.master.service.ManpowerDesignationRateService;
import com.maharecruitment.gov.in.master.service.ResourceLevelExperienceService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/master/designation-rates")
public class DesignationRatePageController {

    private final ManpowerDesignationRateService rateService;
    private final ManpowerDesignationMasterService designationService;
    private final ResourceLevelExperienceService resourceLevelService;

    public DesignationRatePageController(
            ManpowerDesignationRateService rateService,
            ManpowerDesignationMasterService designationService,
            ResourceLevelExperienceService resourceLevelService) {
        this.rateService = rateService;
        this.designationService = designationService;
        this.resourceLevelService = resourceLevelService;
    }

    @GetMapping
    public String list(
            @RequestParam(required = false) Long designationId,
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        Page<ManpowerDesignationRateResponse> rates = rateService.getAll(designationId, includeInactive, pageable);
        List<ManpowerDesignationMasterResponse> designations = getActiveDesignations();
        Map<Long, String> designationLabelMap = designations.stream().collect(Collectors.toMap(
                ManpowerDesignationMasterResponse::getDesignationId,
                d -> d.getCategory() + " - " + d.getDesignationName(),
                (first, second) -> first));

        model.addAttribute("rates", rates);
        model.addAttribute("designationId", designationId);
        model.addAttribute("includeInactive", includeInactive);
        model.addAttribute("availableDesignations", designations);
        model.addAttribute("designationLabelMap", designationLabelMap);
        return "master/designation-rates/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        ManpowerDesignationRateRequest form = new ManpowerDesignationRateRequest();
        populateForm(model, form, null);
        return "master/designation-rates/form";
    }

    @GetMapping("/{rateId}/edit")
    public String editForm(@PathVariable Long rateId, Model model, RedirectAttributes redirectAttributes) {
        try {
            ManpowerDesignationRateResponse existing = rateService.getById(rateId, true);
            ManpowerDesignationRateRequest form = new ManpowerDesignationRateRequest();
            form.setDesignationId(existing.getDesignationId());
            form.setLevelCode(existing.getLevelCode());
            form.setGrossMonthlyCtc(existing.getGrossMonthlyCtc());
            form.setEffectiveFrom(existing.getEffectiveFrom());
            form.setEffectiveTo(existing.getEffectiveTo());
            form.setActiveFlag(existing.getActiveFlag());

            populateForm(model, form, rateId);
            return "master/designation-rates/form";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/master/designation-rates";
        }
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("designationRateForm") ManpowerDesignationRateRequest form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateForm(model, form, null);
            return "master/designation-rates/form";
        }

        try {
            rateService.create(form);
            redirectAttributes.addFlashAttribute("successMessage", "Designation rate created successfully");
            return "redirect:/master/designation-rates";
        } catch (RuntimeException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            populateForm(model, form, null);
            return "master/designation-rates/form";
        }
    }

    @PostMapping("/{rateId}")
    public String update(
            @PathVariable Long rateId,
            @Valid @ModelAttribute("designationRateForm") ManpowerDesignationRateRequest form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateForm(model, form, rateId);
            return "master/designation-rates/form";
        }

        try {
            rateService.update(rateId, form);
            redirectAttributes.addFlashAttribute("successMessage", "Designation rate updated successfully");
            return "redirect:/master/designation-rates";
        } catch (RuntimeException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            populateForm(model, form, rateId);
            return "master/designation-rates/form";
        }
    }

    @PostMapping("/{rateId}/delete")
    public String delete(@PathVariable Long rateId, RedirectAttributes redirectAttributes) {
        try {
            rateService.softDelete(rateId);
            redirectAttributes.addFlashAttribute("successMessage", "Designation rate deleted successfully");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/master/designation-rates";
    }

    @PostMapping("/{rateId}/restore")
    public String restore(@PathVariable Long rateId, RedirectAttributes redirectAttributes) {
        try {
            rateService.restore(rateId);
            redirectAttributes.addFlashAttribute("successMessage", "Designation rate restored successfully");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/master/designation-rates?includeInactive=true";
    }

    private void populateForm(Model model, ManpowerDesignationRateRequest form, Long rateId) {
        model.addAttribute("designationRateForm", form);
        model.addAttribute("rateId", rateId);
        model.addAttribute("isEdit", rateId != null);
        model.addAttribute("availableDesignations", getActiveDesignations());
        model.addAttribute("availableLevels", getActiveLevels());
    }

    private List<ManpowerDesignationMasterResponse> getActiveDesignations() {
        return designationService.getAll(false, Pageable.unpaged()).getContent();
    }

    private List<ResourceLevelExperienceResponse> getActiveLevels() {
        return resourceLevelService.getAll(false, Pageable.unpaged()).getContent();
    }
}
