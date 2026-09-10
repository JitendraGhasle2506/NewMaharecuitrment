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
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.maharecruitment.gov.in.master.dto.ManpowerDesignationMasterResponse;
import com.maharecruitment.gov.in.master.dto.ManpowerDesignationRateRequest;
import com.maharecruitment.gov.in.master.dto.ManpowerDesignationRateResponse;
import com.maharecruitment.gov.in.master.dto.ResourceLevelRefResponse;
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

    @Test
    void editPreservesInactiveDesignationAndSavedUnmappedLevel() {
        ManpowerDesignationRateResponse rate = ManpowerDesignationRateResponse.builder()
                .rateId(111L)
                .designationId(6L)
                .levelCode("L1")
                .grossMonthlyCtc(new BigDecimal("31000.00"))
                .effectiveFrom(LocalDate.of(2026, 1, 1))
                .effectiveTo(LocalDate.of(2026, 12, 31))
                .activeFlag("Y")
                .build();
        ResourceLevelRefResponse mappedLevel = ResourceLevelRefResponse.builder()
                .levelId(2L)
                .levelCode("L2")
                .levelName("level 2")
                .build();
        ManpowerDesignationMasterResponse currentDesignation = ManpowerDesignationMasterResponse.builder()
                .designationId(6L)
                .designationName(".Net Developer")
                .category("Technical")
                .activeFlag("N")
                .levels(java.util.Set.of(mappedLevel))
                .build();

        when(rateService.getById(111L, true)).thenReturn(rate);
        when(designationService.getAll(eq(false), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(designationService.getById(6L, true)).thenReturn(currentDesignation);

        ExtendedModelMap model = new ExtendedModelMap();
        String viewName = controller.editForm(111L, model, new RedirectAttributesModelMap());

        assertThat(viewName).isEqualTo("master/designation-rates/form");
        ManpowerDesignationRateRequest form = (ManpowerDesignationRateRequest) model.get("designationRateForm");
        assertThat(form.getDesignationId()).isEqualTo(6L);
        assertThat(form.getLevelCode()).isEqualTo("L1");
        assertThat(model.get("availableDesignations")).asList().containsExactly(currentDesignation);
        assertThat(model.get("availableLevels")).asList()
                .extracting("levelCode")
                .containsExactly("L1", "L2");
    }

    @Test
    void designationRateDatesUseHtmlDateFormat() throws NoSuchFieldException {
        DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();
        LocalDate date = LocalDate.of(2026, 1, 1);

        assertThat(formatDateField(conversionService, "effectiveFrom", date)).isEqualTo("2026-01-01");
        assertThat(formatDateField(conversionService, "effectiveTo", date)).isEqualTo("2026-01-01");
    }

    private String formatDateField(
            DefaultFormattingConversionService conversionService,
            String fieldName,
            LocalDate value) throws NoSuchFieldException {
        TypeDescriptor fieldType = new TypeDescriptor(
                ManpowerDesignationRateRequest.class.getDeclaredField(fieldName));
        return (String) conversionService.convert(value, fieldType, TypeDescriptor.valueOf(String.class));
    }
}
