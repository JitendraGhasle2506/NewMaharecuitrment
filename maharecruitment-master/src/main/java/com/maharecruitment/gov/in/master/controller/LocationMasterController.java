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
import com.maharecruitment.gov.in.master.dto.LocationMasterDto;
import com.maharecruitment.gov.in.master.service.LocationMasterService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/master/locations")
public class LocationMasterController {

    private final LocationMasterService service;

    public LocationMasterController(LocationMasterService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<LocationMasterDto>> create(@Valid @RequestBody LocationMasterDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of("Location created successfully", service.create(request)));
    }

    @GetMapping("/{locationId}")
    public ResponseEntity<ApiResponse<LocationMasterDto>> getById(@PathVariable Long locationId) {
        return ResponseEntity.ok(ApiResponse.of("Location fetched successfully", service.getById(locationId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<LocationMasterDto>>> getAll(
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        return ResponseEntity.ok(ApiResponse.of("Locations fetched successfully", service.getAll(includeInactive)));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<LocationMasterDto>>> search(
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @RequestParam(defaultValue = "") String searchText,
            @PageableDefault(size = 20, sort = "locationName", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.of(
                "Locations fetched successfully",
                service.search(includeInactive, searchText, pageable)));
    }

    @PutMapping("/{locationId}")
    public ResponseEntity<ApiResponse<LocationMasterDto>> update(
            @PathVariable Long locationId,
            @Valid @RequestBody LocationMasterDto request) {
        return ResponseEntity.ok(ApiResponse.of("Location updated successfully", service.update(locationId, request)));
    }

    @DeleteMapping("/{locationId}")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long locationId) {
        service.deactivate(locationId);
        return ResponseEntity.ok(ApiResponse.of("Location deactivated successfully", null));
    }

    @PatchMapping("/{locationId}/activate")
    public ResponseEntity<ApiResponse<Void>> activate(@PathVariable Long locationId) {
        service.activate(locationId);
        return ResponseEntity.ok(ApiResponse.of("Location activated successfully", null));
    }

    @PatchMapping("/{locationId}/status")
    public ResponseEntity<ApiResponse<Void>> toggleStatus(@PathVariable Long locationId) {
        service.toggleStatus(locationId);
        return ResponseEntity.ok(ApiResponse.of("Location status updated successfully", null));
    }
}
