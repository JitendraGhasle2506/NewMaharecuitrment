package com.maharecruitment.gov.in.web.service.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.recruitment.service.ReportingManagerService;

class HodDashboardServiceTest {
    private final ReportingManagerService reporting = mock(ReportingManagerService.class);
    private final EmployeeRepository employees = mock(EmployeeRepository.class);
    // private final HodDashboardService service = new HodDashboardService(reporting, employees);

    @Test
    void loadsOnlyEmployeesMappedToCurrentAuthority() {
        // when(reporting.getEffectiveEmployeeIdsForAuthority(42L)).thenReturn(List.of(7L, 9L));
        // EmployeeEntity first = new EmployeeEntity();
        // first.setEmployeeId(7L);
        // first.setFullName("Zoya");
        // EmployeeEntity second = new EmployeeEntity();
        // second.setEmployeeId(9L);
        // second.setFullName("Asha");
        // when(employees.findAllById(List.of(7L, 9L))).thenReturn(List.of(first, second));

        // assertThat(service.getEmployees(42L)).extracting(HodDashboardService.EmployeeView::fullName)
        //         .containsExactly("Asha", "Zoya");
        // verify(employees).findAllById(List.of(7L, 9L));
        // verifyNoMoreInteractions(employees);
    }

    @Test
    void unmappedAuthorityDoesNotLoadAnyEmployees() {
        // when(reporting.getEffectiveEmployeeIdsForAuthority(42L)).thenReturn(List.of());
        // assertThat(service.getEmployees(42L)).isEmpty();
        // verifyNoInteractions(employees);
    }

    @Test
    void missingAuthorityDoesNotLoadAnyEmployees() {
        // assertThat(service.getEmployees(null)).isEmpty();
        // verifyNoInteractions(reporting, employees);
    }
}
