package com.maharecruitment.gov.in.common.dto;

import java.time.LocalDateTime;
import java.util.List;

public record SessionUserDTO(
        Long id,
        String name,
        String email,
        List<String> roles,
        Long departmentId,
        String mobileNo,
        String photoPath,
        LocalDateTime loginTime,
        LocalDateTime lastLoginTime
) {
}
