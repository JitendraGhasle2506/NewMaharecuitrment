package com.maharecruitment.gov.in.web.template;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import com.maharecruitment.gov.in.web.service.dashboard.model.HRCellAttendanceView;
import com.maharecruitment.gov.in.web.service.dashboard.model.HRAttendanceSummaryView;
import com.maharecruitment.gov.in.web.service.dashboard.model.HRTodayAttendanceView;

class HRDashboardAttendanceTemplateTest {

    private static final Path TEMPLATE_PATH = Path.of(
            "src/main/resources/templates/hr/hr_attendance_today.html");

    private static final Path DASHBOARD_TEMPLATE_PATH = Path.of(
            "src/main/resources/templates/hr/hr_dashboard.html");

    @Test
    void dedicatedAttendancePageRendersCheckInAndCellCounts() throws Exception {
        String template = Files.readString(TEMPLATE_PATH);
        int pageStart = template.indexOf("<section class=\"attendance-page\"");
        int pageEnd = template.indexOf("</th:block>", pageStart);
        String page = template.substring(pageStart, pageEnd)
                .replace(" th:href=\"@{/hr/dashboard}\"", "");

        StringTemplateResolver resolver = new StringTemplateResolver();
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCacheable(false);
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        Context context = new Context();
        context.setVariable("attendance", new HRTodayAttendanceView(
                LocalDate.of(2026, 8, 7),
                20,
                11,
                9,
                55,
                new HRAttendanceSummaryView(8, 3, 4, 1),
                List.of(new HRCellAttendanceView(
                        27L,
                        "Network Infra Cell",
                        "MAHAIT Project Cells",
                        12,
                        9,
                        3,
                        75))));

        String rendered = engine.process(page, context);

        assertThat(rendered)
                .contains("Early Check-ins")
                .contains("Before 9:45 AM")
                .contains(">3</strong>")
                .contains("Regular Check-ins")
                .contains("9:45 AM to 10:15 AM")
                .contains(">4</strong>")
                .contains("Late Check-ins")
                .contains("After 10:15 AM")
                .contains(">1</strong>")
                .contains("Cell-wise attendance")
                .contains("Present and Absent")
                .contains("07 Aug 2026")
                .contains("Network Infra Cell")
                .contains("75% present");
    }

    @Test
    void dashboardLinksToDedicatedAttendancePageWithoutEmbeddingDetails() throws Exception {
        String dashboardTemplate = Files.readString(DASHBOARD_TEMPLATE_PATH);

        assertThat(dashboardTemplate)
                .contains("th:href=\"@{/hr/attendance-today}\"")
                .doesNotContain("id=\"hrAttendanceDetails\"")
                .doesNotContain("data-attendance-toggle")
                .doesNotContain("setAttendanceDetailsVisible");
    }
}
