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
        String inventory = new ClassPathResource("templates/infra/assets/inventory.html")
                .getContentAsString(StandardCharsets.UTF_8);
        String allocations = new ClassPathResource("templates/infra/assets/allocations.html")
                .getContentAsString(StandardCharsets.UTF_8);
        String allocationInteractions = new ClassPathResource("static/js/infra-asset-allocations.js")
                .getContentAsString(StandardCharsets.UTF_8);
        String allocationStyles = new ClassPathResource("static/css/infra-asset-allocations.css")
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(template)
                .contains("data-asset-view-target=\"addCategoryView\"")
                .contains("data-asset-view-target=\"addAssetView\"")
                .contains("@{/infra/utilities/assets/inventory}")
                .contains("@{/infra/utilities/assets/allocations}")
                .contains("name=\"ownershipType\"")
                .contains("name=\"rentStartDate\"")
                .contains("name=\"rentEndDate\"")
                .contains("id=\"assetPurchaseDateField\"")
                .contains("id=\"assetWarrantyExpiryField\"")
                .contains("@{/js/infra-assets.js(v='20261012-3')}")
                .doesNotContain("Recent Asset Activity", "auditLogs", "allocateAssetView", "returnAssetView")
                .doesNotContain("onclick=", "th:inline=\"javascript\"", "style=\"");

        assertThat(interactions)
                .contains("[data-asset-view-target]")
                .contains("syncRentalFields")
                .contains("purchaseDateField?.classList.toggle('d-none', rented)")
                .doesNotContain("[data-asset-return]", "returnAssetView");

        assertThat(inventory)
                .contains("assetPage.content")
                .contains("assetPage.totalPages")
                .contains("/infra/utilities/assets/inventory(page=")
                .doesNotContain("Recent Asset Activity");
        assertThat(allocations)
                .contains("allocationPage.content")
                .contains("allocationPage.totalPages")
                .contains("/infra/utilities/assets/allocations(page=")
                .contains("name=\"search\"")
                .contains("Serial no., employee or category")
                .contains("search=${search}")
                .contains("employee.fullName + ' (' + employee.mobile + ')'", "allocation.employeeMobile")
                .doesNotContain("allocation.employeeCode", "employee.fullName + ' (' + employee.employeeCode + ')'")
                .contains("@{/infra/utilities/assets/replace}")
                .contains("data-asset-replace")
                .contains("id=\"replaceOldAssetName\"")
                .contains("id=\"replaceOldAssetCode\"")
                .contains("id=\"replaceOldOwnership\"")
                .contains("name=\"replacementReason\"")
                .contains("data-asset-return")
                .contains("id=\"returnAssetModal\"")
                .contains("modal-fullscreen-sm-down")
                .contains("@{/css/infra-asset-allocations.css(v='20261012-1')}")
                .contains("@{/js/infra-asset-allocations.js(v='20261012-2')}")
                .doesNotContain("onclick=", "th:inline=\"javascript\"")
                .doesNotContain("Recent Asset Activity");
        assertThat(allocationInteractions)
                .contains("[data-asset-replace]")
                .contains("[data-asset-return]")
                .contains("bootstrap.Modal.getOrCreateInstance")
                .doesNotContain("onclick=");
        assertThat(allocationStyles)
                .contains("@media (max-width: 991.98px)")
                .contains("content: attr(data-label)")
                .contains(".allocation-search-input");
    }
}
