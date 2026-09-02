package com.maharecruitment.gov.in.web.template;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class EmployeeProfileUpdateAlertTemplateTest {

    @Test
    void profileUpdateWaitsForAConsistentAlertBeforeReloading() throws IOException {
        String template = new ClassPathResource("templates/employee/profile-update.html")
                .getContentAsString(StandardCharsets.UTF_8);
        String script = new ClassPathResource("static/js/employee-dashboard.js")
                .getContentAsString(StandardCharsets.UTF_8);
        String styles = new ClassPathResource("static/assets/vendor/sweetalert2/sweetalert2.min.css")
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(template)
                .contains("@{/assets/vendor/sweetalert2/sweetalert2.min.css}")
                .contains("@{/assets/vendor/sweetalert2/sweetalert2.all.min.js}");
        assertThat(styles)
                .contains("--swal2-backdrop")
                .contains(".swal2-container")
                .contains(".swal2-popup");
        assertThat(script)
                .contains("typeof window.Swal.fire === 'function'")
                .contains("confirmButtonText: 'OK'")
                .contains("allowOutsideClick: false")
                .contains("await showAlert('success', 'Success!'")
                .contains("window.location.reload();")
                .contains("error instanceof Error && error.message");
    }
}
