package com.maharecruitment.gov.in.web.controller.department;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.maharecruitment.gov.in.auth.entity.DepartmentRegistrationEntity;
import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.service.UserAffiliationService;
import com.maharecruitment.gov.in.web.service.hr.EmployeeRelievingService;

import java.security.Principal;

@Controller
@RequestMapping("/department/reports/relieving")
@PreAuthorize("hasAuthority('ROLE_DEPARTMENT') or hasAuthority('ROLE_HOD')")
public class HodEmployeeRelievingReportController {

    private final EmployeeRelievingService relievingService;
    private final UserAffiliationService userAffiliationService;

    public HodEmployeeRelievingReportController(EmployeeRelievingService relievingService,
                                                UserAffiliationService userAffiliationService) {
        this.relievingService = relievingService;
        this.userAffiliationService = userAffiliationService;
    }

    @GetMapping
    public String viewReport(Principal principal, Model model) {
        User user = userAffiliationService.loadUserByEmail(principal.getName());
        DepartmentRegistrationEntity deptReg = userAffiliationService.resolvePrimaryDepartmentRegistration(user);
        Long deptId = deptReg != null ? deptReg.getDepartmentRegistrationId() : null;
        
        model.addAttribute("records", relievingService.getRelievingRecordsByDepartment(deptId));
        return "department/relieving-report";
    }
}
