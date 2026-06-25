package com.maharecruitment.gov.in.web.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class CredentialClientEncryptionTemplateTest {

    @Test
    void commonPasswordFormsUseClientSideCredentialEncryption() throws Exception {
        String headerTemplate = Files.readString(projectPath("src/main/resources/templates/header/header.html"));
        String adminUserForm = Files.readString(projectPath("src/main/resources/templates/admin/users/form.html"));
        String profileTemplate = Files.readString(projectPath("src/main/resources/templates/common/profile.html"));
        String encryptionScript = Files.readString(projectPath("src/main/resources/static/js/credential-encryption.js"));

        assertThat(headerTemplate).contains("@{/js/credential-encryption.js(v='20260625')}");
        assertThat(adminUserForm)
                .contains("data-encrypt-credentials=\"true\"")
                .contains("data-credential-key-url=@{/security/credential-encryption/public-key}");
        assertThat(profileTemplate)
                .contains("data-encrypt-credentials=\"true\"")
                .contains("data-credential-key-url=@{/security/credential-encryption/public-key}");
        assertThat(encryptionScript)
                .contains("input.removeAttribute(\"name\")")
                .contains("window.crypto.subtle.encrypt")
                .contains("window.HTMLFormElement.prototype.submit.call(form)");
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
