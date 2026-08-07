package com.maharecruitment.gov.in.web.template;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

class HRDashboardAttendanceTemplateTest {

    private static final Path TEMPLATE_PATH = Path.of(
            "src/main/resources/templates/hr/hr_dashboard.html");

    @Test
    void attendancePanelRendersCheckInCountsAndTimeBoundaries() throws Exception {
        String template = Files.readString(TEMPLATE_PATH);
        int panelStart = template.indexOf("<section class=\"panel attendance-breakdown-panel");
        int panelEnd = template.indexOf("<section class=\"panel project-scope-panel", panelStart);
        String panel = template.substring(panelStart, panelEnd);

        StringTemplateResolver resolver = new StringTemplateResolver();
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCacheable(false);
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        Context context = new Context();
        context.setVariable("checkedInEmployees", 8);
        context.setVariable("earlyCheckIns", 3);
        context.setVariable("standardCheckIns", 4);
        context.setVariable("lateCheckIns", 1);

        String rendered = engine.process(panel, context);

        assertThat(rendered)
                .contains("Early Check-ins")
                .contains("Before 9:45 AM")
                .contains(">3</strong>")
                .contains("Regular Check-ins")
                .contains("9:45 AM to 10:15 AM")
                .contains(">4</strong>")
                .contains("Late Check-ins")
                .contains("After 10:15 AM")
                .contains(">1</strong>");
        assertThat(template)
                .contains("data-attendance-toggle")
                .contains("aria-controls=\"hrAttendanceDetails\"")
                .contains("setAttendanceDetailsVisible");
    }
}
