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

import com.maharecruitment.gov.in.master.dto.CellMasterDto;
import com.maharecruitment.gov.in.master.service.CellMasterService;
import com.maharecruitment.gov.in.master.service.WingMasterService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/master/cell")
public class CellMasterPageController {

    private final CellMasterService service;
    private final WingMasterService wingService;

    public CellMasterPageController(CellMasterService service, WingMasterService wingService) {
        this.service = service;
        this.wingService = wingService;
    }

    @GetMapping
    public String list(
            @RequestParam(required = false) Long wingId,
            @RequestParam(defaultValue = "true") boolean includeInactive,
            @RequestParam(defaultValue = "") String searchText,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.max(size, 1),
                Sort.by(Sort.Direction.ASC, "cellName"));
        Page<CellMasterDto> cells = service.search(wingId, includeInactive, searchText, pageable);

        model.addAttribute("cells", cells);
        model.addAttribute("wingId", wingId);
        model.addAttribute("availableWings", wingService.getAll(false));
        model.addAttribute("includeInactive", includeInactive);
        model.addAttribute("searchText", searchText);
        return "master/cell-master-list";
    }

    @GetMapping("/add")
    public String addForm(Model model) {
        populateForm(model, CellMasterDto.builder().activeFlag("Y").build(), null);
        return "master/cell-master-form";
    }

    @PostMapping("/save")
    public String save(
            @Valid @ModelAttribute("cellForm") CellMasterDto form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateForm(model, form, null);
            return "master/cell-master-form";
        }

        try {
            service.create(form);
            redirectAttributes.addFlashAttribute("successMessage", "Cell created successfully");
            return "redirect:/master/cell";
        } catch (RuntimeException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            populateForm(model, form, null);
            return "master/cell-master-form";
        }
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable("id") Long cellId, Model model, RedirectAttributes redirectAttributes) {
        try {
            populateForm(model, service.getById(cellId), cellId);
            return "master/cell-master-form";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/master/cell";
        }
    }

    @PostMapping("/update/{id}")
    public String update(
            @PathVariable("id") Long cellId,
            @Valid @ModelAttribute("cellForm") CellMasterDto form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateForm(model, form, cellId);
            return "master/cell-master-form";
        }

        try {
            service.update(cellId, form);
            redirectAttributes.addFlashAttribute("successMessage", "Cell updated successfully");
            return "redirect:/master/cell";
        } catch (RuntimeException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            populateForm(model, form, cellId);
            return "master/cell-master-form";
        }
    }

    @GetMapping("/status/{id}")
    public String toggleStatus(
            @PathVariable("id") Long cellId,
            @RequestParam(required = false) Long wingId,
            @RequestParam(defaultValue = "true") boolean includeInactive,
            @RequestParam(defaultValue = "") String searchText,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            RedirectAttributes redirectAttributes) {
        try {
            service.toggleStatus(cellId);
            redirectAttributes.addFlashAttribute("successMessage", "Cell status updated successfully");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        redirectAttributes.addAttribute("wingId", wingId);
        redirectAttributes.addAttribute("includeInactive", includeInactive);
        redirectAttributes.addAttribute("searchText", searchText);
        redirectAttributes.addAttribute("page", page);
        redirectAttributes.addAttribute("size", size);
        return "redirect:/master/cell";
    }

    private void populateForm(Model model, CellMasterDto form, Long cellId) {
        model.addAttribute("cellForm", form);
        model.addAttribute("cellId", cellId);
        model.addAttribute("isEdit", cellId != null);
        model.addAttribute("availableWings", wingService.getAll(false));
    }
}
