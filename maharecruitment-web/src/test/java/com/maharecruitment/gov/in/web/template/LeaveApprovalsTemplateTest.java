package com.maharecruitment.gov.in.web.template;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class LeaveApprovalsTemplateTest {

    @Test
    void bothTabsShowCountsIncludingZeroRegardlessOfSelectedTab() throws IOException {
        String template = new ClassPathResource("templates/attendance/leave-approvals.html")
                .getContentAsString(StandardCharsets.UTF_8);
        String tabs = template.substring(template.indexOf("<ul class=\"nav nav-tabs\""),
                template.indexOf("</ul>", template.indexOf("<ul class=\"nav nav-tabs\"")) + 5);
        var engine = new org.thymeleaf.spring6.SpringTemplateEngine();
        engine.setTemplateResolver(new org.thymeleaf.templateresolver.StringTemplateResolver());
        var request = new org.springframework.mock.web.MockHttpServletRequest("GET", "/portal/hod1/leaveApprovals");
        request.setContextPath("/portal");
        var application = org.thymeleaf.web.servlet.JakartaServletWebApplication.buildApplication(
                new org.springframework.mock.web.MockServletContext());
        var context = new org.thymeleaf.context.WebContext(application.buildExchange(request,
                new org.springframework.mock.web.MockHttpServletResponse()));
        context.setVariable("searchQuery", "Asha");
        context.setVariable("searchDate", null);
        context.setVariable("pendingLeaves", java.util.List.of());
        context.setVariable("pendingTours", java.util.List.of("tour1", "tour2"));
        for (String tab : java.util.List.of("leave", "tour", "history")) {
            context.setVariable("activeTab", tab);
            assertThat(engine.process(tabs, context))
                    .contains("aria-label=\"Pending leave requests matching the current filters\">0</span>",
                            "aria-label=\"Pending tour requests matching the current filters\">2</span>",
                            "/portal/hod1/leaveApprovals?activeTab=leave")
                    .doesNotContain("th:if", "th:text");
        }
    }

    @Test
    void pendingRequestsWrapReasonsAndUseResponsiveLabels() throws IOException {
        String template = new ClassPathResource("templates/attendance/leave-approvals.html")
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(template)
                .contains("pending-approval-table leave-approval-table")
                .contains("class=\"reason-column\" data-label=\"Reason\"")
                .contains("class=\"request-reason small\"")
                .contains("overflow-wrap: anywhere")
                .contains("content: attr(data-label)")
                .doesNotContain("small text-truncate\" style=\"max-width: 200px;");
    }

    @Test
    void approvalHistoryUsesServerPaginationAndNameDateSearch() throws IOException {
        String template = new ClassPathResource("templates/attendance/leave-approvals.html")
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(template)
                .contains("name=\"query\"")
                .contains("name=\"searchDate\"")
                .contains("leaveHistoryPage.totalPages")
                .contains("tourHistoryPage.totalPages")
                .contains("leavePage=${leaveHistoryPage.number + 1}")
                .contains("tourPage=${tourHistoryPage.number + 1}")
                .doesNotContain("data-bs-toggle=\"tab\"");
    }
}
