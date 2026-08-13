package com.maharecruitment.gov.in.web.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class AgencyMasterSensitiveTransportTemplateTest {

    @Test
    void agencyFormMasksAndEncryptsIdentityFieldsBeforeMultipartSubmission() throws Exception {
        String template = Files.readString(projectPath(
                "src/main/resources/templates/master/agencies/form.html"));
        String departmentTemplate = Files.readString(projectPath(
                "src/main/resources/templates/register/department-registration.html"));
        String script = Files.readString(projectPath(
                "src/main/resources/static/js/sensitive-data-encryption.js"));

        assertThat(template)
                .contains("data-encrypt-sensitive=\"true\"")
                .contains("data-sensitive-purpose=\"AGENCY_MASTER\"")
                .contains("data-sensitive-values-required=${!isEdit}")
                .contains("data-sensitive-encrypted-name=\"panNumberEncrypted\"")
                .contains("data-sensitive-encrypted-name=\"gstNumberEncrypted\"")
                .contains("data-sensitive-encrypted-name=\"bankAccountNumberEncrypted\"")
                .contains("data-sensitive-field=\"panNumber\"")
                .contains("data-sensitive-field=\"gstNumber\"")
                .contains("data-sensitive-field=\"bankAccountNumber\"")
                .contains("type=\"text\" class=\"form-control text-uppercase\"")
                .contains("id=\"bankAccountNumber\"")
                .contains("Leave blank to keep existing PAN")
                .contains("Leave blank to keep existing GST")
                .contains("Leave blank to keep existing account number")
                .contains("@{/js/sensitive-data-encryption.js(v='20260813-agency')}")
                .doesNotContain(
                        "th:field=\"*{panNumber}\"",
                        "th:field=\"*{gstNumber}\"",
                        "th:field=\"*{bankAccountNumber}\"",
                        "name=\"panNumber\"",
                        "name=\"gstNumber\"",
                        "name=\"bankAccountNumber\"");

        assertThat(departmentTemplate)
                .contains("type=\"password\" class=\"form-control text-uppercase\" id=\"tanNo\"")
                .contains("data-sensitive-encrypted-name=\"tanNumberEncrypted\"")
                .doesNotContain("th:field=\"*{tanNo}\"");

        assertThat(script)
                .contains("disableSensitiveFields(sensitiveFields)")
                .contains("field.disabled = true")
                .contains("body: new FormData(form)")
                .contains("input.autocomplete = \"off\"")
                .contains("restoreSensitiveFields(form)")
                .doesNotContain("encryptedFields.forEach(function (item) { item.field.value = \"\"; })");
    }

    private Path projectPath(String relativePath) {
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path direct = userDir.resolve(relativePath);
        return Files.exists(direct) ? direct : userDir.resolve("maharecruitment-web").resolve(relativePath);
    }
}
