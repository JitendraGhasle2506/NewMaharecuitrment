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
import com.maharecruitment.gov.in.master.dto.ResourceLevelExperienceRequest;
import com.maharecruitment.gov.in.master.dto.ResourceLevelExperienceResponse;
import com.maharecruitment.gov.in.master.service.ResourceLevelExperienceService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/master/resource-levels")
public class ResourceLevelExperienceController {

    private final ResourceLevelExperienceService service;

    public ResourceLevelExperienceController(ResourceLevelExperienceService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ResourceLevelExperienceResponse>> create(
            @Valid @RequestBody ResourceLevelExperienceRequest request) {
        ResourceLevelExperienceResponse response = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of("Resource level created successfully", response));
    }

    @GetMapping("/{levelId}")
    public ResponseEntity<ApiResponse<ResourceLevelExperienceResponse>> getById(
            @PathVariable Long levelId,
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        return ResponseEntity.ok(ApiResponse.of(
                "Resource level fetched successfully",
                service.getById(levelId, includeInactive)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ResourceLevelExperienceResponse>>> getAll(
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @PageableDefault(size = 20, sort = "levelId") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.of(
                "Resource levels fetched successfully",
                service.getAll(includeInactive, pageable)));
    }

    @PutMapping("/{levelId}")
    public ResponseEntity<ApiResponse<ResourceLevelExperienceResponse>> update(
            @PathVariable Long levelId,
            @Valid @RequestBody ResourceLevelExperienceRequest request) {
        return ResponseEntity.ok(ApiResponse.of(
                "Resource level updated successfully",
                service.update(levelId, request)));
    }

    @DeleteMapping("/{levelId}")
    public ResponseEntity<ApiResponse<Void>> softDelete(@PathVariable Long levelId) {
        service.softDelete(levelId);
        return ResponseEntity.ok(ApiResponse.of("Resource level deleted successfully", null));
    }

    @PatchMapping("/{levelId}/restore")
    public ResponseEntity<ApiResponse<Void>> restore(@PathVariable Long levelId) {
        service.restore(levelId);
        return ResponseEntity.ok(ApiResponse.of("Resource level restored successfully", null));
    }
}

