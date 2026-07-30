package com.maharecruitment.gov.in.auth.service;

import java.time.LocalDateTime;

import com.maharecruitment.gov.in.auth.entity.User;

public interface UserLoginTrackingService {

    LocalDateTime recordSuccessfulLogin(User user, LocalDateTime loginTime);
}
