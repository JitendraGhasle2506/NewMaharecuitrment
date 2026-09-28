package com.maharecruitment.gov.in.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;

import com.maharecruitment.gov.in.common.dto.SessionUserDTO;
import com.maharecruitment.gov.in.web.service.dashboard.HodDashboardService;
import com.maharecruitment.gov.in.web.service.dashboard.HodDashboardService.HodDashboardView;

class HodDashboardControllerTest {
    @Test
    void stmAndHodUseTheSameScopedDashboardViewWithRoleSpecificTitles() {
        HodDashboardService service = mock(HodDashboardService.class);
        HodDashboardView dashboard = mock(HodDashboardView.class);
        when(service.getDashboard(10L)).thenReturn(dashboard);
        HodDashboardController controller = new HodDashboardController(service);

        ExtendedModelMap stmModel = new ExtendedModelMap();
        assertThat(controller.dashboard(session("ROLE_STM"), stmModel)).isEqualTo("role/hod_dashboard");
        assertThat(stmModel.get("dashboard")).isSameAs(dashboard);
        assertThat(stmModel.get("dashboardTitle")).isEqualTo("STM Dashboard");
        assertThat(stmModel.get("hierarchyApi")).isEqualTo("/hod1/api/employee-hierarchy");

        ExtendedModelMap hodModel = new ExtendedModelMap();
        assertThat(controller.dashboard(session("ROLE_HOD"), hodModel)).isEqualTo("role/hod_dashboard");
        assertThat(hodModel.get("dashboardTitle")).isEqualTo("HOD Dashboard");
    }

    private MockHttpSession session(String role) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("SESSION_USER", new SessionUserDTO(
                10L, "Team Manager", "manager@example.test", List.of(role), null, null, null,
                LocalDateTime.now(), LocalDateTime.now()));
        return session;
    }
}
