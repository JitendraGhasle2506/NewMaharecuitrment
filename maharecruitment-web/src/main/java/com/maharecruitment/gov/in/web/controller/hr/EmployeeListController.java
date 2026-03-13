package com.maharecruitment.gov.in.web.controller.hr;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.maharecruitment.gov.in.web.service.hr.HROnboardingPageService;
import com.maharecruitment.gov.in.web.service.hr.model.EmployeeListView;

@Controller
@RequestMapping("/hr/employees")
@PreAuthorize("hasRole('ROLE_HR')")
public class EmployeeListController {

    private final HROnboardingPageService hrOnboardingPageService;

    public EmployeeListController(HROnboardingPageService hrOnboardingPageService) {
        this.hrOnboardingPageService = hrOnboardingPageService;
    }

    @GetMapping
    public String employeeList(
            @RequestParam(required = false, defaultValue = "ALL") String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        
        var pageRequest = PageRequest.of(page, size, Sort.by("employeeId").descending());
        Page<EmployeeListView> employeePage = hrOnboardingPageService.getOnboardedEmployees(type, pageRequest);
        
        model.addAttribute("employees", employeePage.getContent());
        model.addAttribute("employeePage", employeePage);
        model.addAttribute("currentType", type);
        
        return "hr/employee-list";
    }
}
