package com.maharecruitment.gov.in.web.service.mobile;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.web.dto.mobile.MobileEmployeeDetails;
import com.maharecruitment.gov.in.web.dto.mobile.MobileLoginResponse;

class MobileLoginResponseMapperTest {

    private final MobileLoginResponseMapper mapper = new MobileLoginResponseMapper();

    @Test
    void toResponsePreservesExistingLoginFieldsAndAddsEmployeeDetails() {
        User user = new User();
        user.setId(9L);
        user.setName("Shiv Krushna");
        user.setEmail("shiva@gmail.com");
        user.setMobileNo("7998966981");

        MobileEmployeeDetails employeeDetails = new MobileEmployeeDetails(
                101L,
                "EMP001",
                "Shiv Krushna",
                "/documents/view?path=abc",
                "[0.123,0.456]",
                3L,
                "Project Manager",
                2L,
                "IT Department",
                5L,
                "Development",
                "INTERNAL",
                15L,
                "Mahesh Patil",
                null,
                null);
        MobileTokenIssue token = new MobileTokenIssue(
                "jwt-token",
                "Bearer",
                Instant.parse("2026-07-03T08:27:45Z"),
                Instant.parse("2026-07-03T08:57:45Z"),
                1800);
        MobileRefreshTokenIssue refreshToken = new MobileRefreshTokenIssue(
                "refresh-token",
                Instant.parse("2026-08-02T08:27:45Z"),
                2_592_000);
        LocalDateTime loginAt = LocalDateTime.parse("2026-07-03T13:57:45");
        LocalDateTime lastLoginAt = LocalDateTime.parse("2026-07-03T13:58:00");

        MobileLoginResponse response = mapper.toResponse(
                user,
                employeeDetails,
                List.of("ROLE_EMPLOYEE", "ROLE_PM"),
                token,
                refreshToken,
                loginAt,
                lastLoginAt);

        assertThat(response.userId()).isEqualTo(9L);
        assertThat(response.name()).isEqualTo("Shiv Krushna");
        assertThat(response.email()).isEqualTo("shiva@gmail.com");
        assertThat(response.mobileNo()).isEqualTo("7998966981");
        assertThat(response.roles()).containsExactly("ROLE_EMPLOYEE", "ROLE_PM");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.expiresIn()).isEqualTo(1800);
        assertThat(response.expiresAt()).isEqualTo(Instant.parse("2026-07-03T08:57:45Z"));
        assertThat(response.refreshExpiresIn()).isEqualTo(2_592_000);
        assertThat(response.refreshExpiresAt()).isEqualTo(Instant.parse("2026-08-02T08:27:45Z"));
        assertThat(response.loginAt()).isEqualTo(loginAt);
        assertThat(response.lastLoginAt()).isEqualTo(lastLoginAt);

        assertThat(response.empId()).isEqualTo(101L);
        assertThat(response.employeeCode()).isEqualTo("EMP001");
        assertThat(response.employeeName()).isEqualTo("Shiv Krushna");
        assertThat(response.photoUrl()).isEqualTo("/documents/view?path=abc");
        assertThat(response.faceData()).isEqualTo("[0.123,0.456]");
        assertThat(response.embedding()).isEqualTo("[0.123,0.456]");
        assertThat(response.designationId()).isEqualTo(3L);
        assertThat(response.designationName()).isEqualTo("Project Manager");
        assertThat(response.departmentId()).isEqualTo(2L);
        assertThat(response.departmentName()).isEqualTo("IT Department");
        assertThat(response.subDepartmentId()).isEqualTo(5L);
        assertThat(response.subDepartmentName()).isEqualTo("Development");
        assertThat(response.employeeType()).isEqualTo("INTERNAL");
        assertThat(response.reportingManagerId()).isEqualTo(15L);
        assertThat(response.reportingManagerName()).isEqualTo("Mahesh Patil");
        assertThat(response.reportingDepartmentId()).isNull();
        assertThat(response.reportingDepartmentName()).isNull();
    }

    @Test
    void toResponseFallsBackToUserNameWhenEmployeeDetailsAreMissing() {
        User user = new User();
        user.setId(5L);
        user.setName("User Only");
        user.setEmail("user@example.com");

        MobileTokenIssue token = new MobileTokenIssue(
                "token",
                "Bearer",
                Instant.parse("2026-07-03T08:00:00Z"),
                Instant.parse("2026-07-03T08:30:00Z"),
                1800);

        MobileLoginResponse response = mapper.toResponse(
                user,
                MobileEmployeeDetails.empty(),
                null,
                token,
                LocalDateTime.parse("2026-07-03T13:30:00"),
                null);

        assertThat(response.employeeName()).isEqualTo("User Only");
        assertThat(response.empId()).isNull();
        assertThat(response.roles()).isEmpty();
        assertThat(response.refreshToken()).isNull();
        assertThat(response.refreshExpiresIn()).isZero();
        assertThat(response.refreshExpiresAt()).isNull();
    }
}
