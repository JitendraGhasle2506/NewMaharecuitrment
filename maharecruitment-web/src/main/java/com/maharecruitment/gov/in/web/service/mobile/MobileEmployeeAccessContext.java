package com.maharecruitment.gov.in.web.service.mobile;

import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;

public record MobileEmployeeAccessContext(
        User user,
        EmployeeEntity employee) {
}
