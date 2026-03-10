package com.maharecruitment.gov.in.web.controller.admin;

import java.util.ArrayList;
import java.util.List;

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

import com.maharecruitment.gov.in.auth.dto.MenuUpsertRequest;
import com.maharecruitment.gov.in.auth.entity.MstMenu;
import com.maharecruitment.gov.in.auth.service.MenuManagementService;
import com.maharecruitment.gov.in.auth.service.RoleManagementService;
import com.maharecruitment.gov.in.web.dto.admin.MenuForm;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin/menus")
public class AdminMenuPageController {

    private static final Logger log = LoggerFactory.getLogger(AdminMenuPageController.class);

    private static final int MENU_PARENT = 0;
    private static final int MENU_DIRECT = 1;

    private final MenuManagementService menuManagementService;
    private final RoleManagementService roleManagementService;

    public AdminMenuPageController(
            MenuManagementService menuManagementService,
            RoleManagementService roleManagementService) {
        this.menuManagementService = menuManagementService;
        this.roleManagementService = roleManagementService;
    }

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        Page<MstMenu> menus = menuManagementService.getAll(pageable);
        model.addAttribute("menus", menus);
        return "admin/menus/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        MenuForm form = new MenuForm();
        form.setIsSubMenu(MENU_PARENT);
        populateForm(model, form, null);
        return "admin/menus/form";
    }

    @GetMapping("/{menuId}/edit")
    public String editForm(@PathVariable Long menuId, Model model, RedirectAttributes redirectAttributes) {
        try {
            MstMenu menu = menuManagementService.getById(menuId);
            MenuForm form = toForm(menu);
            populateForm(model, form, menuId);
            return "admin/menus/form";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/menus";
        }
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("menuForm") MenuForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        validateForm(form, bindingResult);
        if (bindingResult.hasErrors()) {
            populateForm(model, form, null);
            return "admin/menus/form";
        }

        try {
            MstMenu created = menuManagementService.create(toRequest(form));
            redirectAttributes.addFlashAttribute("successMessage", "Menu created: " + created.getMenuNameEnglish());
            return "redirect:/admin/menus";
        } catch (RuntimeException ex) {
            log.error("Menu create failed", ex);
            model.addAttribute("errorMessage", ex.getMessage());
            populateForm(model, form, null);
            return "admin/menus/form";
        }
    }

    @PostMapping("/{menuId}")
    public String update(
            @PathVariable Long menuId,
            @Valid @ModelAttribute("menuForm") MenuForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        validateForm(form, bindingResult);
        if (bindingResult.hasErrors()) {
            populateForm(model, form, menuId);
            return "admin/menus/form";
        }

        try {
            MstMenu updated = menuManagementService.update(menuId, toRequest(form));
            redirectAttributes.addFlashAttribute("successMessage", "Menu updated: " + updated.getMenuNameEnglish());
            return "redirect:/admin/menus";
        } catch (RuntimeException ex) {
            log.error("Menu update failed for id={}", menuId, ex);
            model.addAttribute("errorMessage", ex.getMessage());
            populateForm(model, form, menuId);
            return "admin/menus/form";
        }
    }

    @PostMapping("/{menuId}/delete")
    public String delete(@PathVariable Long menuId, RedirectAttributes redirectAttributes) {
        try {
            menuManagementService.delete(menuId);
            redirectAttributes.addFlashAttribute("successMessage", "Menu deleted successfully");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/menus";
    }

    private void populateForm(Model model, MenuForm form, Long menuId) {
        model.addAttribute("menuForm", form);
        model.addAttribute("menuId", menuId);
        model.addAttribute("isEdit", menuId != null);
        model.addAttribute("availableRoles", roleManagementService.getAll());
        model.addAttribute("menuTypeParent", MENU_PARENT);
        model.addAttribute("menuTypeDirect", MENU_DIRECT);
    }

    private void validateForm(MenuForm form, BindingResult bindingResult) {
        if (form.getRoleIds() == null || form.getRoleIds().isEmpty()) {
            bindingResult.rejectValue("roleIds", "menu.roles", "At least one role is required.");
        }
        if (form.getIsSubMenu() != null && form.getIsSubMenu() == MENU_DIRECT) {
            if (form.getUrl() == null || form.getUrl().isBlank()) {
                bindingResult.rejectValue("url", "menu.url", "URL is required for direct-link menu.");
            }
        }
    }

    private MenuUpsertRequest toRequest(MenuForm form) {
        MenuUpsertRequest request = new MenuUpsertRequest();
        request.setMenuNameEnglish(form.getMenuNameEnglish());
        request.setMenuNameMarathi(form.getMenuNameMarathi());
        request.setIcon(form.getIcon());
        request.setUrl(form.getUrl());
        request.setIsSubMenu(form.getIsSubMenu());
        request.setIsActive(form.getIsActive());
        request.setRoleIds(form.getRoleIds() == null ? List.of() : new ArrayList<>(form.getRoleIds()));
        return request;
    }

    private MenuForm toForm(MstMenu menu) {
        MenuForm form = new MenuForm();
        form.setMenuNameEnglish(menu.getMenuNameEnglish());
        form.setMenuNameMarathi(menu.getMenuNameMarathi());
        form.setIcon(menu.getIcon());
        form.setUrl(menu.getUrl());
        form.setIsSubMenu(menu.getIsSubMenu());
        form.setIsActive(menu.getIsActive() == null ? "Y" : menu.getIsActive());
        form.setRoleIds(menu.getRoles() == null
                ? List.of()
                : menu.getRoles().stream().map(role -> role.getId()).toList());
        return form;
    }
}
