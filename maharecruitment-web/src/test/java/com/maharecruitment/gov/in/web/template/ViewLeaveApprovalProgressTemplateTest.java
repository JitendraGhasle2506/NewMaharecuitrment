package com.maharecruitment.gov.in.web.template;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class ViewLeaveApprovalProgressTemplateTest {

    @Test
    void leaveHistoryDisplaysWhoDecidedTheLeaveWithDesignation() throws IOException {
        String template = new ClassPathResource("templates/attendance/view-leave.html")
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(template)
                .contains("Approved / Rejected By")
                .doesNotContain("Approval Progress")
                .doesNotContain("approval-node")
                .contains("leave.decidedByName")
                .contains("leave.decidedByDesignation")
                .contains("leave.managerRemarks")
                .contains("leave.hodRemarks")
                .contains("Awaiting approval")
                .contains("Approver not recorded");
    }

    @Test
    void tourHistoryDisplaysManagerAndHodApprovalProgress() throws IOException {
        String template = new ClassPathResource("templates/attendance/view-tour.html")
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(template)
                .contains("Approval Progress")
                .contains("Reporting Manager")
                .contains("Reporting HOD")
                .contains("Waiting for Manager")
                .contains("approval-node")
                .contains("approval-dot")
                .contains("approval-tooltip")
                .doesNotContain("approval-summary")
                .contains("tour.managerApproverName")
                .contains("tour.hodApproverName")
                .contains("tour.managerRemarks")
                .contains("tour.hodRemarks");
    }
}
