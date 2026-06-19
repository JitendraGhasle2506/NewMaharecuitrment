package com.maharecruitment.gov.in.recruitment.controller.organization;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.maharecruitment.gov.in.master.dto.ApiResponse;
import com.maharecruitment.gov.in.recruitment.dto.organization.OrganizationAuditResponse;
import com.maharecruitment.gov.in.recruitment.dto.organization.OrganizationChartNodeResponse;
import com.maharecruitment.gov.in.recruitment.dto.organization.OrganizationDashboardResponse;
import com.maharecruitment.gov.in.recruitment.dto.organization.OrganizationLookupOption;
import com.maharecruitment.gov.in.recruitment.dto.organization.OrganizationSearchResult;
import com.maharecruitment.gov.in.recruitment.service.organization.OrganizationHierarchyService;
import com.maharecruitment.gov.in.recruitment.service.organization.OrganizationManagementService;

@RestController
@RequestMapping("/api/master/organization-hierarchy")
public class OrganizationHierarchyRestController {

    private final OrganizationHierarchyService organizationHierarchyService;
    private final OrganizationManagementService organizationManagementService;

    public OrganizationHierarchyRestController(
            OrganizationHierarchyService organizationHierarchyService,
            OrganizationManagementService organizationManagementService) {
        this.organizationHierarchyService = organizationHierarchyService;
        this.organizationManagementService = organizationManagementService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<OrganizationDashboardResponse>> dashboard(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long cellId) {
        return ResponseEntity.ok(ApiResponse.of(
                "Organization dashboard fetched successfully",
                organizationHierarchyService.getDashboard(projectId, cellId)));
    }

    @GetMapping("/tree")
    public ResponseEntity<ApiResponse<OrganizationChartNodeResponse>> tree(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long cellId) {
        return ResponseEntity.ok(ApiResponse.of(
                "Organization tree fetched successfully",
                organizationHierarchyService.getTree(projectId, cellId)));
    }

    @GetMapping("/chart")
    public ResponseEntity<ApiResponse<OrganizationChartNodeResponse>> chart(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long cellId) {
        return ResponseEntity.ok(ApiResponse.of(
                "Organization chart fetched successfully",
                organizationHierarchyService.getOrganizationChart(projectId, cellId)));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<OrganizationSearchResult>>> search(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long cellId,
            @RequestParam String search) {
        return ResponseEntity.ok(ApiResponse.of(
                "Organization search completed successfully",
                organizationHierarchyService.search(projectId, cellId, search)));
    }

    @GetMapping("/options/projects")
    public ResponseEntity<ApiResponse<List<OrganizationLookupOption>>> projects() {
        return ResponseEntity.ok(ApiResponse.of(
                "Project options fetched successfully",
                organizationManagementService.getProjectOptions()));
    }

    @GetMapping("/options/cells")
    public ResponseEntity<ApiResponse<List<OrganizationLookupOption>>> cells() {
        return ResponseEntity.ok(ApiResponse.of(
                "Cell options fetched successfully",
                organizationManagementService.getCellOptions()));
    }

    @GetMapping("/options/teams")
    public ResponseEntity<ApiResponse<List<OrganizationLookupOption>>> teams(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long cellId) {
        return ResponseEntity.ok(ApiResponse.of(
                "Team options fetched successfully",
                organizationManagementService.getTeamOptions(projectId, cellId)));
    }

    @GetMapping("/options/positions")
    public ResponseEntity<ApiResponse<List<OrganizationLookupOption>>> positions(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long teamId) {
        return ResponseEntity.ok(ApiResponse.of(
                "Position options fetched successfully",
                organizationManagementService.getPositionOptions(projectId, teamId)));
    }

    @GetMapping("/options/designations")
    public ResponseEntity<ApiResponse<List<OrganizationLookupOption>>> designations() {
        return ResponseEntity.ok(ApiResponse.of(
                "Designation options fetched successfully",
                organizationManagementService.getDesignationOptions()));
    }

    @GetMapping("/options/levels")
    public ResponseEntity<ApiResponse<List<OrganizationLookupOption>>> levels(
            @RequestParam(required = false) Long designationId) {
        return ResponseEntity.ok(ApiResponse.of(
                "Level options fetched successfully",
                organizationManagementService.getLevelOptions(designationId)));
    }

    @GetMapping("/options/employees")
    public ResponseEntity<ApiResponse<Page<OrganizationLookupOption>>> employees(
            @RequestParam(required = false) Long positionId,
            @RequestParam(required = false) Long designationId,
            @RequestParam(required = false) String levelCode,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.of(
                "Employee options fetched successfully",
                organizationManagementService.getEmployeeOptions(
                        positionId,
                        designationId,
                        levelCode,
                        search,
                        pageable)));
    }

    @GetMapping("/audits/{entityType}/{entityId}")
    public ResponseEntity<ApiResponse<List<OrganizationAuditResponse>>> audits(
            @PathVariable String entityType,
            @PathVariable String entityId) {
        return ResponseEntity.ok(ApiResponse.of(
                "Audit timeline fetched successfully",
                organizationManagementService.getAuditTimeline(entityType, entityId)));
    }

    @PostMapping("/projects/{projectId}/seed-sample")
    public ResponseEntity<ApiResponse<Void>> seedSample(@PathVariable Long projectId) {
        organizationManagementService.seedSampleHierarchy(projectId);
        return ResponseEntity.ok(ApiResponse.of("Sample hierarchy generated successfully", null));
    }
}
