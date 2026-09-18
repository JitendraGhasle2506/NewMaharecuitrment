package com.maharecruitment.gov.in.web.controller.hr;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.maharecruitment.gov.in.recruitment.entity.EmployeeRecruitmentType;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.web.dto.hr.EmployeeProjectBulkMappingForm;
import com.maharecruitment.gov.in.web.dto.hr.EmployeeProjectMappingUpdateForm;
import com.maharecruitment.gov.in.web.service.hr.EmployeeProjectMappingPageService;
import com.maharecruitment.gov.in.web.service.hr.model.EmployeeProjectBulkMappingResult;
import com.maharecruitment.gov.in.web.service.hr.model.EmployeeProjectMappingEditView;
import com.maharecruitment.gov.in.web.service.hr.model.EmployeeProjectMappingEmployeeView;
import com.maharecruitment.gov.in.web.service.hr.model.EmployeeProjectOptionView;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/hr/employee-project-mappings")
@PreAuthorize("hasAuthority('ROLE_HR')")
public class EmployeeProjectMappingController {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;

    private final EmployeeProjectMappingPageService mappingService;

    public EmployeeProjectMappingController(EmployeeProjectMappingPageService mappingService) {
        this.mappingService = mappingService;
    }

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "ALL") String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long projectId,
            Model model) {
        int resolvedPage = Math.max(page, 0);
        int resolvedSize = resolvePageSize(size);
        String normalizedType = normalizeType(type);
        String normalizedSearch = normalizeSearch(search);
        List<EmployeeProjectOptionView> availableProjects = mappingService.availableActiveProjects(normalizedType);
        Long selectedProjectId = resolveSelectedProjectId(projectId, availableProjects);
        EmployeeProjectOptionView selectedProject = selectedProjectId == null
                ? null
                : availableProjects.stream()
                        .filter(project -> project.projectId().equals(selectedProjectId))
                        .findFirst()
                        .orElse(null);
        Pageable pageable = PageRequest.of(
                resolvedPage,
                resolvedSize,
                Sort.by(Sort.Order.asc("fullName"), Sort.Order.asc("employeeId")));
        Page<EmployeeProjectMappingEmployeeView> employeePage = mappingService.searchUnmappedEmployees(
                normalizedType,
                normalizedSearch,
                selectedProjectId,
                pageable);
        if (employeePage.getTotalPages() > 0 && resolvedPage >= employeePage.getTotalPages()) {
            pageable = PageRequest.of(employeePage.getTotalPages() - 1, resolvedSize, pageable.getSort());
            employeePage = mappingService.searchUnmappedEmployees(
                    normalizedType,
                    normalizedSearch,
                    selectedProjectId,
                    pageable);
        }

        model.addAttribute("employees", employeePage.getContent());
        model.addAttribute("employeePage", employeePage);
        model.addAttribute("currentType", normalizedType);
        model.addAttribute("searchTerm", normalizedSearch == null ? "" : normalizedSearch);
        model.addAttribute("pageSize", employeePage.getSize());
        model.addAttribute("availableProjects", availableProjects);
        model.addAttribute("selectedProjectId", selectedProjectId);
        model.addAttribute("selectedProject", selectedProject);
        if (!model.containsAttribute("bulkMappingForm")) {
            model.addAttribute("bulkMappingForm", new EmployeeProjectBulkMappingForm());
        }
        return "hr/employee-project-mapping-list";
    }

    @GetMapping("/mapped")
    public String mapped(
            @RequestParam(defaultValue = "ALL") String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            Model model) {
        int resolvedPage = Math.max(page, 0);
        int resolvedSize = resolvePageSize(size);
        String normalizedType = normalizeType(type);
        String normalizedSearch = normalizeSearch(search);
        Pageable pageable = PageRequest.of(
                resolvedPage,
                resolvedSize,
                Sort.by(Sort.Order.asc("fullName"), Sort.Order.asc("employeeId")));
        Page<EmployeeProjectMappingEmployeeView> employeePage = mappingService.searchMappedEmployees(
                normalizedType,
                normalizedSearch,
                pageable);
        if (employeePage.getTotalPages() > 0 && resolvedPage >= employeePage.getTotalPages()) {
            pageable = PageRequest.of(employeePage.getTotalPages() - 1, resolvedSize, pageable.getSort());
            employeePage = mappingService.searchMappedEmployees(normalizedType, normalizedSearch, pageable);
        }

        model.addAttribute("employees", employeePage.getContent());
        model.addAttribute("employeePage", employeePage);
        model.addAttribute("currentType", normalizedType);
        model.addAttribute("searchTerm", normalizedSearch == null ? "" : normalizedSearch);
        model.addAttribute("pageSize", employeePage.getSize());
        return "hr/employee-project-mapped-list";
    }

    @PostMapping("/bulk")
    public String bulkUpdate(
            @Valid @ModelAttribute("bulkMappingForm") EmployeeProjectBulkMappingForm form,
            BindingResult bindingResult,
            @RequestParam(defaultValue = "ALL") String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long projectId,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    bindingResult.getAllErrors().getFirst().getDefaultMessage());
            redirectAttributes.addFlashAttribute("bulkMappingForm", form);
            return "redirect:/hr/employee-project-mappings" + buildBackQuery(type, page, size, search, projectId);
        }
        try {
            EmployeeProjectBulkMappingResult result = mappingService.updateMappings(
                    form.getProjectId(),
                    form.getEmployeeIds());
            redirectAttributes.addFlashAttribute("successMessage", bulkSuccessMessage(result));
        } catch (RecruitmentNotificationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            redirectAttributes.addFlashAttribute("bulkMappingForm", form);
        }
        return "redirect:/hr/employee-project-mappings" + buildBackQuery(type, page, size, search, projectId);
    }

    @GetMapping("/{employeeId}")
    public String edit(
            @PathVariable Long employeeId,
            @RequestParam(defaultValue = "ALL") String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long filterProjectId,
            @RequestParam(required = false) Long projectId,
            @RequestParam(defaultValue = "assign") String source,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            Long resolvedFilterProjectId = filterProjectId == null ? projectId : filterProjectId;
            EmployeeProjectMappingEditView editView = mappingService.loadMapping(employeeId);
            EmployeeProjectMappingUpdateForm form = new EmployeeProjectMappingUpdateForm();
            form.setProjectId(editView.selectedProject() == null ? null : editView.selectedProject().projectId());
            populateEditModel(model, editView, form, type, page, size, search, resolvedFilterProjectId, source);
            return "hr/employee-project-mapping-form";
        } catch (RecruitmentNotificationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/hr/employee-project-mappings";
        }
    }

    @PostMapping("/{employeeId}")
    public String update(
            @PathVariable Long employeeId,
            @Valid @ModelAttribute("mappingForm") EmployeeProjectMappingUpdateForm form,
            BindingResult bindingResult,
            @RequestParam(defaultValue = "ALL") String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long filterProjectId,
            @RequestParam(defaultValue = "assign") String source,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            EmployeeProjectMappingEditView editView = mappingService.loadMapping(employeeId);
            populateEditModel(model, editView, form, type, page, size, search, filterProjectId, source);
            model.addAttribute("errorMessage", bindingResult.getAllErrors().getFirst().getDefaultMessage());
            return "hr/employee-project-mapping-form";
        }
        try {
            boolean changed = mappingService.updateMapping(employeeId, form.getProjectId());
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    changed ? "Employee project mapping updated successfully."
                            : "Employee is already mapped to this project.");
            return "redirect:/hr/employee-project-mappings/" + employeeId
                    + buildBackQuery(type, page, size, search, filterProjectId)
                    + "&source=" + normalizeSource(source);
        } catch (RecruitmentNotificationException ex) {
            EmployeeProjectMappingEditView editView = mappingService.loadMapping(employeeId);
            populateEditModel(model, editView, form, type, page, size, search, filterProjectId, source);
            model.addAttribute("errorMessage", ex.getMessage());
            return "hr/employee-project-mapping-form";
        }
    }

    private void populateEditModel(
            Model model,
            EmployeeProjectMappingEditView editView,
            EmployeeProjectMappingUpdateForm form,
            String type,
            int page,
            int size,
            String search,
            Long filterProjectId,
            String source) {
        model.addAttribute("editView", editView);
        model.addAttribute("mappingForm", form);
        model.addAttribute("currentType", normalizeType(type));
        model.addAttribute("currentPage", Math.max(page, 0));
        model.addAttribute("pageSize", resolvePageSize(size));
        model.addAttribute("searchTerm", normalizeSearch(search) == null ? "" : normalizeSearch(search));
        model.addAttribute("filterProjectId", normalizeProjectId(filterProjectId));
        model.addAttribute("source", normalizeSource(source));
        model.addAttribute("backUrl", buildListUrl(source, type, page, size, search, filterProjectId));
    }

    private int resolvePageSize(int size) {
        return size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
    }

    private String normalizeType(String type) {
        if (!StringUtils.hasText(type)) {
            return "ALL";
        }
        String normalized = EmployeeRecruitmentType.normalizeOrNull(type);
        return normalized == null ? "ALL" : normalized;
    }

    private String normalizeSearch(String search) {
        return StringUtils.hasText(search) ? search.trim() : null;
    }

    private Long normalizeProjectId(Long projectId) {
        return projectId == null || projectId < 1 ? null : projectId;
    }

    private Long resolveSelectedProjectId(Long projectId, List<EmployeeProjectOptionView> availableProjects) {
        Long normalizedProjectId = normalizeProjectId(projectId);
        if (normalizedProjectId == null) {
            return null;
        }
        return availableProjects.stream()
                .anyMatch(project -> project.projectId().equals(normalizedProjectId))
                        ? normalizedProjectId
                        : null;
    }

    private String buildBackQuery(String type, int page, int size, String search) {
        return buildBackQuery(type, page, size, search, null);
    }

    private String buildBackQuery(String type, int page, int size, String search, Long projectId) {
        StringBuilder query = new StringBuilder("?type=").append(normalizeType(type))
                .append("&page=").append(Math.max(page, 0))
                .append("&size=").append(resolvePageSize(size));
        Long normalizedProjectId = normalizeProjectId(projectId);
        if (normalizedProjectId != null) {
            query.append("&projectId=").append(normalizedProjectId);
        }
        String normalizedSearch = normalizeSearch(search);
        if (normalizedSearch != null) {
            query.append("&search=").append(URLEncoder.encode(normalizedSearch, StandardCharsets.UTF_8));
        }
        return query.toString();
    }

    private String buildListUrl(String source, String type, int page, int size, String search, Long projectId) {
        String path = "mapped".equals(normalizeSource(source))
                ? "/hr/employee-project-mappings/mapped"
                : "/hr/employee-project-mappings";
        return path + buildBackQuery(type, page, size, search, projectId);
    }

    private String normalizeSource(String source) {
        return "mapped".equalsIgnoreCase(source) ? "mapped" : "assign";
    }

    private String bulkSuccessMessage(EmployeeProjectBulkMappingResult result) {
        if (result.changedCount() == 0) {
            return "All selected employees are already mapped to this project.";
        }
        if (result.unchangedCount() == 0) {
            return result.changedCount() + " employee project mapping(s) updated successfully.";
        }
        return result.changedCount() + " employee project mapping(s) updated successfully. "
                + result.unchangedCount() + " already matched.";
    }
}
