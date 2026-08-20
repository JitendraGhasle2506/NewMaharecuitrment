package com.maharecruitment.gov.in.web.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AgencyProfileSensitiveDisplayTemplateTest {

    @Test
    void agencyProfileUsesAccessibleOnDemandSensitiveIdentifierControls() throws Exception {
        String template = Files.readString(projectPath("src/main/resources/templates/agency/profile.html"));
        String script = Files.readString(projectPath("src/main/resources/static/js/agency-profile.js"));

        assertThat(template)
                .contains("data-sensitive-toggle=\"PAN\"")
                .contains("data-sensitive-toggle=\"GST\"")
                .contains("data-sensitive-toggle=\"CERTIFICATE\"")
                .contains("th:data-masked-value=\"*{panNumber}\"")
                .contains("th:data-masked-value=\"*{gstNumber}\"")
                .contains("th:data-masked-value=\"*{certificateNumber}\"")
                .contains("aria-pressed=\"false\"")
                .contains("@{/js/agency-profile.js(v='20260820-sensitive-identifiers')}")
                .doesNotContain("data-full-value", "data-sensitive-value");

        assertThat(script)
                .contains("TAN: \"TAN number\"")
                .contains("AUTO_MASK_DELAY_MS = 30000")
                .contains("\"X-App-Loader\": \"off\"")
                .contains("cache: \"no-store\"")
                .contains("document.hidden")
                .contains("buttons.filter")
                .contains("fa-eye-slash");
    }

    private Path projectPath(String relativePath) {
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path direct = userDir.resolve(relativePath);
        return Files.exists(direct) ? direct : userDir.resolve("maharecruitment-web").resolve(relativePath);
    }
}
