package com.maharecruitment.gov.in.web.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.maharecruitment.gov.in.web.dto.mobile.FcmTokenRequest;
import com.maharecruitment.gov.in.web.dto.mobile.MobileFcmTokenResponse;
import com.maharecruitment.gov.in.web.service.mobile.NotificationService;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/mobile/notification")
public class MobileNotificationController {

    private final NotificationService notificationService;

    public MobileNotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping(value = "/save-token", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MobileFcmTokenResponse> saveToken(@Valid @RequestBody FcmTokenRequest request) {
        return ResponseEntity.ok(notificationService.saveToken(request));
    }
}
