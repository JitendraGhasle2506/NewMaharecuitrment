package com.maharecruitment.gov.in.web.dto.mobile;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MobileLoginResponse(
        Long userId,
        Long empId,
        String employeeCode,
        String name,
        String employeeName,
        String email,
        String mobileNo,
        String photoUrl,
        String faceData,
        Long designationId,
        String designationName,
        Long departmentId,
        String departmentName,
        Long subDepartmentId,
        String subDepartmentName,
        String employeeType,
        Long reportingManagerId,
        String reportingManagerName,
        Long reportingDepartmentId,
        String reportingDepartmentName,
        List<String> roles,
        String tokenType,
        String accessToken,
        long expiresIn,
        Instant expiresAt,
        LocalDateTime loginAt,
        LocalDateTime lastLoginAt) {

    public MobileLoginResponse {
        roles = roles == null ? List.of() : List.copyOf(roles);
    }

    public MobileLoginResponse(
            Long userId,
            String name,
            String email,
            String mobileNo,
            List<String> roles,
            String tokenType,
            String accessToken,
            long expiresIn,
            Instant expiresAt,
            LocalDateTime loginAt,
            LocalDateTime lastLoginAt) {
        this(
                userId,
                null,
                null,
                name,
                null,
                email,
                mobileNo,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                roles,
                tokenType,
                accessToken,
                expiresIn,
                expiresAt,
                loginAt,
                lastLoginAt);
    }

    @JsonProperty("embedding")
    public String embedding() {
        return faceData;
    }
}
