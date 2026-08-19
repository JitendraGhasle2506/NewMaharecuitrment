package com.maharecruitment.gov.in.web.template;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class EmployeeDashboardPhotoUploadTemplateTest {

    @Test
    void photoFormPostsMultipartWithCsrfAndAjaxResponseHandling() throws IOException {
        String template = resource("templates/employee/dashboard.html");
        String script = resource("static/js/employee-dashboard.js");

        assertThat(template)
                .contains("id=\"photoUploadForm\" method=\"post\" enctype=\"multipart/form-data\"")
                .contains("th:action=\"@{/employee/profile/photo/upload}\"")
                .contains("th:name=\"${_csrf.parameterName}\"")
                .contains("employee-dashboard.js(v='20260818')");
        assertThat(script)
                .contains("'X-Requested-With': 'XMLHttpRequest'")
                .contains("'Accept': 'application/json'")
                .contains("credentials: 'same-origin'")
                .contains("responsePayload(response)")
                .contains("new FormData(photoForm)");
    }

    private String resource(String path) throws IOException {
        return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
    }
}
