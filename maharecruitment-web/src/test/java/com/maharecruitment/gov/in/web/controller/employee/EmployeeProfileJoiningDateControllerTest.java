package com.maharecruitment.gov.in.web.controller.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.maharecruitment.gov.in.recruitment.service.AdditionalChargeService;
import com.maharecruitment.gov.in.web.dto.employee.EmployeeProfileDTO;
import com.maharecruitment.gov.in.web.service.employee.EmployeeDashboardContentService;
import com.maharecruitment.gov.in.web.service.employee.EmployeeProfileService;

class EmployeeProfileJoiningDateControllerTest {
    @Test void bindsJoiningDateAndReturnsSavedDateForCurrentEmployee() throws Exception {
        var service = mock(EmployeeProfileService.class);
        var saved = new EmployeeProfileDTO();
        saved.setEmail("employee@example.test");
        saved.setJoiningDate(LocalDate.of(2020, 4, 1));
        when(service.updateCurrentEmployeeProfile(eq("employee@example.test"), any())).thenReturn(saved);
        var controller = new EmployeeDashboardController(service,
                mock(EmployeeDashboardContentService.class), mock(AdditionalChargeService.class));
        var mapper = org.springframework.http.converter.json.Jackson2ObjectMapperBuilder.json()
                .featuresToDisable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build();
        var mvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(mapper)).build();
        mvc.perform(post("/employee/profile/update").principal(() -> "employee@example.test")
                .param("email", "employee@example.test").param("joiningDate", "2020-04-01"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.profile.joiningDate").value("2020-04-01"));
        var input = ArgumentCaptor.forClass(EmployeeProfileDTO.class);
        verify(service).updateCurrentEmployeeProfile(eq("employee@example.test"), input.capture());
        assertThat(input.getValue().getJoiningDate()).isEqualTo(saved.getJoiningDate());
    }

    @Test void rejectsFutureAndMalformedDatesWithFieldErrors() throws Exception {
        var service = mock(EmployeeProfileService.class);
        var controller = new EmployeeDashboardController(service,
                mock(EmployeeDashboardContentService.class), mock(AdditionalChargeService.class));
        var mvc = MockMvcBuilders.standaloneSetup(controller).build();
        for (String value : java.util.List.of(LocalDate.now().plusDays(1).toString(), "invalid-date")) {
            mvc.perform(post("/employee/profile/update").principal(() -> "employee@example.test")
                    .param("email", "employee@example.test").param("joiningDate", value))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors.joiningDate").isNotEmpty());
        }
        verifyNoInteractions(service);
    }
}
