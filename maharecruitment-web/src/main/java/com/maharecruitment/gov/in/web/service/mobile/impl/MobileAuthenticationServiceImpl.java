package com.maharecruitment.gov.in.web.service.mobile.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.service.CustomUserDetailsService;
import com.maharecruitment.gov.in.auth.service.UserLoginTrackingService;
import com.maharecruitment.gov.in.web.dto.mobile.MobileLoginRequest;
import com.maharecruitment.gov.in.web.dto.mobile.MobileLoginResponse;
import com.maharecruitment.gov.in.web.service.mobile.MobileAuthenticatedUser;
import com.maharecruitment.gov.in.web.service.mobile.MobileAuthenticationService;
import com.maharecruitment.gov.in.web.service.mobile.MobileTokenIssue;
import com.maharecruitment.gov.in.web.service.mobile.MobileTokenService;

@Service
public class MobileAuthenticationServiceImpl implements MobileAuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final UserLoginTrackingService userLoginTrackingService;
    private final MobileTokenService tokenService;

    public MobileAuthenticationServiceImpl(
            AuthenticationManager authenticationManager,
            CustomUserDetailsService userDetailsService,
            UserLoginTrackingService userLoginTrackingService,
            MobileTokenService tokenService) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.userLoginTrackingService = userLoginTrackingService;
        this.tokenService = tokenService;
    }

    @Override
    @Transactional
    public MobileLoginResponse authenticate(MobileLoginRequest request) {
        String identifier = normalizeIdentifier(request.username());
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(identifier, request.password()));

        User user = userDetailsService.loadDomainUserByIdentifier(authentication.getName());
        LocalDateTime loginAt = LocalDateTime.now();
        LocalDateTime lastLoginAt = userLoginTrackingService.recordSuccessfulLogin(user, loginAt);
        List<String> roles = authorities(authentication);

        MobileTokenIssue token = tokenService.issueToken(new MobileAuthenticatedUser(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getMobileNo(),
                roles));

        return new MobileLoginResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getMobileNo(),
                roles,
                token.tokenType(),
                token.accessToken(),
                token.expiresInSeconds(),
                token.expiresAt(),
                loginAt,
                lastLoginAt);
    }

    private String normalizeIdentifier(String username) {
        if (!StringUtils.hasText(username)) {
            return "";
        }
        return username.trim();
    }

    private List<String> authorities(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(StringUtils::hasText)
                .distinct()
                .sorted()
                .toList();
    }
}
