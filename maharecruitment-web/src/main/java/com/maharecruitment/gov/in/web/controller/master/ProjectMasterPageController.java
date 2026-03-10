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

import com.maharecruitment.gov.in.master.dto.ProjectRequest;
import com.maharecruitment.gov.in.master.dto.ProjectResponse;
import com.maharecruitment.gov.in.master.entity.ProjectType;
import com.maharecruitment.gov.in.master.service.ProjectMstService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/master/projects")
public class ProjectMasterPageController {

    private final ProjectMstService projectService;

    public ProjectMasterPageController(ProjectMstService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        Page<ProjectResponse> projects = projectService.getAll(pageable);

        model.addAttribute("projects", projects);
        return "master/projects/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        populateForm(model, new ProjectRequest(), null);
        return "master/projects/form";
    }

    @GetMapping("/{projectId}/edit")
    public String editForm(@PathVariable Long projectId, Model model, RedirectAttributes redirectAttributes) {
        try {
            ProjectResponse existing = projectService.getById(projectId);
            ProjectRequest form = new ProjectRequest();
            form.setProjectName(existing.getProjectName());
            form.setProjectDesc(existing.getProjectDesc());
            form.setProjectType(existing.getProjectType());
            form.setDepartmentRegistrationId(existing.getDepartmentRegistrationId());
            form.setApplicationId(existing.getApplicationId());

            populateForm(model, form, projectId);
            return "master/projects/form";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/master/projects";
        }
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("projectForm") ProjectRequest form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateForm(model, form, null);
            return "master/projects/form";
        }

        try {
            projectService.create(form);
            redirectAttributes.addFlashAttribute("successMessage", "Project created successfully");
            return "redirect:/master/projects";
        } catch (RuntimeException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            populateForm(model, form, null);
            return "master/projects/form";
        }
    }

    @PostMapping("/{projectId}")
    public String update(
            @PathVariable Long projectId,
            @Valid @ModelAttribute("projectForm") ProjectRequest form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateForm(model, form, projectId);
            return "master/projects/form";
        }

        try {
            projectService.update(projectId, form);
            redirectAttributes.addFlashAttribute("successMessage", "Project updated successfully");
            return "redirect:/master/projects";
        } catch (RuntimeException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            populateForm(model, form, projectId);
            return "master/projects/form";
        }
    }

    @PostMapping("/{projectId}/delete")
    public String delete(@PathVariable Long projectId, RedirectAttributes redirectAttributes) {
        try {
            projectService.delete(projectId);
            redirectAttributes.addFlashAttribute("successMessage", "Project deleted successfully");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/master/projects";
    }

    private void populateForm(Model model, ProjectRequest form, Long projectId) {
        model.addAttribute("projectForm", form);
        model.addAttribute("projectId", projectId);
        model.addAttribute("isEdit", projectId != null);
        model.addAttribute("projectTypes", ProjectType.values());
    }
}
