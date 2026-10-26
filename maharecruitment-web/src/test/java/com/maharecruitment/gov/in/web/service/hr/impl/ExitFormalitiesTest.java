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
    @Test void oldHrEndpointsCannotCreateEditCancelOrExitPipRecords() {
        var records = mock(EmployeeRelievingRepository.class);
        var employees = mock(EmployeeRepository.class);
        var service = new EmployeeRelievingServiceImpl(records, employees);
        var oldPip = new EmployeeRelievingEntity(); oldPip.setReasonOfRelieving("PIP");
        when(records.findById(1L)).thenReturn(Optional.of(oldPip));
        var form = new com.maharecruitment.gov.in.recruitment.dto.employee.EmployeeRelievingDto();
        form.setReasonOfRelieving(" PIP ");
        assertThatThrownBy(() -> service.saveRelieving(form)).hasMessageContaining("authorised reporting manager");
        form.setRelievingId(1L); form.setReasonOfRelieving("Resignation");
        assertThatThrownBy(() -> service.saveRelieving(form)).hasMessageContaining("authorised reporting manager");
        assertThatThrownBy(() -> service.cancelResignation(1L)).hasMessageContaining("authorised reporting manager");
        assertThatThrownBy(() -> service.markExitDate(1L,java.time.LocalDate.now())).hasMessageContaining("authorised reporting manager");
        assertThatThrownBy(() -> service.updatePip(1L,java.time.LocalDate.now(),"1 month")).hasMessageContaining("authorised reporting manager");
        verify(records,never()).save(any()); verifyNoInteractions(employees);
    }}