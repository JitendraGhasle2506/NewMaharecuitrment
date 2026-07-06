package com.maharecruitment.gov.in.web.service.mobile.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.service.CustomUserDetailsService;
import com.maharecruitment.gov.in.auth.service.UserLoginTrackingService;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.web.dto.mobile.MobileEmployeeDetails;
import com.maharecruitment.gov.in.web.dto.mobile.MobileLoginRequest;
import com.maharecruitment.gov.in.web.dto.mobile.MobileLoginResponse;
import com.maharecruitment.gov.in.web.service.mobile.MobileAuthenticatedUser;
import com.maharecruitment.gov.in.web.service.mobile.MobileAuthenticationService;
import com.maharecruitment.gov.in.web.service.mobile.MobileEmployeeDetailsService;
import com.maharecruitment.gov.in.web.service.mobile.MobileLoginResponseMapper;
import com.maharecruitment.gov.in.web.service.mobile.MobileTokenIssue;
import com.maharecruitment.gov.in.web.service.mobile.MobileTokenService;

@Service
public class MobileAuthenticationServiceImpl implements MobileAuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final UserLoginTrackingService userLoginTrackingService;
    private final MobileTokenService tokenService;
    private final MobileEmployeeDetailsService employeeDetailsService;
    private final MobileLoginResponseMapper responseMapper;
    private final EmployeeRepository employeeRepository;

    public MobileAuthenticationServiceImpl(
            AuthenticationManager authenticationManager,
            CustomUserDetailsService userDetailsService,
            UserLoginTrackingService userLoginTrackingService,
            MobileTokenService tokenService,
            MobileEmployeeDetailsService employeeDetailsService,
            MobileLoginResponseMapper responseMapper,
            EmployeeRepository employeeRepository) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.userLoginTrackingService = userLoginTrackingService;
        this.tokenService = tokenService;
        this.employeeDetailsService = employeeDetailsService;
        this.responseMapper = responseMapper;
        this.employeeRepository = employeeRepository;
    }

    @Override
    @Transactional
    public MobileLoginResponse authenticate(MobileLoginRequest request) {
        String identifier = normalizeIdentifier(request.username());
        String authenticationIdentifier = resolveAuthenticationIdentifier(identifier);
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(authenticationIdentifier, request.password()));

        User user = userDetailsService.loadDomainUserByIdentifier(authentication.getName());
        MobileEmployeeDetails employeeDetails = employeeDetailsService.loadForUser(user);
        if (employeeDetails.empId() == null) {
            throw new DisabledException("Active employee profile was not found for mobile login.");
        }

        LocalDateTime loginAt = LocalDateTime.now();
        LocalDateTime lastLoginAt = userLoginTrackingService.recordSuccessfulLogin(user, loginAt);
        List<String> roles = authorities(authentication);

        MobileTokenIssue token = tokenService.issueToken(new MobileAuthenticatedUser(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getMobileNo(),
                roles));

        return responseMapper.toResponse(user, employeeDetails, roles, token, loginAt, lastLoginAt);
    }

    private String resolveAuthenticationIdentifier(String identifier) {
        if (!StringUtils.hasText(identifier) || identifier.contains("@") || identifier.matches("^[0-9]{10,15}$")) {
            return identifier;
        }

        return employeeRepository.findByEmployeeCodeIgnoreCase(identifier)
                .map(this::emailForActiveEmployee)
                .orElse(identifier);
    }

    private String emailForActiveEmployee(EmployeeEntity employee) {
        if (employee == null
                || !StringUtils.hasText(employee.getStatus())
                || !"ACTIVE".equalsIgnoreCase(employee.getStatus().trim())
                || !StringUtils.hasText(employee.getEmail())) {
            throw new DisabledException("Active employee profile was not found for mobile login.");
        }
        return employee.getEmail().trim();
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
