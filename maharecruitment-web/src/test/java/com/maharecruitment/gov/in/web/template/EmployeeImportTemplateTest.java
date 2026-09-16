package com.maharecruitment.gov.in.web.template;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class EmployeeImportTemplateTest {

    private static final Path TEMPLATE = Path.of(
            "src/main/resources/templates/hr/employee-import.html");

    @Test
    void importInstructionsIncludeMahaitEmployeeType() throws Exception {
        assertThat(Files.readString(TEMPLATE))
                .contains("Internal, external, and MAHAIT employees")
                .contains("INTERNAL, EXTERNAL, or MAHAIT")
                .contains("mandatory for all employee types");
    }
}
