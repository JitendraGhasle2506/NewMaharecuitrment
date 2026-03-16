package com.project.notification.controller;

import java.security.Principal;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.auth.dto.SessionUserDTO;
import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserRepository;

import jakarta.servlet.http.HttpSession;

@Component
public class NotificationCurrentUserResolver {

    private static final String SESSION_USER_KEY = "SESSION_USER";

    private final UserRepository userRepository;

    public NotificationCurrentUserResolver(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Long resolveUserId(HttpSession session, Principal principal) {
        if (session != null) {
            Object candidate = session.getAttribute(SESSION_USER_KEY);
            if (candidate instanceof SessionUserDTO) {
                SessionUserDTO dto = (SessionUserDTO) candidate;
                if (dto.id() != null) {
                    return dto.id();
                }
            }
        }

        if (principal != null && StringUtils.hasText(principal.getName())) {
            User user = userRepository.findByEmailIgnoreCase(principal.getName()).orElse(null);
            if (user != null && user.getId() != null) {
                return user.getId();
            }
        }

        throw new IllegalStateException("Authenticated user not found for notifications.");
    }
}
