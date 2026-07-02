package com.maharecruitment.gov.in.master.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maharecruitment.gov.in.master.dto.LocationMasterDto;
import com.maharecruitment.gov.in.master.entity.LocationMaster;
import com.maharecruitment.gov.in.master.exception.BusinessValidationException;
import com.maharecruitment.gov.in.master.exception.DuplicateResourceException;
import com.maharecruitment.gov.in.master.mapper.LocationMasterMapper;
import com.maharecruitment.gov.in.master.repository.LocationMasterRepository;

@ExtendWith(MockitoExtension.class)
class LocationMasterServiceImplTest {

    @Mock
    private LocationMasterRepository repository;

    private LocationMasterServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new LocationMasterServiceImpl(repository, new LocationMasterMapper());
    }

    @Test
    void createSavesTrimmedActiveLocation() {
        LocationMasterDto request = LocationMasterDto.builder()
                .officeName(" MahaIT Office ")
                .locationName(" Mumbai Central ")
                .latitude(new BigDecimal("18.9690000"))
                .longitude(new BigDecimal("72.8205000"))
                .build();

        when(repository.existsByLocationNameIgnoreCase("Mumbai Central")).thenReturn(false);
        when(repository.save(any(LocationMaster.class))).thenAnswer(invocation -> {
            LocationMaster entity = invocation.getArgument(0);
            entity.setLocationId(11L);
            return entity;
        });

        LocationMasterDto response = service.create(request);

        ArgumentCaptor<LocationMaster> captor = ArgumentCaptor.forClass(LocationMaster.class);
        verify(repository).save(captor.capture());

        LocationMaster saved = captor.getValue();
        assertThat(saved.getOfficeName()).isEqualTo("MahaIT Office");
        assertThat(saved.getLocationName()).isEqualTo("Mumbai Central");
        assertThat(saved.getActiveFlag()).isEqualTo("Y");
        assertThat(response.getLocationId()).isEqualTo(11L);
        assertThat(response.getLatitude()).isEqualByComparingTo("18.9690000");
    }

    @Test
    void createRejectsDuplicateLocationName() {
        LocationMasterDto request = validRequest();
        when(repository.existsByLocationNameIgnoreCase("Mumbai Central")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Location name already exists");

        verify(repository, never()).save(any(LocationMaster.class));
    }

    @Test
    void createRejectsLatitudeOutsideAllowedRange() {
        LocationMasterDto request = validRequest();
        request.setLatitude(new BigDecimal("91.0000000"));
        when(repository.existsByLocationNameIgnoreCase("Mumbai Central")).thenReturn(false);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("Latitude must be between");

        verify(repository, never()).save(any(LocationMaster.class));
    }

    private LocationMasterDto validRequest() {
        return LocationMasterDto.builder()
                .locationName("Mumbai Central")
                .latitude(new BigDecimal("18.9690000"))
                .longitude(new BigDecimal("72.8205000"))
                .activeFlag("Y")
                .build();
    }
}
