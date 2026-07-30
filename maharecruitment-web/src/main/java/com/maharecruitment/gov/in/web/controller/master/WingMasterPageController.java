package com.maharecruitment.gov.in.web.controller.master;

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

import com.maharecruitment.gov.in.master.dto.WingMasterDto;
import com.maharecruitment.gov.in.master.service.WingMasterService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/master/wings")
public class WingMasterPageController {

    private final WingMasterService service;

    public WingMasterPageController(WingMasterService service) {
        this.service = service;
    }

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "true") boolean includeInactive,
            @RequestParam(defaultValue = "") String searchText,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.max(size, 1),
                Sort.by(Sort.Direction.ASC, "wingName"));
        Page<WingMasterDto> wings = service.search(includeInactive, searchText, pageable);

        model.addAttribute("wings", wings);
        model.addAttribute("includeInactive", includeInactive);
        model.addAttribute("searchText", searchText);
        return "master/wings/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        populateForm(model, WingMasterDto.builder().activeFlag("Y").build(), null);
        return "master/wings/form";
    }

    @GetMapping("/{wingId}/edit")
    public String editForm(@PathVariable Long wingId, Model model, RedirectAttributes redirectAttributes) {
        try {
            populateForm(model, service.getById(wingId), wingId);
            return "master/wings/form";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/master/wings";
        }
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("wingForm") WingMasterDto form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateForm(model, form, null);
            return "master/wings/form";
        }

        try {
            service.create(form);
            redirectAttributes.addFlashAttribute("successMessage", "Wing created successfully");
            return "redirect:/master/wings";
        } catch (RuntimeException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            populateForm(model, form, null);
            return "master/wings/form";
        }
    }

    @PostMapping("/{wingId}")
    public String update(
            @PathVariable Long wingId,
            @Valid @ModelAttribute("wingForm") WingMasterDto form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateForm(model, form, wingId);
            return "master/wings/form";
        }

        try {
            service.update(wingId, form);
            redirectAttributes.addFlashAttribute("successMessage", "Wing updated successfully");
            return "redirect:/master/wings";
        } catch (RuntimeException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            populateForm(model, form, wingId);
            return "master/wings/form";
        }
    }

    @PostMapping("/{wingId}/deactivate")
    public String deactivate(
            @PathVariable Long wingId,
            @RequestParam(defaultValue = "true") boolean includeInactive,
            @RequestParam(defaultValue = "") String searchText,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            RedirectAttributes redirectAttributes) {
        try {
            service.softDelete(wingId);
            redirectAttributes.addFlashAttribute("successMessage", "Wing deactivated successfully");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        addListRedirectAttributes(redirectAttributes, includeInactive, searchText, page, size);
        return "redirect:/master/wings";
    }

    @PostMapping("/{wingId}/restore")
    public String restore(
            @PathVariable Long wingId,
            @RequestParam(defaultValue = "true") boolean includeInactive,
            @RequestParam(defaultValue = "") String searchText,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            RedirectAttributes redirectAttributes) {
        try {
            service.restore(wingId);
            redirectAttributes.addFlashAttribute("successMessage", "Wing restored successfully");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        addListRedirectAttributes(redirectAttributes, includeInactive, searchText, page, size);
        return "redirect:/master/wings";
    }

    private void addListRedirectAttributes(
            RedirectAttributes redirectAttributes,
            boolean includeInactive,
            String searchText,
            int page,
            int size) {
        redirectAttributes.addAttribute("includeInactive", includeInactive);
        redirectAttributes.addAttribute("searchText", searchText);
        redirectAttributes.addAttribute("page", page);
        redirectAttributes.addAttribute("size", size);
    }

    private void populateForm(Model model, WingMasterDto form, Long wingId) {
        model.addAttribute("wingForm", form);
        model.addAttribute("wingId", wingId);
        model.addAttribute("isEdit", wingId != null);
    }
}
