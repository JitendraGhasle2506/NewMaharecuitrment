package com.maharecruitment.gov.in.recruitment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.security.Principal;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.maharecruitment.gov.in.recruitment.entity.InternalVacancyOpeningStatus;
import com.maharecruitment.gov.in.recruitment.service.InternalVacancyOpeningService;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyOpeningCommand;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyOpeningResult;

class HODInternalVacancyOpeningControllerTest {

    @Test
    void implicitFormSubmissionDefaultsToSubmitAction() throws Exception {
        InternalVacancyOpeningService service = mock(InternalVacancyOpeningService.class);
        when(service.saveOpening(any(InternalVacancyOpeningCommand.class)))
                .thenReturn(InternalVacancyOpeningResult.builder()
                        .internalVacancyOpeningId(1L)
                        .requestId("REQ-1")
                        .build());
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new HODInternalVacancyOpeningController(service))
                .build();
        Principal principal = () -> "hod@example.com";

        mockMvc.perform(post("/hod1/internal-vacancies")
                        .principal(principal)
                        .param("projectId", "10")
                        .param("hiringRequestType", "EMPLOYEE_REPLACEMENT")
                        .param("replacementEmployeeIds", "99"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/hod1/internal-vacancies"));

        ArgumentCaptor<InternalVacancyOpeningCommand> commandCaptor =
                ArgumentCaptor.forClass(InternalVacancyOpeningCommand.class);
        verify(service).saveOpening(commandCaptor.capture());
        assertThat(commandCaptor.getValue().getTargetStatus())
                .isEqualTo(InternalVacancyOpeningStatus.PENDING_HR_APPROVAL);
    }

    @Test
    void missingProjectReturnsClearValidationMessage() throws Exception {
        InternalVacancyOpeningService service = mock(InternalVacancyOpeningService.class);
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new HODInternalVacancyOpeningController(service))
                .build();

        mockMvc.perform(post("/hod1/internal-vacancies")
                        .principal(() -> "hod@example.com")
                        .param("action", "submit")
                        .param("hiringRequestType", "EMPLOYEE_REPLACEMENT")
                        .param("replacementEmployeeIds", "99"))
                .andExpect(status().isOk())
                .andExpect(view().name("hod/internal-vacancy-opening-form"))
                .andExpect(model().attributeHasFieldErrors("openingForm", "projectId"))
                .andExpect(model().attribute(
                        "errorMessage",
                        "Project is required."));

        verify(service, never()).saveOpening(any(InternalVacancyOpeningCommand.class));
    }

    @Test
    void unexpectedSaveFailureReturnsFormWithSafeErrorMessage() throws Exception {
        InternalVacancyOpeningService service = mock(InternalVacancyOpeningService.class);
        when(service.saveOpening(any(InternalVacancyOpeningCommand.class)))
                .thenThrow(new IllegalStateException("Database constraint details"));
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new HODInternalVacancyOpeningController(service))
                .build();

        mockMvc.perform(post("/hod1/internal-vacancies")
                        .principal(() -> "hod@example.com")
                        .param("action", "submit")
                        .param("projectId", "10")
                        .param("hiringRequestType", "EMPLOYEE_REPLACEMENT")
                        .param("replacementEmployeeIds", "99"))
                .andExpect(status().isOk())
                .andExpect(view().name("hod/internal-vacancy-opening-form"))
                .andExpect(model().attribute(
                        "errorMessage",
                        "Unable to submit the resource request right now. Please retry."
                                + " For a new candidate request, select the approval PDF again."));
    }
}
