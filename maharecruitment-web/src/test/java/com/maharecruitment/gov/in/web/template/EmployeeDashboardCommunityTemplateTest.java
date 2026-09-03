package com.maharecruitment.gov.in.web.template;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class EmployeeDashboardCommunityTemplateTest {

    @Test
    void rendersCelebrationsUpcomingEventsAnnouncementsAndTheirEmptyStates() throws IOException {
        String template = new ClassPathResource("templates/employee/dashboard.html")
                .getContentAsString(StandardCharsets.UTF_8);
        String interactions = new ClassPathResource("static/js/employee-dashboard-community.js")
                .getContentAsString(StandardCharsets.UTF_8);
        String stylesheet = new ClassPathResource("static/css/employee-dashboard.css")
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(template)
                .contains("Today's Celebrations")
                .contains("th:each=\"celebration : ${dashboardContent.todaysCelebrations}\"")
                .contains("Birthdays and marriage anniversaries will appear here.")
                .contains("Upcoming Events")
                .contains("Organization holidays scheduled for the next 30 days.")
                .contains("th:each=\"event : ${dashboardContent.upcomingEvents}\"")
                .contains("There are no organization holidays scheduled in the next 30 days.")
                .contains("Announcements")
                .contains("th:each=\"announcement : ${dashboardContent.announcements}\"")
                .contains("New employee announcements will appear here.")
                .contains("class=\"celebration-action-button birthday-wish-composer\"")
                .contains("th:title=\"${celebration.employeeName}\"")
                .contains("class=\"dashboard-dialog birthday-compose-dialog\"")
                .contains("@{/css/employee-dashboard.css(v='20260903')}")
                .contains("th:action=\"@{/employee/birthday-wishes}\"")
                .contains("Birthday message")
                .contains("Wishes for You")
                .contains("data-dashboard-dialog-target=\"birthdayWishesDialog\"")
                .contains("<dialog id=\"birthdayWishesDialog\"")
                .contains("Select a colleague to read the message or reply.")
                .contains("class=\"birthday-wish-list\"")
                .contains("class=\"birthday-wish-card received\"")
                .contains("th:each=\"wish : ${dashboardContent.receivedBirthdayWishes}\"")
                .contains("class=\"birthday-wish-card-body\"")
                .contains("th:action=\"@{/employee/birthday-wishes/{wishId}/reply(wishId=${wish.wishId})}\"")
                .contains("Replies to Your Wishes")
                .contains("th:text=\"${dashboardContent.todayLabel}\"")
                .doesNotContain(
                        "<small th:text=\"${celebration.message}\"",
                        "Birthdays, anniversaries, and organization holidays.",
                        "View Calendar");

        assertThat(interactions)
                .contains("dialog.showModal()")
                .contains("[data-dashboard-dialog-target]")
                .contains("[data-dashboard-dialog-close]")
                .contains("event.target === dialog");

        assertThat(stylesheet)
                .contains("Modern employee dashboard skin")
                .contains("max-width: 1440px")
                .contains("grid-template-columns: repeat(4, minmax(0, 1fr))")
                .contains(".employee-dashboard-home .employee-dashboard .dashboard-community-grid")
                .contains("@media (max-width: 420px)");
    }
}
