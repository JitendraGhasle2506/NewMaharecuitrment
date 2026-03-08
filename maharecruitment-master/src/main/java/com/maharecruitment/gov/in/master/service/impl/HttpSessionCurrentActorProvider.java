package com.maharecruitment.gov.in.master.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.maharecruitment.gov.in.auth.dto.SessionUserDTO;
import com.maharecruitment.gov.in.master.service.CurrentActorProvider;

import jakarta.servlet.http.HttpSession;

@Service
public class HttpSessionCurrentActorProvider implements CurrentActorProvider {

    @Override
    public Long getCurrentUserId() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (!(requestAttributes instanceof ServletRequestAttributes servletRequestAttributes)) {
            return null;
        }

        HttpSession session = servletRequestAttributes.getRequest().getSession(false);
        if (session == null) {
            return null;
        }

        Object sessionUser = session.getAttribute("SESSION_USER");
        if (sessionUser instanceof SessionUserDTO userDTO) {
            return userDTO.id();
        }
        return null;
    }
}
