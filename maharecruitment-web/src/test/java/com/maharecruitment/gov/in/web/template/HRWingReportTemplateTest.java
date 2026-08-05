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

import com.maharecruitment.gov.in.web.service.dashboard.model.HRCellReportView;
import com.maharecruitment.gov.in.web.service.dashboard.model.HREmployeeHierarchyView;
import com.maharecruitment.gov.in.web.service.dashboard.model.HRWingReportView;

class HRWingReportTemplateTest {

    private static final Path TEMPLATE_PATH = Path.of(
            "src/main/resources/templates/hr/wing_report_detail.html");

    @Test
    void hierarchyPanelRendersCellEmployeeAndSubordinateMarkup() throws Exception {
        String template = Files.readString(TEMPLATE_PATH);
        int panelStart = template.indexOf("<section class=\"panel wing-cell-detail-panel wing-hierarchy-panel\">");
        String panelTerminator = "                    </section>\r\n                </div>";
        int panelEnd = template.indexOf(panelTerminator, panelStart);
        if (panelEnd < 0) {
            panelTerminator = "                    </section>\n                </div>";
            panelEnd = template.indexOf(panelTerminator, panelStart);
        }
        String panel = template.substring(panelStart, panelEnd + "                    </section>".length());

        HREmployeeHierarchyView employee = new HREmployeeHierarchyView(
                10L,
                "EMP-010",
                "Asha Lead",
                "AL",
                "",
                "Project Lead",
                0,
                1,
                "",
                "CELL");
        HRCellReportView cell = new HRCellReportView(
                30L,
                "Field Operations Cell",
                2,
                1,
                100,
                100,
                List.of(employee));
        HRCellReportView secondCell = new HRCellReportView(
                31L,
                "Support Cell",
                0,
                0,
                0,
                0,
                List.of());
        HRWingReportView wing = new HRWingReportView(
                2L,
                "MAHAIT Project Cells",
                2,
                2,
                1,
                List.of(cell, secondCell));

        StringTemplateResolver resolver = new StringTemplateResolver();
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCacheable(false);
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        Context context = new Context();
        context.setVariable("wing", wing);

        String rendered = engine.process(panel, context);

        assertThat(rendered)
                .contains("Field Operations Cell")
                .contains("Asha Lead")
                .contains("Cell authority")
                .contains("--hierarchy-depth: 0")
                .contains("data-hierarchy-target=\"cellEmployees-30\"")
                .contains("aria-expanded=\"true\"")
                .contains("aria-expanded=\"false\"")
                .contains("id=\"collapseHierarchy\"")
                .doesNotContain("expandAllHierarchy");
    }
}
