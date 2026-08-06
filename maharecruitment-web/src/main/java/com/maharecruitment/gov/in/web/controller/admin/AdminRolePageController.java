package com.maharecruitment.gov.in.web.controller.admin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
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
    private static final Set<Integer> ALLOWED_PAGE_SIZES = Set.of(10, 25, 50, 100);

    private final RoleManagementService roleManagementService;

    public AdminRolePageController(RoleManagementService roleManagementService) {
        this.roleManagementService = roleManagementService;
    }

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(name = "search", required = false) String search,
            Model model) {
        int normalizedSize = normalizePageSize(size);
        String normalizedSearch = normalizeSearch(search);
        Pageable pageable = PageRequest.of(Math.max(page, 0), normalizedSize,
                Sort.by(Sort.Direction.ASC, "name"));
        Page<Role> roles = roleManagementService.getAll(normalizedSearch, pageable);
        if (roles.getTotalPages() > 0 && page >= roles.getTotalPages()) {
            pageable = PageRequest.of(roles.getTotalPages() - 1, normalizedSize,
                    Sort.by(Sort.Direction.ASC, "name"));
            roles = roleManagementService.getAll(normalizedSearch, pageable);
        }
        model.addAttribute("roles", roles);
        model.addAttribute("searchTerm", normalizedSearch == null ? "" : normalizedSearch);
        model.addAttribute("pageSize", roles.getSize());
        model.addAttribute("protectedRoleNames", roleManagementService.getAllowedRoleNames());
        return "admin/roles/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("roleForm", new RoleForm());
        populateFormModel(model, false, null);
        return "admin/roles/form";
    }

    @GetMapping("/{roleId}/edit")
    public String editForm(@PathVariable Long roleId, Model model, RedirectAttributes redirectAttributes) {
        try {
            Role role = roleManagementService.getById(roleId);
            RoleForm form = new RoleForm();
            form.setName(role.getName());

            model.addAttribute("roleForm", form);
            populateFormModel(model, true, roleId);
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
            populateFormModel(model, false, null);
            return "admin/roles/form";
        }

        try {
            Role saved = roleManagementService.create(form.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Role created: " + saved.getName());
            return "redirect:/admin/roles";
        } catch (RuntimeException ex) {
            log.error("Role create failed", ex);
            model.addAttribute("errorMessage", ex.getMessage());
            populateFormModel(model, false, null);
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
            populateFormModel(model, true, roleId);
            return "admin/roles/form";
        }

        try {
            Role saved = roleManagementService.update(roleId, form.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Role updated: " + saved.getName());
            return "redirect:/admin/roles";
        } catch (RuntimeException ex) {
            log.error("Role update failed for id={}", roleId, ex);
            model.addAttribute("errorMessage", ex.getMessage());
            populateFormModel(model, true, roleId);
            return "admin/roles/form";
        }
    }

    @PostMapping("/{roleId}/delete")
    public String delete(
            @PathVariable Long roleId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(name = "search", required = false) String search,
            RedirectAttributes redirectAttributes) {
        try {
            roleManagementService.delete(roleId);
            redirectAttributes.addFlashAttribute("successMessage", "Role deleted successfully");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        redirectAttributes.addAttribute("page", Math.max(page, 0));
        redirectAttributes.addAttribute("size", normalizePageSize(size));
        if (StringUtils.hasText(search)) {
            redirectAttributes.addAttribute("search", search.trim());
        }
        return "redirect:/admin/roles";
    }

    private void populateFormModel(Model model, boolean isEdit, Long roleId) {
        Set<String> assignedRoleNames = new HashSet<>();
        roleManagementService.getAll().stream()
                .filter(role -> roleId == null || !roleId.equals(role.getId()))
                .map(Role::getName)
                .forEach(assignedRoleNames::add);

        List<String> availableRoleNames = roleManagementService.getAllowedRoleNames().stream()
                .filter(roleName -> !assignedRoleNames.contains(roleName))
                .toList();
        model.addAttribute("availableRoleNames", availableRoleNames);
        model.addAttribute("isEdit", isEdit);
        model.addAttribute("roleId", roleId);
    }

    private int normalizePageSize(int size) {
        return ALLOWED_PAGE_SIZES.contains(size) ? size : 10;
    }

    private String normalizeSearch(String search) {
        return StringUtils.hasText(search) ? search.trim() : null;
    }
}
