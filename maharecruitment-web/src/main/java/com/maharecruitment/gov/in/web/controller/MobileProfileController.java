package com.maharecruitment.gov.in.web.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
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
import com.maharecruitment.gov.in.web.dto.mobile.MobileProfilePhotoUpdateRequest;
import com.maharecruitment.gov.in.web.dto.mobile.MobileProfileResponse;
import com.maharecruitment.gov.in.web.service.mobile.MobileApiException;
import com.maharecruitment.gov.in.web.service.mobile.MobileBase64ImageMapper;
import com.maharecruitment.gov.in.web.service.mobile.MobileProfileService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@Validated
@RestController
@RequestMapping("/api/mobile/profile")
public class MobileProfileController {

    private final MobileProfileService mobileProfileService;
    private final MobileBase64ImageMapper mobileBase64ImageMapper;

    public MobileProfileController(
            MobileProfileService mobileProfileService,
            MobileBase64ImageMapper mobileBase64ImageMapper) {
        this.mobileProfileService = mobileProfileService;
        this.mobileBase64ImageMapper = mobileBase64ImageMapper;
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
            @RequestParam("photo") MultipartFile photo,
            @RequestParam(value = "embedding", required = false) String embedding,
            @RequestParam(value = "faceData", required = false) String faceData,
            @RequestParam(value = "faceEmbedding", required = false) String faceEmbedding) {
        return ResponseEntity.ok(mobileProfileService.updatePhoto(
                employeeId,
                photo,
                firstText(embedding, faceData, faceEmbedding)));
    }

    @PostMapping(value = "/photo", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MobileProfileResponse> updatePhotoJson(
            @Valid @RequestBody MobileProfilePhotoUpdateRequest request) {
        MultipartFile photo = toPhotoMultipartFile(request);
        return ResponseEntity.ok(mobileProfileService.updatePhoto(request.employeeId(), photo, request.embedding()));
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

    private MultipartFile toPhotoMultipartFile(MobileProfilePhotoUpdateRequest request) {
        try {
            return mobileBase64ImageMapper.toMultipartFile(
                    request.photo(),
                    request.photoFileName(),
                    request.photoContentType(),
                    "photo",
                    "profile-photo");
        } catch (MobileBase64ImageMapper.InvalidBase64ImageException ex) {
            throw new MobileApiException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "INVALID_IMAGE",
                    ex.getMessage());
        }
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }
}
