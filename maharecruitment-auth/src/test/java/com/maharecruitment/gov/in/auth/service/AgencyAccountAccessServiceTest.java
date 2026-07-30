package com.maharecruitment.gov.in.auth.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.DisabledException;

import com.maharecruitment.gov.in.auth.entity.Role;
import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserAgencyMappingRepository;

@ExtendWith(MockitoExtension.class)
class AgencyAccountAccessServiceTest {

    @Mock
    private UserAgencyMappingRepository userAgencyMappingRepository;

    @Test
    void nonAgencyUserDoesNotRequireAgencyMapping() {
        new AgencyAccountAccessService(userAgencyMappingRepository).validateLoginAccess(user(10L, "ROLE_EMPLOYEE"));

        verifyNoInteractions(userAgencyMappingRepository);
    }

    @Test
    void agencyUserWithoutMappingIsRejected() {
        when(userAgencyMappingRepository.existsActiveMappingForUserId(20L)).thenReturn(false);

        assertThatThrownBy(() -> new AgencyAccountAccessService(userAgencyMappingRepository)
                .validateLoginAccess(user(20L, "ROLE_AGENCY")))
                .isInstanceOfSatisfying(DisabledException.class, ex ->
                        org.assertj.core.api.Assertions.assertThat(ex.getMessage())
                                .isEqualTo(AgencyAccountAccessService.MISSING_AGENCY_MAPPING_MESSAGE));
    }

    @Test
    void agencyUserMappedToInactiveAgencyIsRejected() {
        when(userAgencyMappingRepository.existsActiveMappingForUserId(30L)).thenReturn(true);
        when(userAgencyMappingRepository.existsActiveAgencyForUserId(30L)).thenReturn(false);

        assertThatThrownBy(() -> new AgencyAccountAccessService(userAgencyMappingRepository)
                .validateLoginAccess(user(30L, "ROLE_AGENCY")))
                .isInstanceOfSatisfying(DisabledException.class, ex ->
                        org.assertj.core.api.Assertions.assertThat(ex.getMessage())
                                .isEqualTo(AgencyAccountAccessService.INACTIVE_AGENCY_MESSAGE));
    }

    @Test
    void activeAgencyMappingAllowsLogin() {
        when(userAgencyMappingRepository.existsActiveMappingForUserId(40L)).thenReturn(true);
        when(userAgencyMappingRepository.existsActiveAgencyForUserId(40L)).thenReturn(true);

        new AgencyAccountAccessService(userAgencyMappingRepository).validateLoginAccess(user(40L, "ROLE_AGENCY"));
    }

    private User user(Long userId, String roleName) {
        User user = new User();
        user.setId(userId);
        user.setEmail("user" + userId + "@example.com");
        user.setRoles(List.of(new Role(null, roleName, List.of(), List.of())));
        return user;
    }
}
