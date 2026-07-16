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
                .contains("data-sensitive-field=\"gstNumber\"")
                .contains("data-sensitive-field=\"panNumber\"")
                .contains("data-sensitive-purpose=\"DEPARTMENT_REGISTRATION\"")
                .contains("@{/js/sensitive-data-encryption.js}")
                .doesNotContain("th:field=\"*{gstNo}\"")
                .doesNotContain("th:field=\"*{panNo}\"")
                .doesNotContain("name=\"gstNo\"")
                .doesNotContain("name=\"panNo\"")
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
                .doesNotContain("includes(\"X\")");
    }

    private Path projectPath(String relativePath) {
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path direct = userDir.resolve(relativePath);
        return Files.exists(direct) ? direct : userDir.resolve("maharecruitment-web").resolve(relativePath);
    }
}
