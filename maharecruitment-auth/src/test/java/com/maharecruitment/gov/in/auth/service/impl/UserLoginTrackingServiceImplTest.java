package com.maharecruitment.gov.in.auth.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.auth.service.LoginAttemptBucketService;

class UserLoginTrackingServiceImplTest {

    @Test
    void successfulLoginClearsTheUsersFailureBucket() {
        UserRepository userRepository = mock(UserRepository.class);
        LoginAttemptBucketService bucketService = mock(LoginAttemptBucketService.class);
        UserLoginTrackingServiceImpl service = new UserLoginTrackingServiceImpl(userRepository, bucketService);
        User user = new User();
        user.setId(42L);
        LocalDateTime previousLogin = LocalDateTime.of(2026, 9, 9, 10, 0);
        LocalDateTime currentLogin = LocalDateTime.of(2026, 9, 10, 10, 0);
        user.setLastLoginAt(previousLogin);

        LocalDateTime result = service.recordSuccessfulLogin(user, currentLogin);

        assertThat(result).isEqualTo(previousLogin);
        assertThat(user.getLastLoginAt()).isEqualTo(currentLogin);
        verify(userRepository).save(user);
        verify(bucketService).reset(42L);
    }
}
