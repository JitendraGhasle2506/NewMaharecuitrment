package com.maharecruitment.gov.in.web.service.mobile;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.web.dto.mobile.MobileEmployeeDetails;
import com.maharecruitment.gov.in.web.dto.mobile.MobileLoginResponse;

@Component
public class MobileLoginResponseMapper {

    public MobileLoginResponse toResponse(
            User user,
            MobileEmployeeDetails employeeDetails,
            List<String> roles,
            MobileTokenIssue token,
            LocalDateTime loginAt,
            LocalDateTime lastLoginAt) {
        MobileEmployeeDetails details = employeeDetails == null
                ? MobileEmployeeDetails.empty()
                : employeeDetails;

        return new MobileLoginResponse(
                user.getId(),
                details.empId(),
                details.employeeCode(),
                textOrNull(user.getName()),
                firstText(details.employeeName(), user.getName()),
                textOrNull(user.getEmail()),
                textOrNull(user.getMobileNo()),
                details.photoUrl(),
                details.designationId(),
                details.designationName(),
                details.departmentId(),
                details.departmentName(),
                details.subDepartmentId(),
                details.subDepartmentName(),
                details.employeeType(),
                details.reportingManagerId(),
                details.reportingManagerName(),
                details.reportingDepartmentId(),
                details.reportingDepartmentName(),
                roles,
                token.tokenType(),
                token.accessToken(),
                token.expiresInSeconds(),
                token.expiresAt(),
                loginAt,
                lastLoginAt);
    }

    private String firstText(String primary, String fallback) {
        String normalizedPrimary = textOrNull(primary);
        return normalizedPrimary != null ? normalizedPrimary : textOrNull(fallback);
    }

    private String textOrNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
