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

import com.maharecruitment.gov.in.master.dto.CommissionRateAuditLogResponse;
import com.maharecruitment.gov.in.master.dto.CommissionRateRequest;
import com.maharecruitment.gov.in.master.dto.CommissionRateResponse;
import com.maharecruitment.gov.in.master.entity.CommissionCode;
import com.maharecruitment.gov.in.master.service.CommissionRateMasterService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/master/commission-rates")
public class CommissionRatePageController {

    private final CommissionRateMasterService service;

    public CommissionRatePageController(CommissionRateMasterService service) {
        this.service = service;
    }

    @GetMapping
    public String list(
            @RequestParam(required = false) CommissionCode commissionCode,
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        model.addAttribute("rates", service.getAll(commissionCode, includeInactive, pageable));
        model.addAttribute("commissionCode", commissionCode);
        model.addAttribute("includeInactive", includeInactive);
        model.addAttribute("commissionCodes", CommissionCode.values());
        return "master/commission-rates/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        CommissionRateRequest form = new CommissionRateRequest();
        form.setActiveFlag("Y");
        populateForm(model, form, null);
        return "master/commission-rates/form";
    }

    @GetMapping("/{commissionRateId}/edit")
    public String editForm(
            @PathVariable Long commissionRateId,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            CommissionRateResponse existing = service.getById(commissionRateId, true);
            CommissionRateRequest form = new CommissionRateRequest();
            form.setCommissionCode(existing.getCommissionCode());
            form.setCommissionPercentage(existing.getCommissionPercentage());
            form.setEffectiveDate(existing.getEffectiveDate());
            form.setActiveFlag(existing.getActiveFlag());
            populateForm(model, form, commissionRateId);
            return "master/commission-rates/form";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/master/commission-rates";
        }
    }

    @GetMapping("/{commissionRateId}/logs")
    public String logs(
            @PathVariable Long commissionRateId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
            CommissionRateResponse rate = service.getById(commissionRateId, true);
            Page<CommissionRateAuditLogResponse> logs = service.getAuditLogs(commissionRateId, pageable);
            model.addAttribute("rate", rate);
            model.addAttribute("logs", logs);
            return "master/commission-rates/logs";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/master/commission-rates";
        }
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("commissionRateForm") CommissionRateRequest form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateForm(model, form, null);
            return "master/commission-rates/form";
        }

        try {
            service.create(form);
            redirectAttributes.addFlashAttribute("successMessage", "Commission rate created successfully");
            return "redirect:/master/commission-rates";
        } catch (RuntimeException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            populateForm(model, form, null);
            return "master/commission-rates/form";
        }
    }

    @PostMapping("/{commissionRateId}")
    public String update(
            @PathVariable Long commissionRateId,
            @Valid @ModelAttribute("commissionRateForm") CommissionRateRequest form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateForm(model, form, commissionRateId);
            return "master/commission-rates/form";
        }

        try {
            service.update(commissionRateId, form);
            redirectAttributes.addFlashAttribute("successMessage", "Commission rate updated successfully");
            return "redirect:/master/commission-rates";
        } catch (RuntimeException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            populateForm(model, form, commissionRateId);
            return "master/commission-rates/form";
        }
    }

    @PostMapping("/{commissionRateId}/delete")
    public String delete(
            @PathVariable Long commissionRateId,
            @RequestParam(required = false) CommissionCode commissionCode,
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            RedirectAttributes redirectAttributes) {
        try {
            service.softDelete(commissionRateId);
            redirectAttributes.addFlashAttribute("successMessage", "Commission rate deactivated successfully");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        redirectAttributes.addAttribute("commissionCode", commissionCode);
        redirectAttributes.addAttribute("includeInactive", includeInactive);
        redirectAttributes.addAttribute("page", page);
        redirectAttributes.addAttribute("size", size);
        return "redirect:/master/commission-rates";
    }

    @PostMapping("/{commissionRateId}/restore")
    public String restore(@PathVariable Long commissionRateId, RedirectAttributes redirectAttributes) {
        try {
            service.restore(commissionRateId);
            redirectAttributes.addFlashAttribute("successMessage", "Commission rate restored successfully");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/master/commission-rates?includeInactive=true";
    }

    private void populateForm(Model model, CommissionRateRequest form, Long commissionRateId) {
        model.addAttribute("commissionRateForm", form);
        model.addAttribute("commissionRateId", commissionRateId);
        model.addAttribute("isEdit", commissionRateId != null);
        model.addAttribute("commissionCodes", CommissionCode.values());
    }
}
