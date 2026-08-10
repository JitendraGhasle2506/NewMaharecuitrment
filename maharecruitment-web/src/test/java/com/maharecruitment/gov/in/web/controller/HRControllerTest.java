package com.maharecruitment.gov.in.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;

import com.maharecruitment.gov.in.web.service.dashboard.HRDashboardService;
import com.maharecruitment.gov.in.web.service.dashboard.model.HRAttendanceDetailView;
import com.maharecruitment.gov.in.web.service.dashboard.model.HRDashboardView;
import com.maharecruitment.gov.in.web.service.dashboard.model.HRTodayAttendanceView;
import com.maharecruitment.gov.in.web.service.dashboard.model.HRWingReportsView;

@ExtendWith(MockitoExtension.class)
class HRControllerTest {

    @Mock
    private HRDashboardService dashboardService;

    @Mock
    private HRDashboardView dashboard;

    @Mock
    private HRTodayAttendanceView attendance;

    @Mock
    private HRAttendanceDetailView attendanceDetail;

    @Mock
    private HRWingReportsView wingDirectory;

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
        when(dashboardService.getTodayAttendance("CELL")).thenReturn(attendance);
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller().todayAttendance("CELL", model);

        assertThat(view).isEqualTo("hr/hr_attendance_today");
        assertThat(model).containsOnlyKeys("attendance").containsEntry("attendance", attendance);
    }

    @Test
    void attendanceEmployeeDetailsLoadOnlyAfterCountNavigation() {
        when(dashboardService.getTodayAttendanceDetails("LATE", 27L, null, null, 0, 25))
                .thenReturn(attendanceDetail);
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller().todayAttendanceDetails("LATE", 27L, null, null, 0, 25, model);

        assertThat(view).isEqualTo("hr/hr_attendance_details");
        assertThat(model)
                .containsOnlyKeys("attendanceDetail")
                .containsEntry("attendanceDetail", attendanceDetail);
    }

    @Test
    void wingReportsUseDedicatedEndpointAndViewModel() {
        when(dashboardService.getWingReports()).thenReturn(wingDirectory);
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller().wingReports(model);

        assertThat(view).isEqualTo("hr/hr_wing_reports");
        assertThat(model).containsOnlyKeys("wingDirectory").containsEntry("wingDirectory", wingDirectory);
    }

    private HRController controller() {
        return new HRController(dashboardService);
    }
}
