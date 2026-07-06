package com.maharecruitment.gov.in.web.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.maharecruitment.gov.in.web.dto.mobile.MobilePasswordUpdateRequest;
import com.maharecruitment.gov.in.web.dto.mobile.MobilePasswordUpdateResponse;
import com.maharecruitment.gov.in.web.dto.mobile.MobileProfileContactUpdateRequest;
import com.maharecruitment.gov.in.web.dto.mobile.MobileProfileResponse;
import com.maharecruitment.gov.in.web.service.mobile.MobileProfileService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@Validated
@RestController
@RequestMapping("/api/mobile/profile")
public class MobileProfileController {

    private final MobileProfileService mobileProfileService;

    public MobileProfileController(MobileProfileService mobileProfileService) {
        this.mobileProfileService = mobileProfileService;
    }

    @GetMapping
    public ResponseEntity<MobileProfileResponse> getProfile(
            @RequestParam("employeeId") @NotNull Long employeeId) {
        return ResponseEntity.ok(mobileProfileService.getProfile(employeeId));
    }

    @PatchMapping(value = "/contact", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MobileProfileResponse> updateContact(
            @Valid @RequestBody MobileProfileContactUpdateRequest request) {
        return ResponseEntity.ok(mobileProfileService.updateContact(request));
    }

    @PostMapping(value = "/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MobileProfileResponse> updatePhoto(
            @RequestParam("employeeId") @NotNull Long employeeId,
            @RequestParam("photo") MultipartFile photo) {
        return ResponseEntity.ok(mobileProfileService.updatePhoto(employeeId, photo));
    }

    @PostMapping(value = "/password/change", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MobilePasswordUpdateResponse> changePassword(
            @Valid @RequestBody MobilePasswordUpdateRequest request) {
        return ResponseEntity.ok(mobileProfileService.changePassword(request));
    }

    @PostMapping(value = "/password/reset", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MobilePasswordUpdateResponse> resetPassword(
            @Valid @RequestBody MobilePasswordUpdateRequest request) {
        return ResponseEntity.ok(mobileProfileService.resetPassword(request));
    }
}
