package com.maharecruitment.gov.in.master.controller;

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
import com.maharecruitment.gov.in.master.dto.ProjectRequest;
import com.maharecruitment.gov.in.master.dto.ProjectResponse;
import com.maharecruitment.gov.in.master.service.ProjectMstService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/master/projects")
public class ProjectMstController {

    private final ProjectMstService service;

    public ProjectMstController(ProjectMstService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProjectResponse>> create(@Valid @RequestBody ProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of("Project created successfully", service.create(request)));
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ApiResponse<ProjectResponse>> getById(@PathVariable Long projectId) {
        return ResponseEntity.ok(ApiResponse.of("Project fetched successfully", service.getById(projectId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProjectResponse>>> getAll(
            @RequestParam(required = false) Long cellId,
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @PageableDefault(size = 20, sort = "projectId") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.of(
                "Projects fetched successfully",
                service.getAll(cellId, includeInactive, pageable)));
    }

    @PutMapping("/{projectId}")
    public ResponseEntity<ApiResponse<ProjectResponse>> update(
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectRequest request) {
        return ResponseEntity.ok(ApiResponse.of("Project updated successfully", service.update(projectId, request)));
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<ApiResponse<Void>> softDelete(@PathVariable Long projectId) {
        service.softDelete(projectId);
        return ResponseEntity.ok(ApiResponse.of("Project deactivated successfully", null));
    }

    @PatchMapping("/{projectId}/restore")
    public ResponseEntity<ApiResponse<Void>> restore(@PathVariable Long projectId) {
        service.restore(projectId);
        return ResponseEntity.ok(ApiResponse.of("Project restored successfully", null));
    }
}
