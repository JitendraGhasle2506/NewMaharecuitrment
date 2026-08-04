package com.maharecruitment.gov.in.web.controller.hr;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.web.dto.hr.EmployeeCellBulkMappingForm;
import com.maharecruitment.gov.in.web.dto.hr.EmployeeCellMappingUpdateForm;
import com.maharecruitment.gov.in.web.service.hr.EmployeeCellMappingPageService;
import com.maharecruitment.gov.in.web.service.hr.model.EmployeeCellBulkMappingResult;
import com.maharecruitment.gov.in.web.service.hr.model.EmployeeCellMappingEditView;
import com.maharecruitment.gov.in.web.service.hr.model.EmployeeCellMappingEmployeeView;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/hr/employee-cell-mappings")
@PreAuthorize("hasAuthority('ROLE_HR')")
public class EmployeeCellMappingController {

    private static final Logger log = LoggerFactory.getLogger(EmployeeCellMappingController.class);
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;

    private final EmployeeCellMappingPageService employeeCellMappingPageService;

    public EmployeeCellMappingController(EmployeeCellMappingPageService employeeCellMappingPageService) {
        this.employeeCellMappingPageService = employeeCellMappingPageService;
    }

    @GetMapping
    public String list(
            @RequestParam(required = false, defaultValue = "ALL") String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(name = "search", required = false) String search,
            Principal principal,
            Model model) {
        int resolvedPage = Math.max(page, 0);
        int resolvedSize = resolvePageSize(size);
        String normalizedType = normalizeType(type);
        String normalizedSearch = normalizeSearch(search);
        Pageable pageable = PageRequest.of(
                resolvedPage,
                resolvedSize,
                Sort.by(Sort.Order.asc("fullName"), Sort.Order.asc("employeeId")));

        log.info(
                "Loading employee cell mapping list. actorLoginId={}, type={}, page={}, size={}, searchPresent={}",
                actorLoginId(principal),
                normalizedType,
                resolvedPage,
                resolvedSize,
                normalizedSearch != null);

        Page<EmployeeCellMappingEmployeeView> employeePage = employeeCellMappingPageService.searchEmployees(
                normalizedType,
                normalizedSearch,
                pageable);
        if (employeePage.getTotalPages() > 0 && resolvedPage >= employeePage.getTotalPages()) {
            pageable = PageRequest.of(
                    employeePage.getTotalPages() - 1,
                    resolvedSize,
                    Sort.by(Sort.Order.asc("fullName"), Sort.Order.asc("employeeId")));
            employeePage = employeeCellMappingPageService.searchEmployees(
                    normalizedType,
                    normalizedSearch,
                    pageable);
        }

        model.addAttribute("employees", employeePage.getContent());
        model.addAttribute("employeePage", employeePage);
        model.addAttribute("currentType", normalizedType);
        model.addAttribute("searchTerm", normalizedSearch == null ? "" : normalizedSearch);
        model.addAttribute("pageSize", employeePage.getSize());
        model.addAttribute("availableCells", employeeCellMappingPageService.availableActiveCells());
        if (!model.containsAttribute("bulkMappingForm")) {
            model.addAttribute("bulkMappingForm", new EmployeeCellBulkMappingForm());
        }
        return "hr/employee-cell-mapping-list";
    }

    @PostMapping("/bulk")
    public String bulkUpdate(
            @Valid @ModelAttribute("bulkMappingForm") EmployeeCellBulkMappingForm form,
            BindingResult bindingResult,
            @RequestParam(required = false, defaultValue = "ALL") String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(name = "search", required = false) String search,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getAllErrors().stream()
                    .findFirst()
                    .map(error -> error.getDefaultMessage())
                    .orElse("Select a cell and at least one employee.");
            redirectAttributes.addFlashAttribute("errorMessage", errorMessage);
            redirectAttributes.addFlashAttribute("bulkMappingForm", form);
            return "redirect:/hr/employee-cell-mappings" + buildBackQuery(type, page, size, search);
        }

        try {
            EmployeeCellBulkMappingResult result = employeeCellMappingPageService.updateMappings(
                    form.getCellId(),
                    form.getEmployeeIds(),
                    actorLoginId(principal));
            redirectAttributes.addFlashAttribute("successMessage", bulkSuccessMessage(result));
        } catch (RecruitmentNotificationException ex) {
            log.warn(
                    "Bulk employee cell mapping failed. actorLoginId={}, cellId={}, selectedEmployees={}, reason={}",
                    actorLoginId(principal),
                    form.getCellId(),
                    form.getEmployeeIds() == null ? 0 : form.getEmployeeIds().size(),
                    ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            redirectAttributes.addFlashAttribute("bulkMappingForm", form);
        }
        return "redirect:/hr/employee-cell-mappings" + buildBackQuery(type, page, size, search);
    }

