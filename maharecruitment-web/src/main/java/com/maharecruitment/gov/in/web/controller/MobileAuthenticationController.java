package com.maharecruitment.gov.in.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.maharecruitment.gov.in.web.dto.mobile.MobileLoginRequest;
import com.maharecruitment.gov.in.web.dto.mobile.MobileLoginResponse;
import com.maharecruitment.gov.in.web.dto.mobile.MobileLogoutRequest;
import com.maharecruitment.gov.in.web.dto.mobile.MobileLogoutResponse;
import com.maharecruitment.gov.in.web.dto.mobile.MobileRefreshTokenRequest;
import com.maharecruitment.gov.in.web.dto.mobile.MobileRefreshTokenResponse;
import com.maharecruitment.gov.in.web.service.mobile.MobileAuthenticationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/mobile/auth")
public class MobileAuthenticationController {

    private final MobileAuthenticationService mobileAuthenticationService;

    public MobileAuthenticationController(MobileAuthenticationService mobileAuthenticationService) {
        this.mobileAuthenticationService = mobileAuthenticationService;
    }

    @PostMapping("/login")
    public ResponseEntity<MobileLoginResponse> login(@Valid @RequestBody MobileLoginRequest request) {
        return ResponseEntity.ok(mobileAuthenticationService.authenticate(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<MobileRefreshTokenResponse> refresh(@Valid @RequestBody MobileRefreshTokenRequest request) {
        return ResponseEntity.ok(mobileAuthenticationService.refresh(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<MobileLogoutResponse> logout(@Valid @RequestBody MobileLogoutRequest request) {
        return ResponseEntity.ok(mobileAuthenticationService.logout(request));
    }
}
