package com.maharecruitment.gov.in.auth.service.impl;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.auth.service.LoginAttemptBucketService;
import com.maharecruitment.gov.in.auth.service.UserLoginTrackingService;

@Service
@Transactional
public class UserLoginTrackingServiceImpl implements UserLoginTrackingService {

    private final UserRepository userRepository;
    private final LoginAttemptBucketService loginAttemptBucketService;

    public UserLoginTrackingServiceImpl(
            UserRepository userRepository,
            LoginAttemptBucketService loginAttemptBucketService) {
        this.userRepository = userRepository;
        this.loginAttemptBucketService = loginAttemptBucketService;
    }

    @Override
    public LocalDateTime recordSuccessfulLogin(User user, LocalDateTime loginTime) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("Authenticated user is required.");
        }
        if (loginTime == null) {
            throw new IllegalArgumentException("Login time is required.");
        }

        LocalDateTime previousLoginTime = user.getLastLoginAt();
        user.setLastLoginAt(loginTime);
        userRepository.save(user);
        loginAttemptBucketService.reset(user.getId());
        return previousLoginTime;
    }
}
