package com.maharecruitment.gov.in.auth.dto;

import java.time.LocalDateTime;
import java.util.List;

public record SessionUserDTO(
         
        Long id,
        String name,
        String email,
        List<String> roles,
        Long departmentId,
        String mobileNo,
        LocalDateTime loginTime,
        LocalDateTime lastLoginTime
) {
}
