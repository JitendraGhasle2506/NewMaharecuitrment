package com.project.notification.controller;

import java.security.Principal;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.project.notification.service.NotificationService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationCurrentUserResolver currentUserResolver;

    public NotificationController(
            NotificationService notificationService,
            NotificationCurrentUserResolver currentUserResolver) {
        this.notificationService = notificationService;
        this.currentUserResolver = currentUserResolver;
    }

    @GetMapping
    public String notificationsPage(Model model, HttpSession session, Principal principal) {
        Long userId = currentUserResolver.resolveUserId(session, principal);
        model.addAttribute("notifications", notificationService.getNotificationsForUser(userId));
        return "notification/notifications";
    }

    @GetMapping("/list")
    public String notificationsList(Model model, HttpSession session, Principal principal) {
        Long userId = currentUserResolver.resolveUserId(session, principal);
        model.addAttribute("notifications", notificationService.getNotificationsForUser(userId));
        return "notification/fragments :: notificationItems";
    }

    @PostMapping("/{id}/read")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> markAsRead(
            @PathVariable("id") Long notificationId,
            HttpSession session,
            Principal principal) {
        Long userId = currentUserResolver.resolveUserId(session, principal);
        notificationService.markAsReadForUser(notificationId, userId);
        return ResponseEntity.ok(Map.of("success", true, "notificationId", notificationId));
    }

    @PostMapping("/seen")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> markAllAsSeen(HttpSession session, Principal principal) {
        Long userId = currentUserResolver.resolveUserId(session, principal);
        notificationService.markAllAsSeen(userId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/unread-count")
    @ResponseBody
    public ResponseEntity<Map<String, Long>> unreadCount(HttpSession session, Principal principal) {
        Long userId = currentUserResolver.resolveUserId(session, principal);
        long unreadCount = notificationService.getUnreadCount(userId);
        long unseenCount = notificationService.getUnseenCount(userId);
        return ResponseEntity.ok(Map.of(
                "unreadCount", unreadCount,
                "unseenCount", unseenCount));
    }

    @GetMapping("/count")
    @ResponseBody
    public ResponseEntity<Map<String, Long>> totalCount(HttpSession session, Principal principal) {
        Long userId = currentUserResolver.resolveUserId(session, principal);
        long totalCount = notificationService.getTotalCount(userId);
        return ResponseEntity.ok(Map.of("totalCount", totalCount));
    }
}
