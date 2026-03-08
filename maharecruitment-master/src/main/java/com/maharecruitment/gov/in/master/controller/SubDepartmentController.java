package com.maharecruitment.gov.in.master.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.maharecruitment.gov.in.master.dto.ApiResponse;
import com.maharecruitment.gov.in.master.dto.SubDepartmentRequest;
import com.maharecruitment.gov.in.master.dto.SubDepartmentResponse;
import com.maharecruitment.gov.in.master.service.SubDepartmentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/master/sub-departments")
public class SubDepartmentController {

    private final SubDepartmentService service;

    public SubDepartmentController(SubDepartmentService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SubDepartmentResponse>> create(
            @Valid @RequestBody SubDepartmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of("Sub-department created successfully", service.create(request)));
    }

    @GetMapping("/{subDeptId}")
    public ResponseEntity<ApiResponse<SubDepartmentResponse>> getById(@PathVariable Long subDeptId) {
        return ResponseEntity.ok(ApiResponse.of(
                "Sub-department fetched successfully",
                service.getById(subDeptId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<SubDepartmentResponse>>> getAll(
            @RequestParam(required = false) Long departmentId,
            @PageableDefault(size = 20, sort = "subDeptId") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.of(
                "Sub-departments fetched successfully",
                service.getAll(departmentId, pageable)));
    }

    @PutMapping("/{subDeptId}")
    public ResponseEntity<ApiResponse<SubDepartmentResponse>> update(
            @PathVariable Long subDeptId,
            @Valid @RequestBody SubDepartmentRequest request) {
        return ResponseEntity.ok(ApiResponse.of(
                "Sub-department updated successfully",
                service.update(subDeptId, request)));
    }

    @DeleteMapping("/{subDeptId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long subDeptId) {
        service.delete(subDeptId);
        return ResponseEntity.ok(ApiResponse.of("Sub-department deleted successfully", null));
    }
}
