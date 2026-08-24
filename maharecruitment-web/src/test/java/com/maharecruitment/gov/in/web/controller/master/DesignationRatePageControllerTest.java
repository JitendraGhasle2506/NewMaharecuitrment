package com.maharecruitment.gov.in.web.controller.master;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.ui.ExtendedModelMap;

import com.maharecruitment.gov.in.master.dto.ManpowerDesignationMasterResponse;
import com.maharecruitment.gov.in.master.dto.ManpowerDesignationRateResponse;
import com.maharecruitment.gov.in.master.service.ManpowerDesignationMasterService;
import com.maharecruitment.gov.in.master.service.ManpowerDesignationRateService;

@ExtendWith(MockitoExtension.class)
class DesignationRatePageControllerTest {

    @Mock
    private ManpowerDesignationRateService rateService;

    @Mock
    private ManpowerDesignationMasterService designationService;

    @InjectMocks
    private DesignationRatePageController controller;

    @Test
    void listResolvesNamesForInactiveDesignations() {
        ManpowerDesignationRateResponse rate = ManpowerDesignationRateResponse.builder()
                .rateId(15L)
                .designationId(8L)
                .levelCode("L2")
                .grossMonthlyCtc(new BigDecimal("75000.00"))
                .effectiveFrom(LocalDate.of(2026, 4, 1))
                .activeFlag("N")
                .build();
        ManpowerDesignationMasterResponse inactiveDesignation = ManpowerDesignationMasterResponse.builder()
                .designationId(8L)
                .designationName("Senior Project Manager")
                .category("Project Management")
                .activeFlag("N")
                .build();
        Pageable pageable = PageRequest.of(0, 10);

        when(rateService.getAll(null, true, pageable))
                .thenReturn(new PageImpl<>(List.of(rate), pageable, 1));
        when(designationService.getAll(eq(true), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(inactiveDesignation)));

        ExtendedModelMap model = new ExtendedModelMap();
        String viewName = controller.list(null, true, 0, 10, model);

        assertThat(viewName).isEqualTo("master/designation-rates/list");
        assertThat(model.get("availableDesignations")).isEqualTo(List.of(inactiveDesignation));

        @SuppressWarnings("unchecked")
        Map<Long, ManpowerDesignationMasterResponse> designationMap =
                (Map<Long, ManpowerDesignationMasterResponse>) model.get("designationMap");
        assertThat(designationMap.get(8L).getDesignationName()).isEqualTo("Senior Project Manager");
        verify(designationService).getAll(eq(true), any(Pageable.class));
    }
}
