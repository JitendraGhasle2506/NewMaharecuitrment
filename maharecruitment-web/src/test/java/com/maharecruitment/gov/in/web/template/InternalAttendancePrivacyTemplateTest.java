package com.maharecruitment.gov.in.web.template;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class InternalAttendancePrivacyTemplateTest {

    @Test
    void attendancePhotoUsesAuthenticatedEndpointAndHasBrokenImageFallback() throws IOException {
        String template = template();

        assertThat(template)
                .contains("th:src=\"@{/employee/profile/photo}\"")
                .contains("th:alt=\"|Photograph of ${attendance.name ?: 'employee'}|\"")
                .contains("profilePhoto.addEventListener('error', showGenericAvatar)")
                .contains("attendanceProfilePhotoFallback")
                .doesNotContain("/files/serve")
                .doesNotContain("attendance.photoPath")
                .doesNotContain("filePath=");
    }

    @Test
    void aadhaarStartsMaskedAndRevealNeverEmbedsAFullValueInMarkupOrScript() throws IOException {
        String template = template();

        assertThat(template)
                .contains("th:text=\"${attendance.aadhaarNumber ?: '-'}\"")
                .contains("data-reveal-url=@{/employee/intAttendance/aadhaar/reveal}")
                .contains("method: 'POST'")
                .contains("meta[name=\"_csrf\"]")
                .contains("meta[name=\"_csrf_header\"]")
                .contains("aria-label=\"Show Aadhaar\"")
                .contains("aria-pressed=\"false\"")
                .contains("title=\"Show Aadhaar\"")
                .contains("window.setTimeout")
                .contains("}, 30000)")
                .contains("visibilitychange")
                .contains("pagehide")
                .doesNotContain("data-aadhaar")
                .doesNotContain("name=\"aadhaar")
                .doesNotContain("type=\"hidden\" id=\"aadhaar");
    }

    @Test
    void mappedLocationsHavePrimarySecondaryDropdownAndSafeFallbackPresentation() throws IOException {
        String template = template();

        assertThat(template)
                .contains("Mapped Work Locations")
                .contains("Primary Location")
                .contains("Secondary Locations")
                .contains("${attendance.primaryLocation.displayName}")
                .contains("<details class=\"profile-location-dropdown\"")
                .contains("th:each=\"location : ${attendance.secondaryLocations}\"")
                .contains("aria-label=\"Secondary work locations\"")
                .contains("${location.displayName}")
                .contains("Not mapped")
                .contains("No secondary locations")
                .doesNotContain("${attendance.primaryLocation.latitude}")
                .doesNotContain("${attendance.primaryLocation.longitude}")
                .doesNotContain("${location.latitude}")
                .doesNotContain("${location.longitude}");
    }

    @Test
    void departmentIsShownOnlyForExternalEmployees() throws IOException {
        assertThat(template())
                .contains("th:if=\"${externalEmployee}\"")
                .contains(">Department</span>")
                .contains("th:text=\"${employeeDepartment ?: '-'}\"")
                .contains("th:if=\"${externalEmployee && employeeSubDepartment != null}\"")
                .contains(">Sub Department</span>")
                .contains("th:text=\"${employeeSubDepartment}\"");
    }

    private String template() throws IOException {
        ClassPathResource resource =
                new ClassPathResource("templates/attendance/attendance-register-internal.html");
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }
}
