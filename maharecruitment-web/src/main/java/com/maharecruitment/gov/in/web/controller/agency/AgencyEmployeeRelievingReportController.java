package com.maharecruitment.gov.in.web.controller.agency;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.service.UserAffiliationService;
import com.maharecruitment.gov.in.web.service.hr.EmployeeRelievingService;

import java.security.Principal;

@Controller
@RequestMapping("/agency/reports/relieving")
@PreAuthorize("hasAuthority('ROLE_AGENCY')")
public class AgencyEmployeeRelievingReportController {

    private final EmployeeRelievingService relievingService;
    private final UserAffiliationService userAffiliationService;

    public AgencyEmployeeRelievingReportController(EmployeeRelievingService relievingService,
                                                   UserAffiliationService userAffiliationService) {
        this.relievingService = relievingService;
        this.userAffiliationService = userAffiliationService;
    }

    @GetMapping
    public String viewReport(Principal principal, Model model) {
        User user = userAffiliationService.loadUserByEmail(principal.getName());
        Long agencyId = userAffiliationService.resolvePrimaryAgencyId(user);
        
        model.addAttribute("records", relievingService.getRelievingRecordsByAgency(agencyId));
        return "agency/relieving-report";
    }
}
