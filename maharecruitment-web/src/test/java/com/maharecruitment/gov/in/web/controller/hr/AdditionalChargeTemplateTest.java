package com.maharecruitment.gov.in.web.controller.hr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import com.maharecruitment.gov.in.master.entity.*;
import com.maharecruitment.gov.in.recruitment.repository.AdditionalChargeRepository.Choice;
import com.maharecruitment.gov.in.recruitment.service.AdditionalChargeService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.mock.web.*;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafView;
import org.thymeleaf.templateresolver.StringTemplateResolver;

class AdditionalChargeTemplateTest {
    static class TemplateView extends ThymeleafView {
        TemplateView(SpringTemplateEngine engine) { setTemplateEngine(engine); setLocale(Locale.ENGLISH); }
    }
    @Test void rendersFormRegisterEscapedTextAndContextAwareActions() throws Exception {
        String template = Files.readString(Path.of("src/main/resources/templates/hr/additional-charge.html"))
                .replaceAll("th:replace=\"[^\"]*\"", "");
        var servlet = new MockServletContext();
        var application = new StaticWebApplicationContext();
        application.setServletContext(servlet);
        application.refresh();
        servlet.setAttribute(org.springframework.web.context.WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, application);
        try {
            var engine = new SpringTemplateEngine();
            engine.setTemplateResolver(new StringTemplateResolver());
            var view = new TemplateView(engine);
            view.setApplicationContext(application);
            view.setTemplateName(template);
            var request = new MockHttpServletRequest(servlet, "GET", "/portal/hr/additional-charge");
            request.setContextPath("/portal");
            var response = new MockHttpServletResponse();
            var employee = mock(Choice.class);
            when(employee.getId()).thenReturn(1L);
            when(employee.getLabel()).thenReturn("Ananya Deshmukh (EMP001)");
            var entry = new AdditionalChargeService.Entry(4L, "Ananya Deshmukh", "HOD", "Finance",
                    LocalDate.of(2026, 10, 15), null, "ORD/12", "<script>alert(1)</script>", "Active", "hr@example.test", null, "Rohan Patil", 1);
            var replacement = mock(Choice.class);
            when(replacement.getId()).thenReturn(3L);
            when(replacement.getLabel()).thenReturn("Rohan Patil (EMP003)");
            var cell = new CellMaster();
            cell.setCellId(2L);
            cell.setCellName("Finance cell");
            var wing = new WingMaster();
            wing.setWingName("Administration");
            cell.setWing(wing);
            Map<String, Object> model = new HashMap<>();
            model.put("chargeForm", new AdditionalChargeService.Request("E:1", "ROLE_HOD", "C:2", LocalDate.of(2026, 10, 15), null, "", "", "E:3"));
            model.put("roles", List.of("ROLE_HOD", "ROLE_PM", "ROLE_STM"));
            model.put("employees", List.of(employee));
            model.put("users", List.of());
            model.put("cells", List.of(cell));
            model.put("replacementEmployees", List.of(employee, replacement));
            model.put("replacementUsers", List.of());
            model.put("charges", new PageImpl<>(List.of(entry)));
            view.render(model, request, response);
            String html = response.getContentAsString();
            assertThat(html).contains("action=\"/portal/hr/additional-charge\"", "value=\"2026-10-15\"",
                    "action=\"/portal/hr/additional-charge/4/revoke\"", "Ananya Deshmukh", "ac-active",
                    "&lt;script&gt;alert(1)&lt;/script&gt;", "value=\"ROLE_HOD\"", "value=\"ROLE_PM\"", "value=\"ROLE_STM\"")
                    .contains("Covering: Rohan Patil", "Team at assignment: 1 direct reports", "(optional)", "All cells")
                    .doesNotContain("<script>alert(1)</script>", "value=\"D:2\"", "Department or cell", "Cell *");
            Files.writeString(Path.of("target/additional-charge-preview.html"), html);
        } finally { application.close(); }
    }
}
