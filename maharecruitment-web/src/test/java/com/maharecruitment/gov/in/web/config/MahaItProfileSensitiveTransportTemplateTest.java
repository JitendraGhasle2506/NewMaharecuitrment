package com.maharecruitment.gov.in.web.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class MahaItProfileSensitiveTransportTemplateTest {

    @Test
    void formEncryptsEveryStoredSensitiveIdentifierBeforeSubmission() throws Exception {
        String template = Files.readString(projectPath(
                "src/main/resources/templates/common/mahait-profile/form.html"));

        assertThat(template)
                .contains("data-encrypt-sensitive=\"true\"")
                .contains("data-sensitive-purpose=\"MAHAIT_PROFILE\"")
                .contains("data-sensitive-values-required=${!isEdit}")
                .contains("data-sensitive-encrypted-name=\"cinNumberEncrypted\"")
                .contains("data-sensitive-encrypted-name=\"panNumberEncrypted\"")
                .contains("data-sensitive-encrypted-name=\"gstNumberEncrypted\"")
                .contains("data-sensitive-encrypted-name=\"accountNumberEncrypted\"")
                .contains("data-sensitive-field=\"cinNumber\"")
                .contains("data-sensitive-field=\"panNumber\"")
                .contains("data-sensitive-field=\"gstNumber\"")
                .contains("data-sensitive-field=\"accountNumber\"")
                .contains("@{/js/sensitive-data-encryption.js(v='20260818-mahait-profile')}")
                .doesNotContain(
                        "th:field=\"*{cinNumber}\"",
                        "th:field=\"*{panNumber}\"",
                        "th:field=\"*{gstNumber}\"",
                        "th:field=\"*{accountNumber}\"",
                        "name=\"cinNumber\"",
                        "name=\"panNumber\"",
                        "name=\"gstNumber\"",
                        "name=\"accountNumber\"");
    }

    private Path projectPath(String relativePath) {
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path direct = userDir.resolve(relativePath);
        return Files.exists(direct) ? direct : userDir.resolve("maharecruitment-web").resolve(relativePath);
    }
}
