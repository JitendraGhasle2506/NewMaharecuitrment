package com.maharecruitment.gov.in.web.controller.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

import com.maharecruitment.gov.in.auth.entity.Role;
import com.maharecruitment.gov.in.auth.service.RoleManagementService;
import com.maharecruitment.gov.in.auth.service.RoleMenuMappingService;
import com.maharecruitment.gov.in.web.dto.admin.RoleMenuMappingForm;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin/role-menu-mappings")
public class AdminRoleMenuMappingPageController {

    private static final Logger log = LoggerFactory.getLogger(AdminRoleMenuMappingPageController.class);

    private final RoleManagementService roleManagementService;
    private final RoleMenuMappingService roleMenuMappingService;

    public AdminRoleMenuMappingPageController(
            RoleManagementService roleManagementService,
            RoleMenuMappingService roleMenuMappingService) {
        this.roleManagementService = roleManagementService;
        this.roleMenuMappingService = roleMenuMappingService;
    }

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        Page<Role> roles = roleManagementService.getAll(pageable);
        List<Long> roleIds = roles.getContent().stream().map(Role::getId).toList();
        Map<Long, Long> menuCounts = roleMenuMappingService.countMenusByRoleIds(roleIds);

        model.addAttribute("roles", roles);
        model.addAttribute("menuCounts", menuCounts);
        return "admin/role-menu-mappings/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        RoleMenuMappingForm form = new RoleMenuMappingForm();
        populateForm(model, form, null);
        return "admin/role-menu-mappings/form";
    }

    @GetMapping("/{roleId}/edit")
    public String editForm(@PathVariable Long roleId, Model model, RedirectAttributes redirectAttributes) {
        try {
            Role role = roleManagementService.getById(roleId);

            RoleMenuMappingForm form = new RoleMenuMappingForm();
            form.setRoleId(roleId);
            form.setMenuIds(new ArrayList<>(roleMenuMappingService.getMenuIdsByRoleId(roleId)));

            populateForm(model, form, role);
            return "admin/role-menu-mappings/form";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/role-menu-mappings";
        }
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("mappingForm") RoleMenuMappingForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateForm(model, form, null);
            return "admin/role-menu-mappings/form";
        }

        try {
            roleMenuMappingService.replaceRoleMenuMappings(form.getRoleId(), form.getMenuIds());
            redirectAttributes.addFlashAttribute("successMessage", "Role-menu mapping saved successfully");
            return "redirect:/admin/role-menu-mappings";
        } catch (RuntimeException ex) {
            log.error("Role-menu mapping create failed", ex);
            model.addAttribute("errorMessage", ex.getMessage());
            populateForm(model, form, null);
            return "admin/role-menu-mappings/form";
        }
    }

    @PostMapping("/{roleId}")
    public String update(
            @PathVariable Long roleId,
            @Valid @ModelAttribute("mappingForm") RoleMenuMappingForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        form.setRoleId(roleId);

        if (bindingResult.hasErrors()) {
            Role role = null;
            try {
                role = roleManagementService.getById(roleId);
            } catch (RuntimeException ignored) {
            }
            populateForm(model, form, role);
            return "admin/role-menu-mappings/form";
        }

        try {
            roleMenuMappingService.replaceRoleMenuMappings(roleId, form.getMenuIds());
            redirectAttributes.addFlashAttribute("successMessage", "Role-menu mapping updated successfully");
            return "redirect:/admin/role-menu-mappings";
        } catch (RuntimeException ex) {
            log.error("Role-menu mapping update failed for roleId={}", roleId, ex);
            model.addAttribute("errorMessage", ex.getMessage());
            Role role = null;
            try {
                role = roleManagementService.getById(roleId);
            } catch (RuntimeException ignored) {
            }
            populateForm(model, form, role);
            return "admin/role-menu-mappings/form";
        }
    }

    @PostMapping("/{roleId}/delete")
    public String delete(@PathVariable Long roleId, RedirectAttributes redirectAttributes) {
        try {
            roleMenuMappingService.clearRoleMenuMappings(roleId);
            redirectAttributes.addFlashAttribute("successMessage", "Role-menu mappings cleared successfully");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/role-menu-mappings";
    }

    private void populateForm(Model model, RoleMenuMappingForm form, Role selectedRole) {
        model.addAttribute("mappingForm", form);
        model.addAttribute("availableRoles", roleManagementService.getAll());
        model.addAttribute("availableMenus", roleMenuMappingService.getAllMenus());
        model.addAttribute("selectedRole", selectedRole);
        model.addAttribute("isEdit", selectedRole != null);
    }
}
