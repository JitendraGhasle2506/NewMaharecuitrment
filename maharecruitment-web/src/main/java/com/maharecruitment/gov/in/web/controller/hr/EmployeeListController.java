package com.maharecruitment.gov.in.web.controller.hr;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.web.service.hr.HROnboardingPageService;
import com.maharecruitment.gov.in.web.service.hr.model.EmployeeOnboardingDetailView;
import com.maharecruitment.gov.in.web.service.hr.model.EmployeeListView;

@Controller
@RequestMapping("/hr/employees")
@PreAuthorize("hasAuthority('ROLE_HR')")
public class EmployeeListController {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;

    private final HROnboardingPageService hrOnboardingPageService;

    public EmployeeListController(HROnboardingPageService hrOnboardingPageService) {
        this.hrOnboardingPageService = hrOnboardingPageService;
    }

    @GetMapping
    public String employeeList(
            @RequestParam(required = false, defaultValue = "ALL") String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(name = "search", required = false) String search,
            Model model) {
        return renderEmployeeList(type, "ACTIVE", page, size, search, model);
    }

    @GetMapping("/resigned")
    public String resignedEmployeeList(
            @RequestParam(required = false, defaultValue = "ALL") String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(name = "search", required = false) String search,
            Model model) {
        return renderEmployeeList(type, "RESIGNED", page, size, search, model);
    }

    @GetMapping("/{employeeId}")
    public String employeeDetail(
            @PathVariable Long employeeId,
            @RequestParam(required = false, defaultValue = "ACTIVE") String status,
            @RequestParam(required = false, defaultValue = "ALL") String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(name = "search", required = false) String search,
            Model model,
            RedirectAttributes redirectAttributes) {
        String normalizedStatus = normalizeStatus(status);
        String normalizedType = normalizeType(type);
        int resolvedPage = Math.max(page, 0);
        int resolvedSize = resolvePageSize(size);
        String normalizedSearch = normalizeSearch(search);

        try {
            EmployeeOnboardingDetailView employeeDetail = hrOnboardingPageService.loadEmployeeDetail(employeeId);
            model.addAttribute("employeeDetail", employeeDetail);
            model.addAttribute("currentStatus", normalizedStatus);
            model.addAttribute("currentType", normalizedType);
            model.addAttribute("currentPage", resolvedPage);
            model.addAttribute("pageSize", resolvedSize);
            model.addAttribute("searchTerm", normalizedSearch == null ? "" : normalizedSearch);
            return "hr/employee-onboarding-detail";
        } catch (RecruitmentNotificationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:" + ("RESIGNED".equals(normalizedStatus) ? "/hr/employees/resigned" : "/hr/employees");
        }
    }

    private String renderEmployeeList(
            String type,
            String status,
            int page,
            int size,
            String search,
            Model model) {
        int resolvedPage = Math.max(page, 0);
        int resolvedSize = resolvePageSize(size);
        String normalizedType = normalizeType(type);
        String normalizedSearch = normalizeSearch(search);

        Page<EmployeeListView> employeePage = loadEmployeePage(
                normalizedType,
                status,
                normalizedSearch,
                resolvedPage,
                resolvedSize);
        if (employeePage.getTotalPages() > 0 && resolvedPage >= employeePage.getTotalPages()) {
            employeePage = loadEmployeePage(
                    normalizedType,
                    status,
                    normalizedSearch,
                    employeePage.getTotalPages() - 1,
                    resolvedSize);
        }

        model.addAttribute("employees", employeePage.getContent());
        model.addAttribute("employeePage", employeePage);
        model.addAttribute("currentType", normalizedType);
        model.addAttribute("currentStatus", status);
        model.addAttribute("searchTerm", normalizedSearch == null ? "" : normalizedSearch);
        model.addAttribute("pageSize", employeePage.getSize());
        if ("RESIGNED".equalsIgnoreCase(status)) {
            model.addAttribute("pageTitle", "Resigned Employees");
            model.addAttribute("pageSubtitle", "Employees resigned from the company and released their vacancy.");
        } else {
            model.addAttribute("pageTitle", "Onboarded Employees");
            model.addAttribute("pageSubtitle", "List of active employees onboarded through the portal.");
        }

        return "hr/employee-list";
    }

    private Page<EmployeeListView> loadEmployeePage(
            String type,
            String status,
            String search,
            int page,
            int size) {
        var pageRequest = PageRequest.of(page, size, Sort.by("employeeId").descending());
        return hrOnboardingPageService.getEmployeesByStatus(type, status, search, pageRequest);
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

    private String normalizeStatus(String status) {
        return "RESIGNED".equalsIgnoreCase(status) ? "RESIGNED" : "ACTIVE";
    }
}
