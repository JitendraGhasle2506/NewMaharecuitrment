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
import com.maharecruitment.gov.in.master.dto.CellMasterDto;
import com.maharecruitment.gov.in.master.service.CellMasterService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/master/cell")
public class CellMasterController {

    private final CellMasterService service;

    public CellMasterController(CellMasterService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CellMasterDto>> create(@Valid @RequestBody CellMasterDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of("Cell created successfully", service.create(request)));
    }

    @GetMapping("/{cellId}")
    public ResponseEntity<ApiResponse<CellMasterDto>> getById(@PathVariable Long cellId) {
        return ResponseEntity.ok(ApiResponse.of("Cell fetched successfully", service.getById(cellId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CellMasterDto>>> getAll(
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        return ResponseEntity.ok(ApiResponse.of("Cells fetched successfully", service.getAll(includeInactive)));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<CellMasterDto>>> search(
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @RequestParam(defaultValue = "") String searchText,
            @PageableDefault(size = 20, sort = "cellName", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.of(
                "Cells fetched successfully",
                service.search(includeInactive, searchText, pageable)));
    }

    @PutMapping("/{cellId}")
    public ResponseEntity<ApiResponse<CellMasterDto>> update(
            @PathVariable Long cellId,
            @Valid @RequestBody CellMasterDto request) {
        return ResponseEntity.ok(ApiResponse.of("Cell updated successfully", service.update(cellId, request)));
    }

    @DeleteMapping("/{cellId}")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long cellId) {
        service.deactivate(cellId);
        return ResponseEntity.ok(ApiResponse.of("Cell deactivated successfully", null));
    }

    @PatchMapping("/{cellId}/activate")
    public ResponseEntity<ApiResponse<Void>> activate(@PathVariable Long cellId) {
        service.activate(cellId);
        return ResponseEntity.ok(ApiResponse.of("Cell activated successfully", null));
    }

    @PatchMapping("/{cellId}/status")
    public ResponseEntity<ApiResponse<Void>> toggleStatus(@PathVariable Long cellId) {
        service.toggleStatus(cellId);
        return ResponseEntity.ok(ApiResponse.of("Cell status updated successfully", null));
    }
}
