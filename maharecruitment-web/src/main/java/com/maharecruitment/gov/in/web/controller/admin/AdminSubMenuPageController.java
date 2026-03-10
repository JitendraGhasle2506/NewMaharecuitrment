package com.maharecruitment.gov.in.web.controller.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import com.maharecruitment.gov.in.auth.dto.SubMenuUpsertRequest;
import com.maharecruitment.gov.in.auth.entity.MstSubMenu;
import com.maharecruitment.gov.in.auth.service.MenuManagementService;
import com.maharecruitment.gov.in.auth.service.SubMenuManagementService;
import com.maharecruitment.gov.in.web.dto.admin.SubMenuForm;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin/submenus")
public class AdminSubMenuPageController {

    private static final Logger log = LoggerFactory.getLogger(AdminSubMenuPageController.class);

    public static final String VIEW_PATH = "admin/submenus/form";

    private final SubMenuManagementService subMenuManagementService;
    private final MenuManagementService menuManagementService;

    public AdminSubMenuPageController(
            SubMenuManagementService subMenuManagementService,
            MenuManagementService menuManagementService) {
        this.subMenuManagementService = subMenuManagementService;
        this.menuManagementService = menuManagementService;
    }

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        Page<MstSubMenu> subMenus = subMenuManagementService.getAll(pageable);
        model.addAttribute("subMenus", subMenus);
        return "admin/submenus/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        populateForm(model, new SubMenuForm(), null);
        return VIEW_PATH;
    }

    @GetMapping("/{subMenuId}/edit")
    public String editForm(@PathVariable Long subMenuId, Model model, RedirectAttributes redirectAttributes) {
        try {
            MstSubMenu existing = subMenuManagementService.getById(subMenuId);
            SubMenuForm form = toForm(existing);
            populateForm(model, form, subMenuId);
            return VIEW_PATH;
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/submenus";
        }
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("subMenuForm") SubMenuForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateForm(model, form, null);
            return VIEW_PATH;
        }

        try {
            MstSubMenu created = subMenuManagementService.create(toRequest(form));
            redirectAttributes.addFlashAttribute("successMessage", "Submenu created: " + created.getSubMenuNameEnglish());
            return "redirect:/admin/submenus";
        } catch (RuntimeException ex) {
            log.error("Submenu create failed", ex);
            model.addAttribute("errorMessage", ex.getMessage());
            populateForm(model, form, null);
            return VIEW_PATH;
        }
    }

    @PostMapping("/{subMenuId}")
    public String update(
            @PathVariable Long subMenuId,
            @Valid @ModelAttribute("subMenuForm") SubMenuForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateForm(model, form, subMenuId);
            return VIEW_PATH;
        }

        try {
            MstSubMenu updated = subMenuManagementService.update(subMenuId, toRequest(form));
            redirectAttributes.addFlashAttribute("successMessage", "Submenu updated: " + updated.getSubMenuNameEnglish());
            return "redirect:/admin/submenus";
        } catch (RuntimeException ex) {
            log.error("Submenu update failed for id={}", subMenuId, ex);
            model.addAttribute("errorMessage", ex.getMessage());
            populateForm(model, form, subMenuId);
            return VIEW_PATH;
        }
    }

    @PostMapping("/{subMenuId}/delete")
    public String delete(@PathVariable Long subMenuId, RedirectAttributes redirectAttributes) {
        try {
            subMenuManagementService.delete(subMenuId);
            redirectAttributes.addFlashAttribute("successMessage", "Submenu deleted successfully");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/submenus";
    }

    private void populateForm(Model model, SubMenuForm form, Long subMenuId) {
        model.addAttribute("subMenuForm", form);
        model.addAttribute("subMenuId", subMenuId);
        model.addAttribute("isEdit", subMenuId != null);
        model.addAttribute("parentMenus", menuManagementService.getParentMenus());
    }

    private SubMenuUpsertRequest toRequest(SubMenuForm form) {
        SubMenuUpsertRequest request = new SubMenuUpsertRequest();
        request.setMenuId(form.getMenuId());
        request.setSubMenuNameEnglish(form.getSubMenuNameEnglish());
        request.setSubMenuNameMarathi(form.getSubMenuNameMarathi());
        request.setControllerName(form.getControllerName());
        request.setUrl(form.getUrl());
        request.setIcon(form.getIcon());
        request.setIsActive(form.getIsActive() == null || form.getIsActive().isBlank()
                ? 'Y'
                : Character.toUpperCase(form.getIsActive().trim().charAt(0)));
        return request;
    }

    private SubMenuForm toForm(MstSubMenu existing) {
        SubMenuForm form = new SubMenuForm();
        form.setMenuId(existing.getMenu() == null ? null : existing.getMenu().getMenuId());
        form.setSubMenuNameEnglish(existing.getSubMenuNameEnglish());
        form.setSubMenuNameMarathi(existing.getSubMenuNameMarathi());
        form.setControllerName(existing.getControllerName());
        form.setUrl(existing.getUrl());
        form.setIcon(existing.getIcon());
        form.setIsActive(existing.getIsActive() == null ? "Y" : String.valueOf(existing.getIsActive()));
        return form;
    }
}