    @GetMapping("/{employeeId}")
    public String edit(
            @PathVariable Long employeeId,
            @RequestParam(required = false, defaultValue = "ALL") String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(name = "search", required = false) String search,
            Principal principal,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            EmployeeCellMappingEditView editView = employeeCellMappingPageService.loadMapping(employeeId);
            EmployeeCellMappingUpdateForm form = new EmployeeCellMappingUpdateForm();
            form.setCellId(editView.selectedCell() == null ? null : editView.selectedCell().cellId());
            populateEditModel(model, editView, form, type, page, size, search);
            log.info(
                    "Loading employee cell mapping edit screen. employeeId={}, actorLoginId={}",
                    employeeId,
                    actorLoginId(principal));
            return "hr/employee-cell-mapping-form";
        } catch (RecruitmentNotificationException ex) {
            log.warn(
                    "Employee cell mapping edit load failed. employeeId={}, actorLoginId={}, reason={}",
                    employeeId,
                    actorLoginId(principal),
                    ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/hr/employee-cell-mappings";
        }
    }

    @PostMapping("/{employeeId}")
    public String update(
            @PathVariable Long employeeId,
            @Valid @ModelAttribute("mappingForm") EmployeeCellMappingUpdateForm form,
            BindingResult bindingResult,
            @RequestParam(required = false, defaultValue = "ALL") String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(name = "search", required = false) String search,
            Principal principal,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            log.warn(
                    "Employee cell mapping validation failed. employeeId={}, actorLoginId={}",
                    employeeId,
                    actorLoginId(principal));
            EmployeeCellMappingEditView editView = employeeCellMappingPageService.loadMapping(employeeId);
            populateEditModel(model, editView, form, type, page, size, search);
            String errorMessage = bindingResult.getAllErrors().stream()
                    .findFirst()
                    .map(error -> error.getDefaultMessage())
                    .orElse("Select a cell to map this employee.");
            model.addAttribute("errorMessage", errorMessage);
            return "hr/employee-cell-mapping-form";
        }

        try {
            boolean changed = employeeCellMappingPageService.updateMapping(
                    employeeId,
                    form.getCellId(),
                    actorLoginId(principal));
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    changed ? "Employee cell mapping updated successfully."
                            : "Employee cell mapping is already up to date.");
            return "redirect:/hr/employee-cell-mappings/" + employeeId
                    + buildBackQuery(type, page, size, search);
        } catch (RecruitmentNotificationException ex) {
            log.warn(
                    "Employee cell mapping update failed. employeeId={}, actorLoginId={}, reason={}",
                    employeeId,
                    actorLoginId(principal),
                    ex.getMessage());
            EmployeeCellMappingEditView editView = employeeCellMappingPageService.loadMapping(employeeId);
            populateEditModel(model, editView, form, type, page, size, search);
            model.addAttribute("errorMessage", ex.getMessage());
            return "hr/employee-cell-mapping-form";
        }
    }

    private void populateEditModel(
            Model model,
            EmployeeCellMappingEditView editView,
            EmployeeCellMappingUpdateForm form,
            String type,
            int page,
            int size,
            String search) {
        model.addAttribute("editView", editView);
        model.addAttribute("mappingForm", form);
        model.addAttribute("currentType", normalizeType(type));
        model.addAttribute("currentPage", Math.max(page, 0));
        model.addAttribute("pageSize", resolvePageSize(size));
        String normalizedSearch = normalizeSearch(search);
        model.addAttribute("searchTerm", normalizedSearch == null ? "" : normalizedSearch);
    }

    private int resolvePageSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private String normalizeType(String type) {
        if (!StringUtils.hasText(type)) {
            return "ALL";
        }
        String normalizedType = type.trim().toUpperCase();
        if ("INTERNAL".equals(normalizedType) || "EXTERNAL".equals(normalizedType)) {
            return normalizedType;
        }
        return "ALL";
    }

    private String normalizeSearch(String search) {
        return StringUtils.hasText(search) ? search.trim() : null;
    }

    private String actorLoginId(Principal principal) {
        return principal == null || !StringUtils.hasText(principal.getName()) ? "SYSTEM" : principal.getName();
    }

    private String bulkSuccessMessage(EmployeeCellBulkMappingResult result) {
        if (result.changedCount() == 0) {
            return "Selected employees are already mapped to this cell.";
        }
        if (result.unchangedCount() == 0) {
            return result.changedCount() + " employee cell mapping(s) updated successfully.";
        }
        return result.changedCount() + " employee cell mapping(s) updated successfully. "
                + result.unchangedCount() + " already matched.";
    }

    private String buildBackQuery(String type, int page, int size, String search) {
        StringBuilder query = new StringBuilder("?type=").append(normalizeType(type))
                .append("&page=").append(Math.max(page, 0))
                .append("&size=").append(resolvePageSize(size));
        String normalizedSearch = normalizeSearch(search);
        if (normalizedSearch != null) {
            query.append("&search=").append(URLEncoder.encode(normalizedSearch, StandardCharsets.UTF_8));
        }
        return query.toString();
    }
}
