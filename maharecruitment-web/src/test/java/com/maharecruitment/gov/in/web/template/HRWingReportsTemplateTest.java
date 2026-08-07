package com.maharecruitment.gov.in.web.template;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import com.maharecruitment.gov.in.web.service.dashboard.model.HRWingDirectoryItemView;
import com.maharecruitment.gov.in.web.service.dashboard.model.HRWingReportsView;

class HRWingReportsTemplateTest {

    private static final Path WING_REPORTS_TEMPLATE = Path.of(
            "src/main/resources/templates/hr/hr_wing_reports.html");

    private static final Path DASHBOARD_TEMPLATE = Path.of(
            "src/main/resources/templates/hr/hr_dashboard.html");

    @Test
    void dedicatedWingReportsPageRendersSummaryAndDirectory() throws Exception {
        String template = Files.readString(WING_REPORTS_TEMPLATE);
        int pageStart = template.indexOf("<section class=\"wing-directory-page\"");
        int pageEnd = template.indexOf("</th:block>", pageStart);
        String page = template.substring(pageStart, pageEnd)
                .replace(" th:href=\"@{/hr/dashboard}\"", "")
                .replace(" th:href=\"@{/hr/wing-report/{wingId}(wingId=${wing.wingId})}\"", "");

        StringTemplateResolver resolver = new StringTemplateResolver();
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCacheable(false);
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        Context context = new Context();
        context.setVariable("wingDirectory", new HRWingReportsView(
                1,
                2,
                4,
                12,
                List.of(new HRWingDirectoryItemView(
                        2L,
                        "MAHAIT Project Cells",
                        2,
                        4,
                        12))));

        String rendered = engine.process(page, context);

        assertThat(rendered)
                .contains("Wing Performance Directory")
                .contains("MAHAIT Project Cells")
                .contains("2</b> Cells")
                .contains("4</b> Projects")
                .contains("12</b> Employees")
                .doesNotContain("hrWingReportDetails");
    }

    @Test
    void dashboardLinksToWingReportsWithoutEmbeddingDirectory() throws Exception {
        String dashboard = Files.readString(DASHBOARD_TEMPLATE);

        assertThat(dashboard)
                .contains("th:href=\"@{/hr/wing-reports}\"")
                .doesNotContain("dashboard.wingReports")
                .doesNotContain("id=\"hrWingReportDetails\"")
                .doesNotContain("id=\"hrWingReportLoad\"")
                .doesNotContain("data-wing-load-label");
    }
}
