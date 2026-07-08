package com.maharecruitment.gov.in.web.service.agency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.service.AgencyAccountAccessService;
import com.maharecruitment.gov.in.auth.service.UserAffiliationService;
import com.maharecruitment.gov.in.master.entity.AgencyMaster;
import com.maharecruitment.gov.in.master.entity.AgencyStatus;
import com.maharecruitment.gov.in.master.repository.AgencyMasterRepository;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;

@ExtendWith(MockitoExtension.class)
class AgencyAccessServiceTest {

    @Mock
    private UserAffiliationService userAffiliationService;

    @Mock
    private AgencyMasterRepository agencyMasterRepository;

    @Test
    void mappedActiveAgencyReturnsContext() {
        User user = user(10L, true);
        AgencyMaster agency = agency(50L, "Valid Agency", AgencyStatus.ACTIVE);
        when(userAffiliationService.loadUserByEmail("agency@example.com")).thenReturn(user);
        when(userAffiliationService.resolvePrimaryAgencyId(user)).thenReturn(50L);
        when(agencyMasterRepository.findById(50L)).thenReturn(Optional.of(agency));

        AgencyUserContext context = service().requireActiveAgencyContext(" agency@example.com ");

        assertThat(context.userId()).isEqualTo(10L);
        assertThat(context.agencyId()).isEqualTo(50L);
        assertThat(context.agencyName()).isEqualTo("Valid Agency");
    }

    @Test
    void missingMappingIsRejectedWithoutEmailFallback() {
        User user = user(11L, true);
        when(userAffiliationService.loadUserByEmail("agency@example.com")).thenReturn(user);
        when(userAffiliationService.resolvePrimaryAgencyId(user)).thenReturn(null);

        assertThatThrownBy(() -> service().requireActiveAgencyContext("agency@example.com"))
                .isInstanceOfSatisfying(RecruitmentNotificationException.class, ex ->
                        assertThat(ex.getMessage()).isEqualTo(
                                AgencyAccountAccessService.MISSING_AGENCY_MAPPING_MESSAGE));
    }

    @Test
    void inactiveAgencyIsRejected() {
        User user = user(12L, true);
        when(userAffiliationService.loadUserByEmail("agency@example.com")).thenReturn(user);
        when(userAffiliationService.resolvePrimaryAgencyId(user)).thenReturn(60L);
        when(agencyMasterRepository.findById(60L))
                .thenReturn(Optional.of(agency(60L, "Inactive Agency", AgencyStatus.INACTIVE)));

        assertThatThrownBy(() -> service().requireActiveAgencyContext("agency@example.com"))
                .isInstanceOfSatisfying(RecruitmentNotificationException.class, ex ->
                        assertThat(ex.getMessage()).isEqualTo(
                                AgencyAccountAccessService.INACTIVE_AGENCY_MESSAGE));
    }

    private AgencyAccessService service() {
        return new AgencyAccessService(userAffiliationService, agencyMasterRepository);
    }

    private User user(Long userId, boolean active) {
        User user = new User();
        user.setId(userId);
        user.setEmail("agency@example.com");
        user.setActive(active);
        return user;
    }

    private AgencyMaster agency(Long agencyId, String agencyName, AgencyStatus status) {
        AgencyMaster agency = new AgencyMaster();
        agency.setAgencyId(agencyId);
        agency.setAgencyName(agencyName);
        agency.setStatus(status);
        return agency;
    }
}
