package com.maharecruitment.gov.in.web.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class UploadContentGuardTemplateTest {

    @Test
    void sharedHeaderLoadsUploadContentGuardForFileInputs() throws Exception {
        String headerTemplate = Files.readString(projectPath("src/main/resources/templates/header/header.html"));
        String uploadGuardScript = Files.readString(projectPath("src/main/resources/static/js/upload-content-guard.js"));

        assertThat(headerTemplate).contains("@{/js/upload-content-guard.js(v='20260625')}");
        assertThat(uploadGuardScript)
                .contains("Selected file is malicious because it contains script or active content.")
                .contains("input.type !== \"file\"")
                .contains("window.alert(MALICIOUS_FILE_MESSAGE)")
                .contains("/<\\s*\\/?\\s*script\\b/i")
                .contains("/\\/(JavaScript|JS|OpenAction|AA|Launch|RichMedia|EmbeddedFile)\\b/i");
    }

    private Path projectPath(String relativePath) throws IOException {
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path modulePath = userDir.resolve(relativePath);
        if (Files.exists(modulePath)) {
            return modulePath;
        }
        return userDir.resolve("maharecruitment-web").resolve(relativePath);
    }
}
