package com.maharecruitment.gov.in.web.controller.master;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.maharecruitment.gov.in.recruitment.dto.organization.TeamByCellResponse;
import com.maharecruitment.gov.in.recruitment.service.organization.OrganizationManagementService;

@Controller
@RequestMapping("/master/team-management")
public class TeamManagementPageController {

    private final OrganizationManagementService organizationManagementService;

    public TeamManagementPageController(OrganizationManagementService organizationManagementService) {
        this.organizationManagementService = organizationManagementService;
    }

    @GetMapping
    public String teamManagementPage(Model model) {
        model.addAttribute("cellOptions", organizationManagementService.getCellOptions());
        return "master/team-management/team-management";
    }

    @GetMapping("/teams-by-cell/{cellId}")
    @ResponseBody
    public List<TeamByCellResponse> teamsByCell(@PathVariable Long cellId) {
        return organizationManagementService.getActiveTeamsByCell(cellId);
    }
}
