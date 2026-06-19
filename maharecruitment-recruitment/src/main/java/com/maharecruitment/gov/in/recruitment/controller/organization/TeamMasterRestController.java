package com.maharecruitment.gov.in.recruitment.controller.organization;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.maharecruitment.gov.in.master.dto.ApiResponse;
import com.maharecruitment.gov.in.recruitment.dto.organization.TeamRequest;
import com.maharecruitment.gov.in.recruitment.dto.organization.TeamResponse;
import com.maharecruitment.gov.in.recruitment.service.organization.OrganizationManagementService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/master/teams")
public class TeamMasterRestController {

    private final OrganizationManagementService organizationManagementService;

    public TeamMasterRestController(OrganizationManagementService organizationManagementService) {
        this.organizationManagementService = organizationManagementService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TeamResponse>> create(@Valid @RequestBody TeamRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of("Team created successfully", organizationManagementService.createTeam(request)));
    }

    @GetMapping("/{teamId}")
    public ResponseEntity<ApiResponse<TeamResponse>> get(@PathVariable Long teamId) {
        return ResponseEntity.ok(ApiResponse.of("Team fetched successfully", organizationManagementService.getTeam(teamId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<TeamResponse>>> search(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long cellId,
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.of(
                "Teams fetched successfully",
                organizationManagementService.searchTeams(projectId, cellId, includeInactive, search, pageable)));
    }

    @PutMapping("/{teamId}")
    public ResponseEntity<ApiResponse<TeamResponse>> update(
            @PathVariable Long teamId,
            @Valid @RequestBody TeamRequest request) {
        return ResponseEntity.ok(ApiResponse.of(
                "Team updated successfully",
                organizationManagementService.updateTeam(teamId, request)));
    }

    @DeleteMapping("/{teamId}")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long teamId) {
        organizationManagementService.changeTeamStatus(teamId, false);
        return ResponseEntity.ok(ApiResponse.of("Team deactivated successfully", null));
    }

    @PatchMapping("/{teamId}/restore")
    public ResponseEntity<ApiResponse<Void>> restore(@PathVariable Long teamId) {
        organizationManagementService.changeTeamStatus(teamId, true);
        return ResponseEntity.ok(ApiResponse.of("Team restored successfully", null));
    }
}
