package com.maharecruitment.gov.in.web.controller.hr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.ui.ConcurrentModel;

import com.maharecruitment.gov.in.web.service.hr.EmployeeListExcelExporter;
import com.maharecruitment.gov.in.web.service.hr.HROnboardingPageService;
import com.maharecruitment.gov.in.web.service.hr.model.EmployeeListView;
import com.maharecruitment.gov.in.workorder.service.HrWorkOrderService;

class EmployeeListControllerTest {

    @Test
    void employeeListQualifiesSortFieldsForJoinedEmployeeQuery() {
        HROnboardingPageService onboardingService = mock(HROnboardingPageService.class);
        EmployeeListController controller = new EmployeeListController(
                onboardingService,
                mock(HrWorkOrderService.class),
                mock(EmployeeListExcelExporter.class));
        Page<EmployeeListView> emptyPage = new PageImpl<>(List.of());
        when(onboardingService.getEmployeesByStatus(
                eq("ALL"), eq("ACTIVE"), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(emptyPage);
        when(onboardingService.getAgencyFilterOptions("ACTIVE")).thenReturn(List.of());

        controller.employeeList("ALL", null, 0, 10, null, new ConcurrentModel());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(onboardingService).getEmployeesByStatus(
                eq("ALL"), eq("ACTIVE"), eq(null), eq(null), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort().stream())
                .extracting(order -> order.getProperty())
                .containsExactly("employee.fullName", "employee.employeeId");
    }
}
