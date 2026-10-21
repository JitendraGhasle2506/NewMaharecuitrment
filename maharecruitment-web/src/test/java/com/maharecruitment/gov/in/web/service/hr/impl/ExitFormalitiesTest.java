package com.maharecruitment.gov.in.web.service.hr.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeRelievingEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRelievingRepository;

class ExitFormalitiesTest {
    @Test void cancellingPendingFormalitiesCannotReactivateAnExitedEmployee() {
        var records = mock(EmployeeRelievingRepository.class);
        var employees = mock(EmployeeRepository.class);
        var employee = new EmployeeEntity();
        employee.setStatus("RESIGNED");
        var record = new EmployeeRelievingEntity();
        record.setEmployee(employee);
        record.setStatus("Resignation Submitted");
        when(records.findById(1L)).thenReturn(Optional.of(record));
        var service = new EmployeeRelievingServiceImpl(records, employees);
        assertThatThrownBy(() -> service.cancelResignation(1L)).hasMessageContaining("cannot reactivate");
        assertThat(employee.getStatus()).isEqualTo("RESIGNED");
        assertThat(record.getStatus()).isEqualTo("Resignation Submitted");
        verify(records, never()).save(any());
        verifyNoInteractions(employees);
    }
}