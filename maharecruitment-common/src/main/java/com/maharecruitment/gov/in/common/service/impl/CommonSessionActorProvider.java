package com.maharecruitment.gov.in.common.service.impl;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.maharecruitment.gov.in.auth.dto.SessionUserDTO;
import com.maharecruitment.gov.in.common.service.CurrentActorProvider;

import jakarta.servlet.http.HttpSession;

@Service
public class CommonSessionActorProvider implements CurrentActorProvider {

    private static final String SESSION_USER_KEY = "SESSION_USER";

    @Override
    public Long getCurrentUserId() {
        SessionUserDTO sessionUser = getSessionUser();
        return sessionUser != null ? sessionUser.id() : null;
    }

    @Override
    public String getCurrentActorEmail() {
        SessionUserDTO sessionUser = getSessionUser();
        if (sessionUser != null && sessionUser.email() != null && !sessionUser.email().isBlank()) {
            return sessionUser.email();
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getName() != null && !authentication.getName().isBlank()) {
            return authentication.getName();
        }

        return "SYSTEM";
    }

    private SessionUserDTO getSessionUser() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (!(requestAttributes instanceof ServletRequestAttributes)) {
            return null;
        }

        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) requestAttributes;
        HttpSession session = servletRequestAttributes.getRequest().getSession(false);
        if (session == null) {
            return null;
        }

        Object sessionUser = session.getAttribute(SESSION_USER_KEY);
        if (sessionUser instanceof SessionUserDTO) {
            return (SessionUserDTO) sessionUser;
        }

        return null;
    }
}
