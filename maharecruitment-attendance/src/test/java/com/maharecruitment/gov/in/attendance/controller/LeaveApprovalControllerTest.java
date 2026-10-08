package com.maharecruitment.gov.in.attendance.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;

import com.maharecruitment.gov.in.attendance.service.LeaveApplicationService;
import com.maharecruitment.gov.in.attendance.service.TourApplicationService;
import com.maharecruitment.gov.in.common.dto.SessionUserDTO;

@ExtendWith(MockitoExtension.class)
class LeaveApprovalControllerTest {

    @Mock
    private LeaveApplicationService leaveApplicationService;

    @Mock
    private TourApplicationService tourApplicationService;

    @InjectMocks
    private LeaveApprovalController controller;

    @Test
    void leaveTabLoadsOnlyPendingLeaveData() {
        LocalDate date = LocalDate.of(2026, 10, 7);
        MockHttpSession session = authenticatedSession(7L);
        ExtendedModelMap model = new ExtendedModelMap();
        when(leaveApplicationService.getPendingLeavesForHOD(7L, "Asha", date)).thenReturn(List.of());

        String view = controller.showLeaveApprovals("Asha", date, "leave", 0, 0, model, session);

        assertThat(view).isEqualTo("attendance/leave-approvals");
        assertThat(model.get("activeTab")).isEqualTo("leave");
        verify(leaveApplicationService).getPendingLeavesForHOD(7L, "Asha", date);
        verify(leaveApplicationService, never()).getProcessedLeavesForHOD(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verifyNoInteractions(tourApplicationService);
    }

    @Test
    void historyTabLoadsOnlyRequestedDatabasePages() {
        MockHttpSession session = authenticatedSession(7L);
        ExtendedModelMap model = new ExtendedModelMap();
        PageRequest leavePage = PageRequest.of(2, 10);
        PageRequest tourPage = PageRequest.of(1, 10);
        when(leaveApplicationService.getProcessedLeavesForHOD(7L, null, null, leavePage))
                .thenReturn(Page.empty(leavePage));
        when(tourApplicationService.getProcessedToursForHOD(7L, null, null, tourPage))
                .thenReturn(Page.empty(tourPage));

        controller.showLeaveApprovals(null, null, "history", 2, 1, model, session);

        verify(leaveApplicationService).getProcessedLeavesForHOD(7L, null, null, leavePage);
        verify(tourApplicationService).getProcessedToursForHOD(7L, null, null, tourPage);
        verify(leaveApplicationService, never()).getPendingLeavesForHOD(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
        verify(tourApplicationService, never()).getPendingToursForHOD(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    private MockHttpSession authenticatedSession(Long userId) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("SESSION_USER", new SessionUserDTO(
                userId, "Approver", "approver@example.com", List.of("ROLE_HOD"),
                1L, null, null, null, null));
        return session;
    }
}
