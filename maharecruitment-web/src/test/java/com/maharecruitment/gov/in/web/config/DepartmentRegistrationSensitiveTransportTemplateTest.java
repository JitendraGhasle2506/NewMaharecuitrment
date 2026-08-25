package com.maharecruitment.gov.in.web.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DepartmentRegistrationSensitiveTransportTemplateTest {

    @Test
    void registrationUsesUnnamedVisibleFieldsAndEncryptedMultipartProperties() throws Exception {
        String template = Files.readString(projectPath(
                "src/main/resources/templates/register/department-registration.html"));
        String script = Files.readString(projectPath(
                "src/main/resources/static/js/sensitive-data-encryption.js"));

        assertThat(template)
                .contains("data-encrypt-sensitive=\"true\"")
                .contains("data-sensitive-key-url=@{/security/credential-encryption/public-key}")
                .contains("data-sensitive-encrypted-name=\"gstNumberEncrypted\"")
                .contains("data-sensitive-encrypted-name=\"panNumberEncrypted\"")
                .contains("data-sensitive-encrypted-name=\"tanNumberEncrypted\"")
                .contains("type=\"text\" class=\"form-control text-uppercase\" id=\"gstNo\"")
                .contains("type=\"text\" class=\"form-control text-uppercase\" id=\"panNo\"")
                .contains("type=\"password\" class=\"form-control text-uppercase\" id=\"tanNo\"")
                .contains("data-sensitive-field=\"gstNumber\"")
                .contains("data-sensitive-field=\"panNumber\"")
                .contains("data-sensitive-field=\"tanNumber\"")
                .contains("data-sensitive-purpose=\"DEPARTMENT_REGISTRATION\"")
                .contains("data-preserve-on-validation-error=\"true\"")
                .contains("data-field-error=\"gstNumberEncrypted\"")
                .contains("data-field-error=\"panNumberEncrypted\"")
                .contains("data-field-error=\"tanNumberEncrypted\"")
                .contains("data-selected-file-name=\"gstFile\"")
                .contains("data-selected-file-name=\"panFile\"")
                .contains("data-selected-file-name=\"tanFile\"")
                .contains("data-otp-resend-cooldown-seconds=${otpResendCooldownSeconds}")
                .contains("@{/js/otp-verification.js(v='20260819-otp-policy')}")
                .contains("@{/js/sensitive-data-encryption.js(v='20260813-agency')}")
                .contains("@{/js/department-registration.js(v='20260819-otp-policy')}")
                .doesNotContain("MutationObserver")
                .doesNotContain("th:field=\"*{gstNo}\"")
                .doesNotContain("th:field=\"*{panNo}\"")
                .doesNotContain("type=\"password\" class=\"form-control text-uppercase\" id=\"gstNo\"")
                .doesNotContain("type=\"password\" class=\"form-control text-uppercase\" id=\"panNo\"")
                .doesNotContain("name=\"gstNo\"")
                .doesNotContain("name=\"panNo\"")
                .doesNotContain("name=\"tanNo\"")
                .doesNotContain("uploadedGstFilePath", "uploadedPanFilePath", "uploadedTanFilePath");
        assertThat(script)
                .contains("if (event.defaultPrevented) return")
                .contains("form.dataset.sensitiveSubmitting === \"true\"")
                .contains("event.preventDefault()")
                .contains("form.dataset.sensitiveSubmitting = \"true\"")
                .contains("data-sensitive-encrypted-name")
                .contains("algorithm !== \"RSA-OAEP-256\"")
                .contains("Date.now() + published.clockOffset")
                .contains("\"SENSITIVE:v1\"")
                .contains("window.requestIdleCallback(warmEncryptionKey")
                .contains("submitWithoutNavigation(form)")
                .contains("body: new FormData(form)")
                .contains("renderValidationErrors(form, result)")
                .contains("\"Accept\": \"application/json\"")
                .contains("Your entered values and selected PDF documents have been kept.")
                .contains("clearGeneratedFields(form)")
                .doesNotContain("new DOMParser()")
                .doesNotContain("includes(\"X\")");

        String otpScript = Files.readString(projectPath(
                "src/main/resources/static/js/otp-verification.js"));
        assertThat(otpScript)
                .contains("state.sendInProgress || resendTimerId")
                .contains("data.resendAvailableInSeconds")
                .contains("DEFAULT_RESEND_COOLDOWN_SECONDS = 120")
                .contains("startResendCooldown")
                .contains("Resend OTP (")
                .contains("state.verifyInProgress || state.locked || !state.otpActive")
                .contains("OTP_ATTEMPTS_EXCEEDED")
                .contains("OTP expired. Please request a new OTP.")
                .contains("OTP valid for ");

        String registrationScript = Files.readString(projectPath(
                "src/main/resources/static/js/department-registration.js"));
        assertThat(registrationScript)
                .contains("form.dataset.otpResendCooldownSeconds || \"120\"")
                .contains("resendCooldownSeconds: otpResendCooldownSeconds")
                .contains("Selected: ${file.name}")
                .contains("const subDepartmentCache = new Map()")
                .contains("const requestController = new AbortController()")
                .contains("subDepartmentSelect.replaceChildren(options)")
                .contains("cacheInitialSubDepartments()")
                .contains(".on(\"change.departmentRegistration\", handleDepartmentChange)")
                .contains("bindDynamicSelectEvents()")
                .contains("updateDepartmentState(true, false)");
    }

    private Path projectPath(String relativePath) {
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path direct = userDir.resolve(relativePath);
        return Files.exists(direct) ? direct : userDir.resolve("maharecruitment-web").resolve(relativePath);
    }
}
