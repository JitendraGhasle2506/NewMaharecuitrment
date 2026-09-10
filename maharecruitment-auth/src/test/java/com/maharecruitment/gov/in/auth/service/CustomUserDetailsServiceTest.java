package com.maharecruitment.gov.in.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import com.maharecruitment.gov.in.auth.entity.Role;
import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserRepository;

class CustomUserDetailsServiceTest {

    @Test
    void exposesBlockedBucketAsLockedSpringSecurityAccount() {
        UserRepository userRepository = mock(UserRepository.class);
        AgencyAccountAccessService agencyAccountAccessService = mock(AgencyAccountAccessService.class);
        LoginAttemptBucketService bucketService = mock(LoginAttemptBucketService.class);
        User user = new User();
        user.setId(42L);
        user.setEmail("user@example.com");
        user.setPassword("encoded-password");
        user.setActive(true);
        user.setRoles(List.of(new Role(null, "ROLE_EMPLOYEE", List.of(), List.of())));
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(bucketService.isBlocked(42L)).thenReturn(true);
        CustomUserDetailsService service = new CustomUserDetailsService(
                userRepository,
                agencyAccountAccessService,
                bucketService);

        UserDetails userDetails = service.loadUserByUsername("user@example.com");

        assertThat(userDetails.isAccountNonLocked()).isFalse();
        verify(agencyAccountAccessService).validateLoginAccess(user);
    }
}
