package com.maharecruitment.gov.in.web.template;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class LeaveApprovalsTemplateTest {

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
