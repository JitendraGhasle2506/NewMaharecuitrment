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
                .contains("Enter a valid email address or 10 digit mobile number.")
                .contains("detectIdentifierChannel")
                .contains("Math.max(0, otpResendCooldownSeconds)")
                .contains("form.dataset.otpResendCooldownSeconds || \"120\"")
                .contains("data.resendAvailableInSeconds")
                .contains("response.status === 429")
                .contains("Please enter the latest valid OTP")
                .contains("window.crypto.subtle.encrypt")
                .contains("encryptCredential(passwordInput.value")
                .contains("window.HTMLFormElement.prototype.submit.call(passwordLoginForm)");
        assertThat(loginTemplate)
                .contains("id=\"encryptedPassword\" name=\"password\"")
                .contains("id=\"otpChannel\" name=\"channel\"")
                .contains("id=\"otpChannelDisplay\"")
                .contains("data-email-otp-enabled=${otpEmailEnabled}")
                .contains("data-sms-otp-enabled=${otpSmsEnabled}")
                .contains("data-credential-key-url=@{/security/credential-encryption/public-key}")
                .contains("otpResendCooldownSecondsRemaining != null")
                .contains("@{/js/login-otp.js(v='20260819-otp-policy')}")
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
