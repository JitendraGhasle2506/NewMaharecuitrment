package com.maharecruitment.gov.in.web.template;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class ApplyLeaveTemplateTest {

    @Test
    void backButtonReturnsToEmployeeAttendanceRegister() throws IOException {
        ClassPathResource resource =
                new ClassPathResource("templates/attendance/apply-leave.html");
        String template = resource.getContentAsString(StandardCharsets.UTF_8);

        assertThat(template)
                .contains("th:href=\"@{/employee/intAttendance}\" class=\"btn btn-back\"")
                .doesNotContain("@{/employee/attendance-module}");
    }
}
