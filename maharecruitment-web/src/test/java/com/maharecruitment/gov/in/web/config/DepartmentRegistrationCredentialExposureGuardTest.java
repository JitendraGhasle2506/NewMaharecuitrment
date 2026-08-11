package com.maharecruitment.gov.in.web.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class DepartmentRegistrationCredentialExposureGuardTest {

    @Test
    void registrationRedirectAndLoginPageDoNotExposeGeneratedCredentials() throws Exception {
        String controller = Files.readString(projectPath(
                "src/main/java/com/maharecruitment/gov/in/web/controller/DepartmentRegistrationPageController.java"));
        String loginTemplate = Files.readString(projectPath(
                "src/main/resources/templates/login.html"));

        assertThat(controller)
                .contains("redirectAttributes.addAttribute(\"registered\", \"true\")")
                .doesNotContain(
                        "addFlashAttribute(\"generatedUsername\"",
                        "addFlashAttribute(\"generatedPassword\"",
                        "result.temporaryPassword()",
                        "result.username()");
        assertThat(loginTemplate)
                .contains("Department registration submitted successfully.")
                .contains("Login credentials have been sent to your")
                .contains("registered email and mobile number.")
                .doesNotContain(
                        "generatedUsername",
                        "generatedPassword",
                        "Temporary Password:",
                        "Username: <strong");
    }

    private Path projectPath(String relativePath) {
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path direct = userDir.resolve(relativePath);
        return Files.exists(direct) ? direct : userDir.resolve("maharecruitment-web").resolve(relativePath);
    }
}
