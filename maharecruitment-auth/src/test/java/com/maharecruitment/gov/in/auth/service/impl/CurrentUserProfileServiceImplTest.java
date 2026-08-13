package com.maharecruitment.gov.in.auth.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.maharecruitment.gov.in.auth.dto.UserPasswordChangeRequest;
import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.auth.service.UserAffiliationService;

class CurrentUserProfileServiceImplTest {

    @Test
    void successfulPasswordChangeClearsMandatoryChangeFlag() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        UserAffiliationService affiliationService = mock(UserAffiliationService.class);
        CurrentUserProfileServiceImpl service = new CurrentUserProfileServiceImpl(
                userRepository, passwordEncoder, affiliationService);
        User user = new User();
        user.setId(9L);
        user.setEmail("first.login@example.com");
        user.setPassword("encoded-current");
        user.setPasswordChangeRequired(true);
        when(affiliationService.loadUserByEmail(user.getEmail())).thenReturn(user);
        when(passwordEncoder.matches("Current@123", "encoded-current")).thenReturn(true);
        when(passwordEncoder.matches("New@Password123", "encoded-current")).thenReturn(false);
        when(passwordEncoder.encode("New@Password123")).thenReturn("encoded-new");
        UserPasswordChangeRequest request = new UserPasswordChangeRequest();
        request.setCurrentPassword("Current@123");
        request.setNewPassword("New@Password123");

        service.changePassword(user.getEmail(), request);

        assertThat(user.getPassword()).isEqualTo("encoded-new");
        assertThat(user.getPasswordChangeRequired()).isFalse();
        verify(userRepository).save(user);
    }
}
