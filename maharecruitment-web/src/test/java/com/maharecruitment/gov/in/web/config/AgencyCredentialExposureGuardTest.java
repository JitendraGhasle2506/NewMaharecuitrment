package com.maharecruitment.gov.in.web.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class AgencyCredentialExposureGuardTest {

    @Test
    void agencySuccessBannerDoesNotExposeUsernameOrTemporaryPassword() throws Exception {
        String controller = Files.readString(projectPath(
                "src/main/java/com/maharecruitment/gov/in/web/controller/master/AgencyMasterPageController.java"));
        String listTemplate = Files.readString(projectPath(
                "src/main/resources/templates/master/agencies/list.html"));

        assertThat(controller)
                .contains("Login credentials have been sent to the official email address")
                .doesNotContain(
                        "response.getProvisionedUserEmail()",
                        "response.getTemporaryPassword()",
                        "Agency User Created:",
                        "Username: ",
                        "Password: ");
        assertThat(listTemplate)
                .contains("th:text=\"${successMessage}\"")
                .doesNotContain("th:utext=\"${successMessage}\"");
    }

    private Path projectPath(String relativePath) {
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path direct = userDir.resolve(relativePath);
        return Files.exists(direct) ? direct : userDir.resolve("maharecruitment-web").resolve(relativePath);
    }
}
