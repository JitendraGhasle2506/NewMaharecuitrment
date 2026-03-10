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

import com.maharecruitment.gov.in.auth.dto.UserUpsertRequest;
import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.DepartmentRegistrationRepository;
import com.maharecruitment.gov.in.auth.service.RoleManagementService;
import com.maharecruitment.gov.in.auth.service.UserManagementService;
import com.maharecruitment.gov.in.web.dto.admin.UserForm;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin/users")
public class AdminUserPageController {

    private static final Logger log = LoggerFactory.getLogger(AdminUserPageController.class);

    private final UserManagementService userManagementService;
    private final RoleManagementService roleManagementService;
    private final DepartmentRegistrationRepository departmentRegistrationRepository;

    public AdminUserPageController(
            UserManagementService userManagementService,
            RoleManagementService roleManagementService,
            DepartmentRegistrationRepository departmentRegistrationRepository) {
        this.userManagementService = userManagementService;
        this.roleManagementService = roleManagementService;
        this.departmentRegistrationRepository = departmentRegistrationRepository;
    }

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        Page<User> users = userManagementService.getAll(pageable);
        model.addAttribute("users", users);
        return "admin/users/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        UserForm form = new UserForm();
        populateForm(model, form, null);
        return "admin/users/form";
    }

    @GetMapping("/{userId}/edit")
    public String editForm(@PathVariable Long userId, Model model, RedirectAttributes redirectAttributes) {
        try {
            User user = userManagementService.getById(userId);
            UserForm form = toForm(user);
            populateForm(model, form, userId);
            return "admin/users/form";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/users";
        }
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("userForm") UserForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        validateRoleSelection(form, bindingResult);
        if (form.getPassword() == null || form.getPassword().isBlank()) {
            bindingResult.rejectValue("password", "user.password", "Password is required.");
        }

        if (bindingResult.hasErrors()) {
            populateForm(model, form, null);
            return "admin/users/form";
        }

        try {
            User created = userManagementService.create(toRequest(form));
            redirectAttributes.addFlashAttribute("successMessage", "User created: " + created.getEmail());
            return "redirect:/admin/users";
        } catch (RuntimeException ex) {
            log.error("User create failed", ex);
            model.addAttribute("errorMessage", ex.getMessage());
            populateForm(model, form, null);
            return "admin/users/form";
        }
    }

    @PostMapping("/{userId}")
    public String update(
            @PathVariable Long userId,
            @Valid @ModelAttribute("userForm") UserForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        validateRoleSelection(form, bindingResult);
        if (bindingResult.hasErrors()) {
            populateForm(model, form, userId);
            return "admin/users/form";
        }

        try {
            User updated = userManagementService.update(userId, toRequest(form));
            redirectAttributes.addFlashAttribute("successMessage", "User updated: " + updated.getEmail());
            return "redirect:/admin/users";
        } catch (RuntimeException ex) {
            log.error("User update failed for id={}", userId, ex);
            model.addAttribute("errorMessage", ex.getMessage());
            populateForm(model, form, userId);
            return "admin/users/form";
        }
    }

    @PostMapping("/{userId}/delete")
    public String delete(@PathVariable Long userId, RedirectAttributes redirectAttributes) {
        try {
            userManagementService.delete(userId);
            redirectAttributes.addFlashAttribute("successMessage", "User deleted successfully");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    private void populateForm(Model model, UserForm form, Long userId) {
        model.addAttribute("userForm", form);
        model.addAttribute("userId", userId);
        model.addAttribute("isEdit", userId != null);
        model.addAttribute("availableRoles", roleManagementService.getAll());
        model.addAttribute("departments", departmentRegistrationRepository.findAll());
    }

    private UserUpsertRequest toRequest(UserForm form) {
        UserUpsertRequest request = new UserUpsertRequest();
        request.setName(form.getName());
        request.setEmail(form.getEmail());
        request.setMobileNo(form.getMobileNo());
        request.setPassword(form.getPassword());
        request.setDepartmentRegistrationId(form.getDepartmentRegistrationId());
        request.setRoleIds(form.getRoleIds() == null ? List.of() : new ArrayList<>(form.getRoleIds()));
        return request;
    }

    private UserForm toForm(User user) {
        UserForm form = new UserForm();
        form.setName(user.getName());
        form.setEmail(user.getEmail());
        form.setMobileNo(user.getMobileNo());
        form.setDepartmentRegistrationId(
                user.getDepartmentRegistrationId() == null ? null : user.getDepartmentRegistrationId().getDepartmentRegistrationId());
        form.setRoleIds(user.getRoleIds());
        return form;
    }

    private void validateRoleSelection(UserForm form, BindingResult bindingResult) {
        if (form.getRoleIds() == null || form.getRoleIds().isEmpty()) {
            bindingResult.rejectValue("roleIds", "user.roles", "At least one role is required.");
        }
    }
}
