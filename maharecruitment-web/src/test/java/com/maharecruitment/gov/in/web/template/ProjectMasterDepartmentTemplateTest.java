package com.maharecruitment.gov.in.web.template;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class ProjectMasterDepartmentTemplateTest {

    @Test
    void formCapturesDepartmentAndFiltersSubDepartments() throws IOException {
        String template = template("form.html");

        assertThat(template)
                .contains("th:field=\"*{departmentId}\"")
                .contains("th:field=\"*{subDepartmentId}\"")
                .contains("data-subdepartments-url=@{/master/projects/sub-departments}")
                .contains("fetch(")
                .contains("encodeURIComponent(departmentId)")
                .contains("departmentSelect.addEventListener('change'")
                .contains("Select Sub Department (optional)");
    }

    @Test
    void listShowsDepartmentAndSubDepartmentNames() throws IOException {
        assertThat(template("list.html"))
                .contains("<th>Department</th>")
                .contains("<th>Sub Department</th>")
                .contains("${item.departmentName != null ? item.departmentName : '-'}")
                .contains("${item.subDepartmentName != null ? item.subDepartmentName : '-'}");
    }

    private String template(String fileName) throws IOException {
        ClassPathResource resource = new ClassPathResource("templates/master/projects/" + fileName);
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }
}
