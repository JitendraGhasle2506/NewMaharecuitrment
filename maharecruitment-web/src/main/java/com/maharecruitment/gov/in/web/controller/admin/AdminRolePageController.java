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

import com.maharecruitment.gov.in.auth.entity.Role;
import com.maharecruitment.gov.in.auth.service.RoleManagementService;
import com.maharecruitment.gov.in.web.dto.admin.RoleForm;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin/roles")
public class AdminRolePageController {

    private static final Logger log = LoggerFactory.getLogger(AdminRolePageController.class);

    private final RoleManagementService roleManagementService;

    public AdminRolePageController(RoleManagementService roleManagementService) {
        this.roleManagementService = roleManagementService;
    }

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        Page<Role> roles = roleManagementService.getAll(pageable);
        model.addAttribute("roles", roles);
        return "admin/roles/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("roleForm", new RoleForm());
        model.addAttribute("isEdit", false);
        model.addAttribute("roleId", null);
        return "admin/roles/form";
    }

    @GetMapping("/{roleId}/edit")
    public String editForm(@PathVariable Long roleId, Model model, RedirectAttributes redirectAttributes) {
        try {
            Role role = roleManagementService.getById(roleId);
            RoleForm form = new RoleForm();
            form.setName(role.getName());

            model.addAttribute("roleForm", form);
            model.addAttribute("isEdit", true);
            model.addAttribute("roleId", roleId);
            return "admin/roles/form";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/roles";
        }
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("roleForm") RoleForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", false);
            model.addAttribute("roleId", null);
            return "admin/roles/form";
        }

        try {
            Role saved = roleManagementService.create(form.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Role created: " + saved.getName());
            return "redirect:/admin/roles";
        } catch (RuntimeException ex) {
            log.error("Role create failed", ex);
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("isEdit", false);
            model.addAttribute("roleId", null);
            return "admin/roles/form";
        }
    }

    @PostMapping("/{roleId}")
    public String update(
            @PathVariable Long roleId,
            @Valid @ModelAttribute("roleForm") RoleForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", true);
            model.addAttribute("roleId", roleId);
            return "admin/roles/form";
        }

        try {
            Role saved = roleManagementService.update(roleId, form.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Role updated: " + saved.getName());
            return "redirect:/admin/roles";
        } catch (RuntimeException ex) {
            log.error("Role update failed for id={}", roleId, ex);
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("isEdit", true);
            model.addAttribute("roleId", roleId);
            return "admin/roles/form";
        }
    }

    @PostMapping("/{roleId}/delete")
    public String delete(@PathVariable Long roleId, RedirectAttributes redirectAttributes) {
        try {
            roleManagementService.delete(roleId);
            redirectAttributes.addFlashAttribute("successMessage", "Role deleted successfully");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/roles";
    }
}
