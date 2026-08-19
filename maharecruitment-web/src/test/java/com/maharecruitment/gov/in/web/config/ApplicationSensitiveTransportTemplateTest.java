package com.maharecruitment.gov.in.web.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class ApplicationSensitiveTransportTemplateTest {

    @Test
    void candidateIdentityNeverUsesPlaintextFormNamesOrUrlValidation() throws Exception {
        String template = Files.readString(modulePath(
                "maharecruitment-web",
                "src/main/resources/templates/agency/pre-onboarding-form.html"));
        String script = Files.readString(modulePath(
                "maharecruitment-web",
                "src/main/resources/static/js/pre-onboarding.js"));
        String controller = Files.readString(modulePath(
                "maharecruitment-web",
                "src/main/java/com/maharecruitment/gov/in/web/controller/agency/AgencyOnboardingPageController.java"));

        assertThat(template)
                .contains("data-sensitive-purpose=\"AGENCY_PRE_ONBOARDING\"")
                .contains("data-sensitive-encrypted-name=\"aadhaarEncrypted\"")
                .contains("data-sensitive-encrypted-name=\"panEncrypted\"")
                .contains("data-sensitive-field=\"aadhaar\"")
                .contains("data-sensitive-field=\"pan\"")
                .contains("th:value=\"*{maskedAadhaar}\"")
                .contains("th:value=\"*{maskedPan}\"")
                .contains("/js/sensitive-data-encryption.js")
                .doesNotContain(
                        "th:field=\"*{aadhaar}\"",
                        "th:field=\"*{pan}\"",
                        "name=\"aadhaar\"",
                        "name=\"pan\"");
        assertThat(script)
                .contains("const aadhaarInput = document.getElementById(\"aadhaar\")")
                .contains("endpoint: null")
                .doesNotContain("validate-aadhaar", "validate-pan");
        assertThat(controller).doesNotContain("/pre/validate-aadhaar", "/pre/validate-pan");
    }

    @Test
    void employeePanUsesEncryptedAjaxPayloadAndMaskedDisplay() throws Exception {
        String template = Files.readString(modulePath(
                "maharecruitment-web",
                "src/main/resources/templates/employee/dashboard.html"));
        String dashboardScript = Files.readString(modulePath(
                "maharecruitment-web",
                "src/main/resources/static/js/employee-dashboard.js"));
        String encryptionScript = Files.readString(modulePath(
                "maharecruitment-web",
                "src/main/resources/static/js/sensitive-data-encryption.js"));

        assertThat(template)
                .contains("data-sensitive-purpose=\"EMPLOYEE_PROFILE\"")
                .contains("data-sensitive-encrypted-name=\"panNoEncrypted\"")
                .contains("data-sensitive-field=\"panNo\"")
                .contains("@{/js/sensitive-data-encryption.js}")
                .doesNotContain("name=\"panNo\"", "th:value=\"${profile.panNo}\"");
        assertThat(dashboardScript)
                .contains("SensitiveDataEncryption.createEncryptedFormData(form)")
                .contains("body: encryptedFormData")
                .doesNotContain("formData.get('panNo')");
        assertThat(encryptionScript)
                .contains("createEncryptedFormData: createEncryptedFormData")
                .contains("formData.append(item.name, String(item.value))");
    }

    @Test
    void departmentProfileTanUsesEncryptedTransport() throws Exception {
        String template = Files.readString(modulePath(
                "maharecruitment-department",
                "src/main/resources/templates/department/profile-edit.html"));

        assertThat(template)
                .contains("data-sensitive-purpose=\"DEPARTMENT_PROFILE\"")
                .contains("data-sensitive-encrypted-name=\"tanNumberEncrypted\"")
                .contains("data-sensitive-field=\"tanNumber\"")
                .contains("/js/sensitive-data-encryption.js")
                .doesNotContain("th:field=\"*{tanNumber}\"", "name=\"tanNumber\"");
    }

    private Path modulePath(String module, String relativePath) {
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path fromRepositoryRoot = userDir.resolve(module).resolve(relativePath);
        if (Files.exists(fromRepositoryRoot)) {
            return fromRepositoryRoot;
        }
        if (userDir.getFileName() != null && module.equals(userDir.getFileName().toString())) {
            return userDir.resolve(relativePath);
        }
        Path parent = userDir.getParent();
        return parent == null ? fromRepositoryRoot : parent.resolve(module).resolve(relativePath);
    }
}
