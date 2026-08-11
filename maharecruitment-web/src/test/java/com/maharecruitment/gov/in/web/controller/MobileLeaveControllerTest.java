package com.maharecruitment.gov.in.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maharecruitment.gov.in.web.dto.mobile.MobileLeaveApplication;
import com.maharecruitment.gov.in.web.dto.mobile.MobileLeaveApplicationResponse;
import com.maharecruitment.gov.in.web.dto.mobile.MobileLeaveApplyRequest;
import com.maharecruitment.gov.in.web.service.mobile.MobileLeaveService;

@ExtendWith(MockitoExtension.class)
class MobileLeaveControllerTest {

    @Mock
    private MobileLeaveService mobileLeaveService;

    @Test
    void applyReturnsCreatedWithResourceLocation() {
        MobileLeaveController controller = new MobileLeaveController(mobileLeaveService);
        MobileLeaveApplyRequest request = new MobileLeaveApplyRequest(
                101L,
                "CL",
                "FULL_DAY",
                LocalDate.of(2026, 8, 12),
                LocalDate.of(2026, 8, 12),
                null,
                "Personal work");
        MobileLeaveApplication application = new MobileLeaveApplication(
                55L,
                101L,
                "CL",
                "Full Day",
                LocalDate.of(2026, 8, 12),
                LocalDate.of(2026, 8, 12),
                null,
                "Personal work",
                null,
                "PENDING",
                null,
                null,
                true);
        MobileLeaveApplicationResponse response = new MobileLeaveApplicationResponse(
                true,
                "Leave application submitted successfully.",
                application);
        when(mobileLeaveService.apply(request)).thenReturn(response);

        var result = controller.apply(request);

        assertThat(result.getStatusCode().value()).isEqualTo(201);
        assertThat(result.getHeaders().getLocation()).hasToString("/api/mobile/leaves/55");
        assertThat(result.getBody()).isSameAs(response);
        verify(mobileLeaveService).apply(request);
    }
}
