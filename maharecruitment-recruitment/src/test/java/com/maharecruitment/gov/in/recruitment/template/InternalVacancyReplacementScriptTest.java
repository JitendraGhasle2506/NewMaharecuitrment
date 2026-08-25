package com.maharecruitment.gov.in.recruitment.template;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class InternalVacancyReplacementScriptTest {

    @Test
    void groupsSelectedEmployeesByDesignationAndLevelAndCountsVacancies() throws IOException {
        String script = new ClassPathResource("static/js/internal-vacancy-replacement.js")
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(script)
                .contains("const key = `${designationId}_${levelCode}`")
                .contains("? \"application/x-www-form-urlencoded\"")
                .contains(": \"multipart/form-data\"")
                .contains("group.vacancyCount += 1")
                .contains("tableBody.replaceChildren()")
                .contains("vacancyInput.readOnly = true")
                .contains("if (selectedEmployees().length > 0)")
                .contains("event.preventDefault()")
                .contains("searchInput?.addEventListener(\"input\", filterEmployeeOptions)")
                .contains("searchInput?.addEventListener(\"keydown\", preventSearchSubmit)")
                .contains("if (event.key === \"Enter\")")
                .contains("option.textContent.toLocaleLowerCase().includes(query)")
                .contains("noResults?.classList.toggle(\"d-none\", matches > 0)")
                .contains("clearSelectionButton?.addEventListener(\"click\", clearEmployeeSelection)");
    }
}
