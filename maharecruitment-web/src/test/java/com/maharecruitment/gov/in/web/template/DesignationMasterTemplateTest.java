package com.maharecruitment.gov.in.web.template;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class DesignationMasterTemplateTest {

    private static final Path FORM = Path.of(
            "src/main/resources/templates/master/designations/form.html");
    private static final Path LIST = Path.of(
            "src/main/resources/templates/master/designations/list.html");

    @Test
    void formProvidesOtherAndMahaitOnRollTypes() throws Exception {
        assertThat(Files.readString(FORM))
                .contains("th:field=\"*{designationType}\"")
                .contains("value=\"O\">O - Other")
                .contains("value=\"M\">M - MAHAIT On Roll");
    }

    @Test
    void listDisplaysDesignationType() throws Exception {
        assertThat(Files.readString(LIST))
                .contains("<th>Designation Type</th>")
                .contains("item.designationType.displayName")
                .contains("colspan=\"7\"");
    }
}
