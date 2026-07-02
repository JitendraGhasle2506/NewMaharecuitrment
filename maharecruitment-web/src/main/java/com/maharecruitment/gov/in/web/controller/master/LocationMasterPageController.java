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

import com.maharecruitment.gov.in.master.dto.LocationMasterDto;
import com.maharecruitment.gov.in.master.service.LocationMasterService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/master/locations")
public class LocationMasterPageController {

    private final LocationMasterService service;

    public LocationMasterPageController(LocationMasterService service) {
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
                Sort.by(Sort.Direction.ASC, "locationName"));
        Page<LocationMasterDto> locations = service.search(includeInactive, searchText, pageable);

        model.addAttribute("locations", locations);
        model.addAttribute("includeInactive", includeInactive);
        model.addAttribute("searchText", searchText);
        return "master/locations/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        populateForm(model, LocationMasterDto.builder().activeFlag("Y").build(), null);
        return "master/locations/form";
    }

    @GetMapping("/{locationId}/edit")
    public String editForm(@PathVariable Long locationId, Model model, RedirectAttributes redirectAttributes) {
        try {
            populateForm(model, service.getById(locationId), locationId);
            return "master/locations/form";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/master/locations";
        }
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("locationForm") LocationMasterDto form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateForm(model, form, null);
            return "master/locations/form";
        }

        try {
            service.create(form);
            redirectAttributes.addFlashAttribute("successMessage", "Location created successfully");
            return "redirect:/master/locations";
        } catch (RuntimeException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            populateForm(model, form, null);
            return "master/locations/form";
        }
    }

    @PostMapping("/{locationId}")
    public String update(
            @PathVariable Long locationId,
            @Valid @ModelAttribute("locationForm") LocationMasterDto form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateForm(model, form, locationId);
            return "master/locations/form";
        }

        try {
            service.update(locationId, form);
            redirectAttributes.addFlashAttribute("successMessage", "Location updated successfully");
            return "redirect:/master/locations";
        } catch (RuntimeException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            populateForm(model, form, locationId);
            return "master/locations/form";
        }
    }

    @PostMapping("/{locationId}/deactivate")
    public String deactivate(
            @PathVariable Long locationId,
            @RequestParam(defaultValue = "true") boolean includeInactive,
            @RequestParam(defaultValue = "") String searchText,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            RedirectAttributes redirectAttributes) {
        try {
            service.deactivate(locationId);
            redirectAttributes.addFlashAttribute("successMessage", "Location deactivated successfully");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        addListRedirectAttributes(redirectAttributes, includeInactive, searchText, page, size);
        return "redirect:/master/locations";
    }

    @PostMapping("/{locationId}/activate")
    public String activate(
            @PathVariable Long locationId,
            @RequestParam(defaultValue = "true") boolean includeInactive,
            @RequestParam(defaultValue = "") String searchText,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            RedirectAttributes redirectAttributes) {
        try {
            service.activate(locationId);
            redirectAttributes.addFlashAttribute("successMessage", "Location activated successfully");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        addListRedirectAttributes(redirectAttributes, includeInactive, searchText, page, size);
        return "redirect:/master/locations";
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

    private void populateForm(Model model, LocationMasterDto form, Long locationId) {
        model.addAttribute("locationForm", form);
        model.addAttribute("locationId", locationId);
        model.addAttribute("isEdit", locationId != null);
    }
}
