package com.maharecruitment.gov.in.web.template;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class InfraAssetsTemplateTest {

    @Test
    void usesCspCompatibleExternalInteractionsForEveryAssetView() throws IOException {
        String template = new ClassPathResource("templates/infra/utilities.html")
                .getContentAsString(StandardCharsets.UTF_8);
        String interactions = new ClassPathResource("static/js/infra-assets.js")
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(template)
                .contains("data-asset-view-target=\"addCategoryView\"")
                .contains("data-asset-view-target=\"addAssetView\"")
                .contains("data-asset-view-target=\"allocateAssetView\"")
                .contains("data-asset-return")
                .contains("@{/js/infra-assets.js(v='20261009-1')}")
                .doesNotContain("onclick=", "th:inline=\"javascript\"", "style=\"");

        assertThat(interactions)
                .contains("[data-asset-view-target]")
                .contains("[data-asset-return]")
                .contains("showView('returnAssetView')");
    }
}
