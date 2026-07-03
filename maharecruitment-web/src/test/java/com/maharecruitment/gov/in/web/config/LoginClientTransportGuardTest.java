package com.maharecruitment.gov.in.web.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class LoginClientTransportGuardTest {

    @Test
    void loginJavascriptBlocksCredentialSubmissionOnNonLocalHttpPages() throws Exception {
        String loginScript = Files.readString(loginScriptPath());
        String loginTemplate = Files.readString(loginTemplatePath());

        assertThat(loginScript)
                .contains("window.location.protocol !== \"https:\"")
                .contains("isLoopbackHost(window.location.hostname)")
                .contains("normalizedHostname === \"localhost\"")
                .contains("normalizedHostname === \"127.0.0.1\"")
                .contains("HTTPS is required before submitting credentials.")
                .contains("passwordLoginForm.addEventListener(\"submit\"")
                .contains("event.preventDefault();")
                .contains("Select Email OTP or Mobile OTP.")
                .contains("Math.max(otpExpirySeconds, otpResendCooldownSeconds)")
                .contains("response.status === 429")
                .contains("Please enter the latest valid OTP")
                .contains("window.crypto.subtle.encrypt")
                .contains("encryptCredential(passwordInput.value")
                .contains("window.HTMLFormElement.prototype.submit.call(passwordLoginForm)");
        assertThat(loginTemplate)
                .contains("id=\"encryptedPassword\" name=\"password\"")
                .contains("data-credential-key-url=@{/security/credential-encryption/public-key}")
                .contains("@{/js/login-otp.js(v='20260625-credential-encryption')}")
                .doesNotContain("id=\"password\" name=\"password\"");
    }

    private Path loginScriptPath() throws IOException {
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path modulePath = userDir.resolve("src/main/resources/static/js/login-otp.js");
        if (Files.isRegularFile(modulePath)) {
            return modulePath;
        }
        return userDir.resolve("maharecruitment-web/src/main/resources/static/js/login-otp.js");
    }

    private Path loginTemplatePath() throws IOException {
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path modulePath = userDir.resolve("src/main/resources/templates/login.html");
        if (Files.isRegularFile(modulePath)) {
            return modulePath;
        }
        return userDir.resolve("maharecruitment-web/src/main/resources/templates/login.html");
    }
}
