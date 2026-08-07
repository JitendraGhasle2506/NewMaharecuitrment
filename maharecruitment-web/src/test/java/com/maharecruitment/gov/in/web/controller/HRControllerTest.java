package com.maharecruitment.gov.in.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;

import com.maharecruitment.gov.in.web.service.dashboard.HRDashboardService;
import com.maharecruitment.gov.in.web.service.dashboard.model.HRDashboardView;
import com.maharecruitment.gov.in.web.service.dashboard.model.HRTodayAttendanceView;

@ExtendWith(MockitoExtension.class)
class HRControllerTest {

    @Mock
    private HRDashboardService dashboardService;

    @Mock
    private HRDashboardView dashboard;

    @Mock
    private HRTodayAttendanceView attendance;

    @Test
    void dashboardUsesSingleViewModel() {
        when(dashboardService.getDashboard()).thenReturn(dashboard);
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller().hrDashboard(model);

        assertThat(view).isEqualTo("hr/hr_dashboard");
        assertThat(model).containsOnlyKeys("dashboard").containsEntry("dashboard", dashboard);
    }

    @Test
    void attendanceDetailsUseDedicatedEndpointAndViewModel() {
        when(dashboardService.getTodayAttendance()).thenReturn(attendance);
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller().todayAttendance(model);

        assertThat(view).isEqualTo("hr/hr_attendance_today");
        assertThat(model).containsOnlyKeys("attendance").containsEntry("attendance", attendance);
    }

    private HRController controller() {
        return new HRController(dashboardService);
    }
}
