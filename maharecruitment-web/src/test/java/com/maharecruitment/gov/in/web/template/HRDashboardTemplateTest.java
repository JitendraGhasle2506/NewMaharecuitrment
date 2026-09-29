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

import com.maharecruitment.gov.in.web.service.dashboard.model.DepartmentOnboardingView;
import com.maharecruitment.gov.in.web.service.dashboard.model.HRAttendanceSummaryView;
import com.maharecruitment.gov.in.web.service.dashboard.model.HRDashboardView;
import com.maharecruitment.gov.in.web.service.dashboard.model.ProjectScopeListItemView;

class HRDashboardTemplateTest {

    private static final Path DASHBOARD_TEMPLATE_PATH = Path.of(
            "src/main/resources/templates/hr/hr_dashboard.html");

    @Test
    void summaryCardsAndSectionsRenderDashboardValues() throws Exception {
        String rendered = render(new HRDashboardView(
                9, 4, 5, 6, 30, 15, 5, 50, 40, 10, 22, 8, 10, 80,
                new HRAttendanceSummaryView(38, 10, 20, 6, 2),
                3, 7, 12, 60, 40,
                List.of(new DepartmentOnboardingView("Finance Department", 5, 10)),
                List.of(new ProjectScopeListItemView("INT-1", "Internal Portal")),
                List.of(new ProjectScopeListItemView("EXT-1", "External Portal"))));

        assertThat(rendered)
                .contains("Total Employees")
                .contains("Internal Employees")
                .contains("External Employees")
                .contains("MAHAIT Employees")
                .contains("Onboarded This Month")
                .contains("Total Cells")
                .contains("Present Today")
                .contains("/ 50")
                .contains("80% attendance")
                .contains("width:80%")
                .contains("Team Hierarchy")
                .contains("Attendance Today")
                .contains("Internal Present")
                .contains("MAHAIT Present")
                .contains("External Present")
                .contains("Absent")
                .contains("Internal Portal");
    }

    @Test
    void emptySectionsShowNoDataAvailable() throws Exception {
        String rendered = render(new HRDashboardView(
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                new HRAttendanceSummaryView(0, 0, 0, 0, 0),
                0, 0, 0, 0, 0,
                List.of(), List.of(), List.of()));

        // Internal projects, external projects, and attendance are the active empty-state sections.
        assertThat(rendered.split("No data available", -1).length - 1).isEqualTo(3);
    }

    @Test
    void dashboardKeepsEmployeeAndProjectBreakdowns() throws Exception {
        String template = Files.readString(DASHBOARD_TEMPLATE_PATH);

        assertThat(template)
                .contains("th:href=\"@{/hr/employee-hierarchy}\"")
                .contains("id=\"hrEmployeeBreakdownToggle\"")
                .contains("aria-controls=\"hrEmployeeBreakdown\"")
                .contains("id=\"hrEmployeeBreakdown\"")
                .contains("id=\"hrProjectScopePanel\"")
                .contains("data-project-scope=\"internal\"")
                .contains("data-project-scope=\"external\"");
    }

    private String render(HRDashboardView dashboard) throws Exception {
        String template = Files.readString(DASHBOARD_TEMPLATE_PATH)
                .replaceAll("(?s)<!--.*?-->", "");
        int start = template.indexOf("<section class=\"hrd-kpi-grid\"");
        int end = template.lastIndexOf("</th:block>", template.indexOf("<script>", start));
        String page = template.substring(start, end)
                .replaceAll("\\s+th:href=\"[^\"]+\"", "");

        StringTemplateResolver resolver = new StringTemplateResolver();
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCacheable(false);
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        Context context = new Context();
        context.setVariable("dashboard", dashboard);
        return engine.process(page, context);
    }
}
