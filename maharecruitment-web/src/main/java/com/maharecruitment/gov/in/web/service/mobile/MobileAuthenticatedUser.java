package com.maharecruitment.gov.in.web.service.mobile;

import java.util.List;

public record MobileAuthenticatedUser(
        Long userId,
        String name,
        String email,
        String mobileNo,
        List<String> roles) {

    public MobileAuthenticatedUser {
        roles = roles == null ? List.of() : List.copyOf(roles);
    }
}
