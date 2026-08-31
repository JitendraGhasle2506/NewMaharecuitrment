package com.maharecruitment.gov.in.web.template;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class AgencyInternalAssessmentDetailTemplateTest {

    @Test
    void agencyDetailDoesNotRenderPanelIdentities() throws IOException {
        String template = new ClassPathResource("templates/agency/internal-assessment-detail.html")
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(template)
                .contains("'Panel ' + assessmentStat.count")
                .doesNotContain("assessmentItem.assessorName")
                .doesNotContain("assessmentItem.assessorType")
                .doesNotContain("detail.assessment.interviewAuthority")
                .doesNotContain("detail.assessment.panelMembers")
                .doesNotContain("member.panelMemberName");
    }
}
