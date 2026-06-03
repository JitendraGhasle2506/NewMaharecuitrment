package com.maharecruitment.gov.in.master.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import com.maharecruitment.gov.in.master.dto.WingMasterDto;
import com.maharecruitment.gov.in.master.service.WingMasterService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/master/wings")
public class WingMasterController {

    private final WingMasterService service;

    public WingMasterController(WingMasterService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WingMasterDto>> create(@Valid @RequestBody WingMasterDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of("Wing created successfully", service.create(request)));
    }

    @GetMapping("/{wingId}")
    public ResponseEntity<ApiResponse<WingMasterDto>> getById(@PathVariable Long wingId) {
        return ResponseEntity.ok(ApiResponse.of("Wing fetched successfully", service.getById(wingId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<WingMasterDto>>> getAll(
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        return ResponseEntity.ok(ApiResponse.of("Wings fetched successfully", service.getAll(includeInactive)));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<WingMasterDto>>> search(
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @RequestParam(defaultValue = "") String searchText,
            @PageableDefault(size = 20, sort = "wingName", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.of(
                "Wings fetched successfully",
                service.search(includeInactive, searchText, pageable)));
    }

    @PutMapping("/{wingId}")
    public ResponseEntity<ApiResponse<WingMasterDto>> update(
            @PathVariable Long wingId,
            @Valid @RequestBody WingMasterDto request) {
        return ResponseEntity.ok(ApiResponse.of("Wing updated successfully", service.update(wingId, request)));
    }

    @DeleteMapping("/{wingId}")
    public ResponseEntity<ApiResponse<Void>> softDelete(@PathVariable Long wingId) {
        service.softDelete(wingId);
        return ResponseEntity.ok(ApiResponse.of("Wing deactivated successfully", null));
    }

    @PatchMapping("/{wingId}/restore")
    public ResponseEntity<ApiResponse<Void>> restore(@PathVariable Long wingId) {
        service.restore(wingId);
        return ResponseEntity.ok(ApiResponse.of("Wing restored successfully", null));
    }

    @PatchMapping("/{wingId}/status")
    public ResponseEntity<ApiResponse<Void>> toggleStatus(@PathVariable Long wingId) {
        service.toggleStatus(wingId);
        return ResponseEntity.ok(ApiResponse.of("Wing status updated successfully", null));
    }
}
