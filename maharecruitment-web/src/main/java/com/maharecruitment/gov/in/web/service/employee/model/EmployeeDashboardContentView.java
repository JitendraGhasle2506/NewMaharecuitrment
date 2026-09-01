package com.maharecruitment.gov.in.web.service.employee.model;

import java.time.LocalDate;
import java.util.List;

public record EmployeeDashboardContentView(
        LocalDate today,
        String todayLabel,
        List<CelebrationView> todaysCelebrations,
        List<UpcomingEventView> upcomingEvents,
        List<AnnouncementView> announcements,
        List<BirthdayWishView> receivedBirthdayWishes,
        List<BirthdayWishView> birthdayReplies) {

    public EmployeeDashboardContentView {
        todaysCelebrations = List.copyOf(todaysCelebrations);
        upcomingEvents = List.copyOf(upcomingEvents);
        announcements = List.copyOf(announcements);
        receivedBirthdayWishes = List.copyOf(receivedBirthdayWishes);
        birthdayReplies = List.copyOf(birthdayReplies);
    }

    public record CelebrationView(
            Long employeeId,
            String employeeName,
            String type,
            String label,
            String message,
            boolean ownCelebration,
            boolean canWish,
            boolean wishSent,
            String wishReply) {
    }

    public record UpcomingEventView(
            String title,
            LocalDate date,
            String day,
            String month,
            String dateLabel,
            String relativeLabel) {
    }

    public record AnnouncementView(
            String title,
            String message,
            String tone) {
    }

    public record BirthdayWishView(
            Long wishId,
            String employeeName,
            String wishMessage,
            String replyMessage,
            String sentLabel,
            String repliedLabel) {
    }
}
