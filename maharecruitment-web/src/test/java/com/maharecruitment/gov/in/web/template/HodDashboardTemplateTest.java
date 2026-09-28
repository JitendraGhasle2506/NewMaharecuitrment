package com.maharecruitment.gov.in.web.template;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import com.maharecruitment.gov.in.web.service.dashboard.HodDashboardService.CellView;
import com.maharecruitment.gov.in.web.service.dashboard.HodDashboardService.EmployeeView;
import com.maharecruitment.gov.in.web.service.dashboard.HodDashboardService.HodDashboardView;
import com.maharecruitment.gov.in.web.service.dashboard.HodDashboardService.PresentEmployeeView;

class HodDashboardTemplateTest {
    private static final Path TEMPLATE = Path.of("src/main/resources/templates/role/hod_dashboard.html");

    @Test
    void cellCountExpandsANameAndDesignationOnlyEmployeeList() throws Exception {
        EmployeeView employee = new EmployeeView(
                "EMP001", "Asha Patil", "Developer", "asha@example.test", "INTERNAL", "ACTIVE",
                "Applications Cell", "-");
        CellView cell = new CellView(7L, "Applications Cell", "Technology", 1, 0, List.of(employee));
        HodDashboardView dashboard = new HodDashboardView(
                1, 1, 0, 0, 1, List.of(employee), List.of(cell), List.of(), List.of(), List.of(employee));

        String page = Files.readString(TEMPLATE);
        int start = page.indexOf("<section class=\"tab-pane fade hod-detail-panel\" id=\"cells-panel\"");
        int end = page.indexOf("</section>", start) + "</section>".length();
        Context context = new Context();
        context.setVariable("dashboard", dashboard);

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(new StringTemplateResolver());
        String html = engine.process(page.substring(start, end), context);

        assertThat(html)
                .contains("data-bs-target=\"#cell-employees-7\"")
                .contains("aria-controls=\"cell-employees-7\"")
                .contains("Asha Patil", "Developer", "<th scope=\"col\">Name</th>",
                        "<th scope=\"col\">Designation</th>")
                .doesNotContain("asha@example.test", "EMP001");
    }

    @Test
    void presentCardShowsAttendanceTimesAndSearchesNameOrDesignation() throws Exception {
        EmployeeView employee = new EmployeeView(
                "EMP001", "Asha Patil", "Developer", "asha@example.test", "INTERNAL", "ACTIVE",
                "Applications Cell", "Citizen Portal");
        PresentEmployeeView presentEmployee = new PresentEmployeeView(
                "Asha Patil", "Developer", "09:12", "18:07");
        HodDashboardView dashboard = new HodDashboardView(
                1, 0, 0, 1, 0, List.of(employee), List.of(), List.of(), List.of(presentEmployee), List.of());

        String page = Files.readString(TEMPLATE);
        int start = page.indexOf("<section class=\"tab-pane fade hod-detail-panel\" id=\"present-panel\"");
        int end = page.indexOf("</section>", start) + "</section>".length();
        Context context = new Context();
        context.setVariable("dashboard", dashboard);

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(new StringTemplateResolver());
        String html = engine.process(page.substring(start, end), context);

        assertThat(html)
                .contains("id=\"presentEmployeeSearch\"", "placeholder=\"Search name or designation\"",
                        "<th scope=\"col\">Employee Name</th>", "<th scope=\"col\">Designation</th>",
                        "<th scope=\"col\">In Time</th>", "<th scope=\"col\">Out Time</th>",
                        "data-search=\"asha patil developer\"", "Asha Patil", "Developer", "09:12", "18:07")
                .doesNotContain("asha@example.test", "EMP001", "Applications Cell", "Citizen Portal");
        assertThat(page).contains("presentEmployeeSearch?.addEventListener('input'");
    }
}
