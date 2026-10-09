package com.maharecruitment.gov.in.web.template;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class CommonCalendarTemplateTest {

    @Test
    void commonCalendarSupportsHolidayFestivalAndEventManagement() throws IOException {
        String template = new ClassPathResource("templates/attendance/holiday-list.html")
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(template)
                .contains("Holiday, Festival, Event &amp; Working Day Calendar")
                .contains("th:field=\"*{entryType}\"")
                .contains("type='HOLIDAY'")
                .contains("type='FESTIVAL'")
                .contains("type='EVENT'")
                .contains("th:action=\"@{/common/holidays/calendar-entries}\"")
                .contains("th:field=\"*{photos}\"")
                .contains("multiple")
                .contains("/photos/{photoId}/archive")
                .contains("th:action=\"@{/common/holidays/working-days}\"")
                .contains("/common/holidays/working-days/{id}/archive")
                .contains("th:field=\"*{officeOrderFile}\"")
                .contains("monthlyWorkingDayCount")
                .contains("/common/holidays/holidays/{id}/archive")
                .contains("/common/holidays/entries/{id}/archive")
                .doesNotContain("href=\"@{/common/holidays/archive/");

        int formStart = template.indexOf(
                "<form autocomplete=\"off\" th:action=\"@{/common/holidays/calendar-entries}\"");
        int formEnd = template.indexOf("</form>", formStart);
        assertThat(formStart).isGreaterThanOrEqualTo(0);
        assertThat(formEnd).isGreaterThan(formStart);
        assertThat(template.indexOf("#fields.hasAnyErrors()"))
                .as("Spring field helpers must be evaluated inside the bound form")
                .isBetween(formStart, formEnd);

        int workingDayFormStart = template.indexOf(
                "<form autocomplete=\"off\" th:action=\"@{/common/holidays/working-days}\"");
        int workingDayFormEnd = template.indexOf("</form>", workingDayFormStart);
        assertThat(workingDayFormStart).isGreaterThanOrEqualTo(0);
        assertThat(workingDayFormEnd).isGreaterThan(workingDayFormStart);
    }
}
