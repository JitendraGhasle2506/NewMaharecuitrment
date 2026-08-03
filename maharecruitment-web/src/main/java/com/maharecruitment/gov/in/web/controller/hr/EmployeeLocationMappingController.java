package com.maharecruitment.gov.in.web.controller.hr;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import com.maharecruitment.gov.in.web.dto.hr.EmployeeLocationMappingUpdateForm;
import com.maharecruitment.gov.in.web.service.hr.EmployeeLocationMappingPageService;
import com.maharecruitment.gov.in.web.service.hr.model.EmployeeLocationMappingEditView;
import com.maharecruitment.gov.in.web.service.hr.model.EmployeeLocationMappingEmployeeView;
import com.maharecruitment.gov.in.web.service.hr.model.EmployeeLocationOptionView;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/hr/employee-location-mappings")
@PreAuthorize("hasAuthority('ROLE_HR')")
public class EmployeeLocationMappingController {

    private static final Logger log = LoggerFactory.getLogger(EmployeeLocationMappingController.class);
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;

    private final EmployeeLocationMappingPageService employeeLocationMappingPageService;

    public EmployeeLocationMappingController(EmployeeLocationMappingPageService employeeLocationMappingPageService) {
        this.employeeLocationMappingPageService = employeeLocationMappingPageService;
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
                "Loading employee location mapping list. actorLoginId={}, type={}, page={}, size={}, searchPresent={}",
                actorLoginId(principal),
                normalizedType,
                resolvedPage,
                resolvedSize,
                normalizedSearch != null);

        Page<EmployeeLocationMappingEmployeeView> employeePage = employeeLocationMappingPageService.searchEmployees(
                normalizedType,
                normalizedSearch,
                pageable);
        if (employeePage.getTotalPages() > 0 && resolvedPage >= employeePage.getTotalPages()) {
            pageable = PageRequest.of(
                    employeePage.getTotalPages() - 1,
                    resolvedSize,
                    Sort.by(Sort.Order.asc("fullName"), Sort.Order.asc("employeeId")));
            employeePage = employeeLocationMappingPageService.searchEmployees(
                    normalizedType,
                    normalizedSearch,
                    pageable);
        }

        model.addAttribute("employees", employeePage.getContent());
        model.addAttribute("employeePage", employeePage);
        model.addAttribute("currentType", normalizedType);
        model.addAttribute("searchTerm", normalizedSearch == null ? "" : normalizedSearch);
        model.addAttribute("pageSize", employeePage.getSize());
        return "hr/employee-location-mapping-list";
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
            EmployeeLocationMappingEditView editView = employeeLocationMappingPageService.loadMapping(employeeId);
            EmployeeLocationMappingUpdateForm form = new EmployeeLocationMappingUpdateForm();
            form.setSelectedLocationIds(editView.selectedLocations().stream()
                    .map(EmployeeLocationOptionView::locationId)
                    .toList());
            form.setPrimaryLocationId(editView.primaryLocation() != null
                    ? editView.primaryLocation().locationId()
                    : null);
            populateEditModel(model, editView, form, type, page, size, search);
            log.info(
                    "Loading employee location mapping edit screen. employeeId={}, actorLoginId={}",
                    employeeId,
                    actorLoginId(principal));
            return "hr/employee-location-mapping-form";
        } catch (RecruitmentNotificationException ex) {
            log.warn(
                    "Employee location mapping edit load failed. employeeId={}, actorLoginId={}, reason={}",
                    employeeId,
                    actorLoginId(principal),
                    ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/hr/employee-location-mappings";
        }
    }

    @PostMapping("/{employeeId}")
    public String update(
            @PathVariable Long employeeId,
            @Valid @ModelAttribute("mappingForm") EmployeeLocationMappingUpdateForm form,
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
                    "Employee location mapping validation failed. employeeId={}, actorLoginId={}",
                    employeeId,
                    actorLoginId(principal));
            EmployeeLocationMappingEditView editView = employeeLocationMappingPageService.loadMapping(employeeId);
            populateEditModel(model, editView, form, type, page, size, search);
            String errorMsg = bindingResult.getAllErrors().stream()
                    .findFirst()
                    .map(e -> e.getDefaultMessage())
                    .orElse("Select at least one location and designate a primary location.");
            model.addAttribute("errorMessage", errorMsg);
            return "hr/employee-location-mapping-form";
        }

        try {
            boolean changed = employeeLocationMappingPageService.updateMapping(
                    employeeId,
                    form.getSelectedLocationIds(),
                    form.getPrimaryLocationId(),
                    actorLoginId(principal));
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    changed ? "Employee location mapping updated successfully."
                            : "Employee location mapping is already up to date.");
            return "redirect:/hr/employee-location-mappings/" + employeeId
                    + buildBackQuery(type, page, size, search);
        } catch (RecruitmentNotificationException ex) {
            log.warn(
                    "Employee location mapping update failed. employeeId={}, actorLoginId={}, reason={}",
                    employeeId,
                    actorLoginId(principal),
                    ex.getMessage());
            EmployeeLocationMappingEditView editView = employeeLocationMappingPageService.loadMapping(employeeId);
            populateEditModel(model, editView, form, type, page, size, search);
            model.addAttribute("errorMessage", ex.getMessage());
            return "hr/employee-location-mapping-form";
        }
    }

    private void populateEditModel(
            Model model,
            EmployeeLocationMappingEditView editView,
            EmployeeLocationMappingUpdateForm form,
            String type,
            int page,
            int size,
            String search) {
        model.addAttribute("editView", editView);
        model.addAttribute("mappingForm", form);
        model.addAttribute("selectedLocationFormItems", selectedLocationFormItems(editView, form));
        model.addAttribute("currentType", normalizeType(type));
        model.addAttribute("currentPage", Math.max(page, 0));
        model.addAttribute("pageSize", resolvePageSize(size));
        String normalizedSearch = normalizeSearch(search);
        model.addAttribute("searchTerm", normalizedSearch == null ? "" : normalizedSearch);
    }

    private List<EmployeeLocationOptionView> selectedLocationFormItems(
            EmployeeLocationMappingEditView editView,
            EmployeeLocationMappingUpdateForm form) {
        Map<Long, EmployeeLocationOptionView> locationById = new HashMap<>();
        editView.availableLocations().forEach(location -> locationById.put(location.locationId(), location));
        editView.selectedLocations().forEach(location -> locationById.put(location.locationId(), location));

        Long primaryId = form.getPrimaryLocationId();
        Set<Long> selectedIds = new LinkedHashSet<>();
        if (form.getSelectedLocationIds() != null) {
            form.getSelectedLocationIds().stream()
                    .filter(id -> id != null && id > 0)
                    .forEach(selectedIds::add);
        }

        List<EmployeeLocationOptionView> selectedLocations = new ArrayList<>();
        selectedIds.forEach(locationId -> {
            boolean isPrimary = locationId.equals(primaryId);
            EmployeeLocationOptionView base = locationById.get(locationId);
            if (base != null) {
                selectedLocations.add(new EmployeeLocationOptionView(
                        base.locationId(),
                        base.officeName(),
                        base.address(),
                        base.latitude(),
                        base.longitude(),
                        base.active(),
                        base.displayName(),
                        isPrimary));
            } else {
                selectedLocations.add(new EmployeeLocationOptionView(
                        locationId,
                        "",
                        "Location #" + locationId,
                        null,
                        null,
                        true,
                        "Location #" + locationId,
                        isPrimary));
            }
        });
        return selectedLocations;
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
