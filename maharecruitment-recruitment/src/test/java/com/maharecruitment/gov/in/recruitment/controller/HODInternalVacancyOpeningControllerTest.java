package com.maharecruitment.gov.in.recruitment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.maharecruitment.gov.in.recruitment.service.InternalVacancyCandidateReviewService;
import com.maharecruitment.gov.in.recruitment.service.InternalVacancyInterviewAuthorityShortlistingService;
import com.maharecruitment.gov.in.recruitment.service.InternalVacancyOpeningService;
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentCandidateReviewDecision;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyCandidateListView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyOpeningCommand;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyOpeningDetailsView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyOpeningResult;

class HODInternalVacancyOpeningControllerTest {

    @Test
    void ownerCanViewRequestInReadOnlyPage() throws Exception {
        InternalVacancyOpeningService service = mock(InternalVacancyOpeningService.class);
        InternalVacancyCandidateReviewService candidateReviewService = mock(InternalVacancyCandidateReviewService.class);
        InternalVacancyOpeningDetailsView application = InternalVacancyOpeningDetailsView.builder()
                .internalVacancyOpeningId(7L)
                .requestId("REQ-7")
                .build();
        when(service.getOpeningDetailsForOwner(7L, "hod@example.com")).thenReturn(application);
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(createController(service, candidateReviewService))
                .build();

        mockMvc.perform(get("/hod1/internal-vacancies/7/view")
                        .principal(() -> "hod@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("hod/internal-vacancy-opening-view"))
                .andExpect(model().attribute("application", application));

        verify(service).getOpeningDetailsForOwner(7L, "hod@example.com");
    }

    @Test
    void ownerCanViewCandidateApplicationsSubmittedAgainstRequest() throws Exception {
        InternalVacancyOpeningService service = mock(InternalVacancyOpeningService.class);
        InternalVacancyCandidateReviewService candidateReviewService = mock(InternalVacancyCandidateReviewService.class);
        InternalVacancyCandidateListView candidateListView = InternalVacancyCandidateListView.builder()
                .internalVacancyOpeningId(23L)
                .requestId("REQ-23")
                .projectName("Project")
                .build();
        InternalVacancyOpeningDetailsView requestDetails = InternalVacancyOpeningDetailsView.builder()
                .internalVacancyOpeningId(23L)
                .requestId("REQ-23")
                .build();
        when(candidateReviewService.getSubmittedCandidatesByRequestIdForOwner(
                "REQ-23",
                "hod@example.com"))
                .thenReturn(candidateListView);
        when(service.getOpeningDetailsForOwner(23L, "hod@example.com"))
                .thenReturn(requestDetails);
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(createController(service, candidateReviewService))
                .build();

        mockMvc.perform(get("/hod1/internal-vacancies/request/REQ-23/applications")
                        .principal(() -> "hod@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("hod/internal-vacancy-candidate-list"))
                .andExpect(model().attribute("candidateListView", candidateListView))
                .andExpect(model().attribute("requestDetails", requestDetails));

        verify(candidateReviewService).getSubmittedCandidatesByRequestIdForOwner(
                "REQ-23",
                "hod@example.com");
        verify(service).getOpeningDetailsForOwner(23L, "hod@example.com");
    }

    @Test
    void ownerCanShortlistCandidateFromApplicationsPage() throws Exception {
        InternalVacancyOpeningService service = mock(InternalVacancyOpeningService.class);
        InternalVacancyCandidateReviewService candidateReviewService = mock(InternalVacancyCandidateReviewService.class);
        InternalVacancyInterviewAuthorityShortlistingService shortlistingService =
                mock(InternalVacancyInterviewAuthorityShortlistingService.class);
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new HODInternalVacancyOpeningController(
                        service,
                        candidateReviewService,
                        shortlistingService))
                .build();

        mockMvc.perform(post("/hod1/internal-vacancies/request/REQ-23/applications/91/review")
                        .principal(() -> "hod@example.com")
                        .param("decision", "SHORTLIST")
                        .param("remarks", "Meets the role requirements"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/hod1/internal-vacancies/request/REQ-23/applications"));

        verify(shortlistingService).reviewCandidate(
                "hod@example.com",
                "REQ-23",
                91L,
                DepartmentCandidateReviewDecision.SHORTLIST,
                "Meets the role requirements");
    }

    @Test
    void implicitFormSubmissionDefaultsToSubmitAction() throws Exception {
        InternalVacancyOpeningService service = mock(InternalVacancyOpeningService.class);
        InternalVacancyCandidateReviewService candidateReviewService = mock(InternalVacancyCandidateReviewService.class);
        when(service.saveOpening(any(InternalVacancyOpeningCommand.class)))
                .thenReturn(InternalVacancyOpeningResult.builder()
                        .internalVacancyOpeningId(1L)
                        .requestId("REQ-1")
                        .build());
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(createController(service, candidateReviewService))
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
        InternalVacancyCandidateReviewService candidateReviewService = mock(InternalVacancyCandidateReviewService.class);
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(createController(service, candidateReviewService))
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
        InternalVacancyCandidateReviewService candidateReviewService = mock(InternalVacancyCandidateReviewService.class);
        when(service.saveOpening(any(InternalVacancyOpeningCommand.class)))
                .thenThrow(new IllegalStateException("Database constraint details"));
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(createController(service, candidateReviewService))
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

    private HODInternalVacancyOpeningController createController(
            InternalVacancyOpeningService service,
            InternalVacancyCandidateReviewService candidateReviewService) {
        return new HODInternalVacancyOpeningController(
                service,
                candidateReviewService,
                mock(InternalVacancyInterviewAuthorityShortlistingService.class));
    }
}
