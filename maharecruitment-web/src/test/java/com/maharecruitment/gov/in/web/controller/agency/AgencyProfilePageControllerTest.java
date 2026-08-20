package com.maharecruitment.gov.in.web.controller.agency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ExtendedModelMap;

import com.maharecruitment.gov.in.master.dto.AgencyMasterResponse;
import com.maharecruitment.gov.in.web.controller.agency.AgencyProfilePageController.SensitiveIdentifierResponse;
import com.maharecruitment.gov.in.web.service.master.AgencyMasterPageService;

@ExtendWith(MockitoExtension.class)
class AgencyProfilePageControllerTest {

    private static final String AGENCY_EMAIL = "agency@example.com";

    @Mock
    private AgencyMasterPageService agencyMasterPageService;

    private AgencyProfilePageController controller;
    private Principal principal;

    @BeforeEach
    void setUp() {
        controller = new AgencyProfilePageController(agencyMasterPageService);
        principal = () -> AGENCY_EMAIL;
    }

    @Test
    void profileShowsOnlyLastFourCharactersOfStoredIdentifiers() {
        AgencyMasterResponse profile = profile();
        when(agencyMasterPageService.getAgencyProfile(AGENCY_EMAIL)).thenReturn(profile);
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.profile(principal, model, new AgencyMasterResponse());

        assertThat(view).isEqualTo("agency/profile");
        AgencyMasterResponse rendered = (AgencyMasterResponse) model.get("agencyMasterResponse");
        assertThat(rendered.getPanNumber()).isEqualTo("XXXXXX234F");
        assertThat(rendered.getGstNumber()).isEqualTo("XXXXXXXXXXXF1Z5");
        assertThat(rendered.getCertificateNumber()).isEqualTo("XXXXXXXXXXX9876");
    }

    @Test
    void revealReturnsUnmaskedValueWithoutAllowingResponseCaching() {
        when(agencyMasterPageService.getAgencyProfile(AGENCY_EMAIL)).thenReturn(profile());

        ResponseEntity<SensitiveIdentifierResponse> response =
                controller.revealSensitiveIdentifier("PAN", principal);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().value()).isEqualTo("ABCDE1234F");
    }

    @Test
    void revealRejectsMissingAuthenticationBeforeReadingAgencyData() {
        ResponseEntity<SensitiveIdentifierResponse> response =
                controller.revealSensitiveIdentifier("GST", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(agencyMasterPageService, never()).getAgencyProfile(AGENCY_EMAIL);
    }

    private AgencyMasterResponse profile() {
        AgencyMasterResponse profile = new AgencyMasterResponse();
        profile.setPanNumber("ABCDE1234F");
        profile.setGstNumber("27ABCDE1234F1Z5");
        profile.setCertificateNumber("CIN-2026-009876");
        return profile;
    }
}
