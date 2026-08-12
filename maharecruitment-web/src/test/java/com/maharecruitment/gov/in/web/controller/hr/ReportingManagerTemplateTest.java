package com.maharecruitment.gov.in.web.controller.hr;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.StringTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

class ReportingManagerTemplateTest {

    private static final Path TEMPLATE_PATH = Path.of(
            "src/main/resources/templates/hr/reporting-manager-mapping.html");

    @Test
    void inlineJavascriptIsValidThymeleafMarkup() throws Exception {
        String template = Files.readString(TEMPLATE_PATH);
        int scriptStart = template.indexOf("<script th:inline=\"javascript\">");
        int scriptEnd = template.indexOf("</script>", scriptStart) + "</script>".length();
        String inlineScript = template.substring(scriptStart, scriptEnd)
                .replace("/*[[@{/}]]*/", "");

        StringTemplateResolver resolver = new StringTemplateResolver();
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCacheable(false);
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);

        assertThatCode(() -> engine.process(inlineScript, new Context()))
                .doesNotThrowAnyException();
    }

    @Test
    void searchableDropdownsAreNotAttachedToClippingCardContainer() throws Exception {
        String template = Files.readString(TEMPLATE_PATH);

        assertThat(template)
                .contains("select.select2({")
                .doesNotContain("dropdownParent: select.closest('.card-body')");
    }
}
